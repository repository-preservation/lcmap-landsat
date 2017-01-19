(ns lcmap.aardvark.worker
  "Aardvark worker related functions.

  A worker creates tiles and tile-specs from ESPA archives. This can
  take several minutes; handling these requests synchronously as part
  of an HTTP request is not feasible."
  (:require [clojure.tools.logging :as log]
            [langohr.core :as rmq]
            [langohr.basic :as lb]
            [langohr.channel :as lch]
            [langohr.queue :as lq]
            [langohr.exchange :as le]
            [langohr.consumers :as lcons]
            [lcmap.aardvark.config :refer [config]]
            [lcmap.aardvark.db :as db :refer [db-session]]
            [lcmap.aardvark.event :as event]
            [lcmap.aardvark.tile :as tile]
            [mount.core :as mount :refer [defstate]]))

;; RabbitMQ message handlers

(defn handle-delivery
  [ch metadata payload]
  (let [source (event/decode-message metadata payload)]
    (log/debugf "deliver: %s" metadata)
    (log/debugf "content: %s" source)
    (tile/process source)
    (lb/ack event/amqp-channel (metadata :delivery-tag))))

(defn handle-consume
  [consumer-tag]
  (log/debugf "consume ok: %s" consumer-tag))

(defstate worker-consumer
  :start (let [f {:handle-delivery-fn handle-delivery
                  :handle-consume-ok-fn handle-consume}
               queue (get-in config [:worker :queue])
               worker-fn (lcons/create-default event/amqp-channel f)]
           (log/debugf "starting worker consumer: %s" queue)
           (lb/consume event/amqp-channel queue worker-fn))
  :stop  (let []
           (log/debug "stopping worker consumer: %s" worker-consumer)
           (lb/cancel event/amqp-channel worker-consumer)))

(def worker worker-consumer)
