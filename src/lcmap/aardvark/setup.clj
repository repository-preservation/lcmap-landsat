(ns lcmap.aardvark.setup
  ""
  (:require [lcmap.aardvark.db :as db]
            [lcmap.aardvark.event :as event]
            [lcmap.aardvark.setup.database :as setup-db]
            [lcmap.aardvark.setup.event :as setup-event]
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
   ;; start minimal states needed to create configuration
   (-> (mount/with-args {:config (util/read-edn cfg)})
       (mount/start #'db/db-cluster #'event/amqp-channel))
   ;;
   (setup-db/setup cql)
   (setup-event/setup eqb)
   ;;
   (mount/stop #'db/db-cluster #'event/amqp-channel))
