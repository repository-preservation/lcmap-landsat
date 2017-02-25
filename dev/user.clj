(ns user
  "This is the entry-point for REPL sessions.

  Functions in this namespace are useful for setting up and running
  the project in a local development environment. Exercise caution
  when using a REPL that is connected to an operational environment.

  Should you need to override default configuration, please see the
  following files in dev/resources:

  - lcmap.aardvark.edn: contains connection information to backing
    services (Cassandra, RabbitMQ, and Elasticsearch).

  - event.setup.edn: defines the queues, exchanges, and bindings
    for RabbitMQ.

  - schema.setup.cql: CQL that will create Cassandra keyspaces, tables,
    and indexes."
  (:require [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [lcmap.aardvark.server :as server]
            [lcmap.aardvark.worker :as worker]
            [lcmap.aardvark.util :as util]
            [mount.core :as mount]))

(defn start
  "Start stateful 'beings' using an EDN-derived configuration.

  Any namespaces that have been required that define mount 'beings' will
  be started. If you evaluate other namespaces during development, they
  will also be started when this function is invoked."
  []
  (-> (mount/with-args {:config (util/read-edn "lcmap.aardvark.edn")})
      (mount/start)))

(defn stop
  "Stop stateful 'beings'"
  []
  (mount/stop))

(defn reset
  "Stop, reload code, and start a system."
  []
  (stop)
  (refresh :after `start))
