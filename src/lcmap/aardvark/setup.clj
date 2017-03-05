(ns lcmap.aardvark.setup
  ""
  (:require [lcmap.aardvark.db :as db]
            [lcmap.aardvark.event :as event]
            [lcmap.aardvark.setup.database :as setup-db]
            [lcmap.aardvark.setup.event :as setup-event]
            [lcmap.aardvark.setup.elasticsearch :as setup-es]
            [lcmap.aardvark.util :as util]
            [mount.core :as mount]))

(defn run
  "Bootstrap system state, apply DB and event configuration.

  Parameters:
  * cfg: EDN file with service connection details.
  * cql: file containing list of CQL statements.
  * eqb: EDN file with RabbitMQ exchanges, queues, and bindings.
  "
  [{:keys [cfg cql eqb]}]
  (let [config-data (util/read-edn cfg)]
    ;; start minimal states needed to create configuration
    (-> (mount/only [#'db/db-cluster #'event/amqp-connection #'event/amqp-channel])
        (mount/with-args {:config config-data})
        (mount/start))
    ;;
    (setup-db/setup cql)
    (setup-es/setup config-data)
    (setup-event/setup eqb)
    ;;
    (mount/stop #'db/db-cluster #'event/amqp-channel)))
