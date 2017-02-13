(ns user
  "This is the entry-point for REPL sessions, useful for development.

   Run `init` to create the Cassandra schema and RabbitMQ exchanges,
   queues, and bindings.

   Please see the following files in dev/resources:
   - lcmap.aardvark.edn: connection information to backing services
   - event.setup.edn: essentially a 'schema' for RabbitMQ
   - schema.setup.cql: create Cassandra keyspaces, tables, indexes
   - schema.teardown.cql: remove Cassandra keyspaces, etc..."

  ;; Requirement order of lcmap namespaces are important because they
  ;; may affect the order in which states are started and stopped.
  (:require [clojure.java.io :as io]
            [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [lcmap.aardvark.config :as config]
            [lcmap.aardvark.db :as db]
            [lcmap.aardvark.event :as event]
            [lcmap.aardvark.util :as util]
            [mount.core :as mount]))

(defn start
  "Start stateful 'beings' using an EDN-derived configuration.

   If the system has not been initialized the db/db-session will
   fail to start because the default keyspace cannot be found,
   this can be solved by running `init`"
  []
  (-> {:config (util/read-edn "lcmap.aardvark.edn")}
      (mount/with-args)
      (mount/start)))

(defn stop
  "Stop system."
  []
  (mount/stop))

(defn reset
  "Stop, reload code, and start a system."
  []
  (stop)
  (refresh :after `start))
