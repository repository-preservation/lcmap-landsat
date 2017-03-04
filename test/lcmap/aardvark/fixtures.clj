(ns lcmap.aardvark.fixtures
  "Fixtures simplify managing system state during tests.

  There are four broad categories, each of which has a setup
  and teardown method: services, data, server, and worker.

  1. Service fixtures are connections to backing services like
     Cassandra, RabbitMQ, Elasticseach. Although this is not
     a traditional use of fixtures, this avoids having to write
     that wrap tests that behave identically to `use-fixtures`.

  2. Data fixtures are related to the DB schema and seed data
     such as tile-specs. These fixtures only work if service
     fixtures have been setup. These have been separated so that
     each can be configured to run :once or :each time.

  3. Server fixtures are related to the HTTP listener. This
     fixture is used for integration tests that exercise HTTP.

  4. Worker fixtures are related the AMQP consumer.
  "
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [lcmap.aardvark.db :as db]
            [lcmap.aardvark.event :as event]
            [lcmap.aardvark.server :as server]
            [lcmap.aardvark.tile-spec :as tile-spec]
            [lcmap.aardvark.tile-spec-index :as index]
            [lcmap.aardvark.setup.database :as setup-db]
            [lcmap.aardvark.setup.event :as setup-event]
            [lcmap.aardvark.util :as util]
            [mount.core :as mount]))

;; Configuration is provided as a rebindable var so that test
;; code has a way to override it with different configuration
;; values, ostensibly so that tests can exercise error-cases.

(def ^:dynamic *test-config-path* "test.lcmap.aardvark.edn")

(def ^:dynamic *test-config-data* (util/read-edn *test-config-path*))

;;; Service Fixtures
;;;

(defn setup-services []
  (log/debug "start test service mounts")
  (-> (mount/with-args {:config *test-config-data*})
      (mount/start #'db/db-cluster
                   #'db/db-session
                   #'event/amqp-connection
                   #'event/amqp-channel)))

(defn teardown-services []
  (log/debug "stop test service mounts")
  (setup-db/teardown "test.schema.teardown.cql")
  (mount/stop #'event/amqp-channel
              #'event/amqp-connection
              #'db/db-session
              #'db/db-cluster))

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
  (setup-db/setup "test.schema.setup.cql")
  (setup-event/setup "test.event.setup.cql")
  (->> "tile-specs/L5.edn" util/read-edn (map tile-spec/save))
  (->> "tile-specs/L7.edn" util/read-edn (map tile-spec/save)))

(defn teardown-data []
  (setup-db/teardown "test.schema.teardown.cql")
  (index/clear))

(defn with-data
  [test-fn]
  (setup-data)
  (test-fn)
  (teardown-data))

;;; Server Fixtures
;;;

(defn start-server []
  (prn *test-config-data*)
  (-> (mount/with-args {:config *test-config-data*})
      (mount/start #'server/server)))

(defn stop-server []
  (mount/stop #'server/server))

(defn with-server [test-fn]
  (start-server)
  (test-fn)
  (stop-server))

;;; Worker Fixtures
;;; TODO
