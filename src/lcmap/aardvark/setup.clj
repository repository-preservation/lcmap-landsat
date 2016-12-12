(ns lcmap.aardvark.setup
  "Functions for preparing system components with schemas, etc..."
  (:require [clojure.tools.logging :as log]
            [lcmap.aardvark.state :as state]))

(defn cassandra [db]
  (log/debug "setting up schema"))

;; Should these be managed in state instead?

#_(defn rabbitmq [ch cfg]
  (log/debug "setting up exchanges, queues, and bindings")
  (let [exchange-name (get-in cfg [:event :scene-exchange])
        queue-name    (get-in cfg [:event :ingest-queue])
        exchange      (le/direct ch exchange-name)
        queue         (lq/declare ch queue-name)]
    (lq/bind ch "lcmap.landsat.ingest" "lcmap.landsat.scenes")))

#_(defn setup []
  (rabbitmq state/event-channel state/config))
