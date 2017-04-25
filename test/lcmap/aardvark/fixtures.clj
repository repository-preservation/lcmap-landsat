(ns lcmap.aardvark.fixtures
  "Fixtures simplify managing system state during tests.

  There are four broad categories, each of which has a setup
  and teardown method: services, data, server, and worker.

  1. Service fixtures are stateful connections to things like:
     Cassandra, RabbitMQ, Elasticseach. Although not a common
     use of fixtures, this obviates the need for functions
     behaving identically to `use-fixtures`.

  2. Data fixtures are related to the DB schema and seed data
     such as chip-specs. These fixtures only work if service
     fixtures have been setup. These have been separated so that
     each can be configured to run :once or :each time.

  3. Server fixtures are related to the HTTP listener. This
     fixture is used for integration tests that exercise HTTP.

  4. Worker fixtures are related the AMQP consumer.
  "
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [lcmap.aardvark.config :as config]
            [lcmap.aardvark.db :as db]
            [lcmap.aardvark.elasticsearch :as es]
            [lcmap.aardvark.event :as event]
            [lcmap.aardvark.server :as server]
            [lcmap.aardvark.chip-spec :as chip-spec]
            [lcmap.aardvark.setup :as setup]
            [lcmap.aardvark.setup.database :as setup-db]
            [lcmap.aardvark.setup.event :as setup-event]
            [lcmap.aardvark.util :as util]
            [mount.core :as mount]))

;; Configuration is provided as a rebindable var so that test
;; code has a way to override it with different configuration
;; values, ostensibly so that tests can exercise error-cases.

(def ^:dynamic *test-config-path* "test.lcmap.aardvark.edn")

(def ^:dynamic *test-config-data* (util/read-edn *test-config-path*))

;; Idempotent setup. Fixtures won't work without this because
;; keyspaces, exchanges, queues, and the search index do not
;; exist.
(setup/run {:cfg "test.lcmap.aardvark.edn"
            :cql "test.schema.setup.cql"
            :eqb "test.event.setup.edn"})

;; Use this function to trigger an index refresh when integration
;; tests update the index and subsequently retrieve data.

(defn refresh-index
  "Convenience function for refreshing index"
  []
  (let [url (get-in *test-config-data* [:search :index-url])]
    (es/index-refresh url)))

;;; Service Fixtures
;;;

(defn setup-services []
  (log/debug "start test service mounts")
  (-> (mount/with-args {:config *test-config-data*})
      (mount/start #'config/config
                   #'db/db-cluster
                   #'db/db-session
                   #'event/amqp-connection
                   #'event/amqp-channel)))

(defn teardown-services []
  (log/debug "stop test service mounts")
  (setup-db/teardown "test.schema.teardown.cql")
  (mount/stop #'event/amqp-channel
              #'event/amqp-connection
              #'db/db-session
              #'db/db-cluster
              #'config/config))

(defn with-services [test-fn]
  (setup-services)
  (test-fn)
  (teardown-services))

;;; Data Fixtures
;;;
;;; Please Note: These will only work if the services fixtures
;;; are used!
;;;

(defn setup-data []
  ;; the search index would get automatically created, but this
  ;; just makes things explicit.
  (log/debug "creating a search index")
  (es/index-create (get-in *test-config-data* [:search :index-url]))
  ;; add data to the db and search index, this does rely on functions
  ;; that require testing.
  (log/debug "saving chip-specs")
  (doall (->> "chip-specs/L7.edn" util/read-edn (map chip-spec/save)))
  ;; the search index will update itself every second, this will force
  ;; re-indexing data, without this tests may fail intermittently.
  (es/index-refresh (get-in *test-config-data* [:search :index-url])))

(defn teardown-data []
  ;; This should only truncate tables,
  (log/debug "removing data from db")
  (setup-db/teardown "test.schema.teardown.cql")
  ;; Leaving messages in queues between runs could lead to some very
  ;; confusing errors messages. Like data in a table, it should be
  ;; purged. However, queues and exchanges will remain.
  (log/debug "purging messages from queues")
  (-> (get-in *test-config-data* [:server :queue]) (event/purge-queue))
  (-> (get-in *test-config-data* [:worker :queue]) (event/purge-queue))
  ;; Obviously, we want to get rid of previously indexed data.
  (log/debug "deleting search index")
  (es/index-delete (get-in *test-config-data* [:search :index-url])))

(defn with-data
  [test-fn]
  (setup-data)
  (test-fn)
  (teardown-data))

;;; Server Fixtures

(defn start-server []
  (-> (mount/only [#'server/server])
      (mount/with-args {:config *test-config-data*})
      (mount/start)))

(defn stop-server []
  (mount/stop #'server/server))

(defn with-server [test-fn]
  (start-server)
  (test-fn)
  (stop-server))
