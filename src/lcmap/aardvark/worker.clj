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

(declare worker-channel)

(defn handle-delivery
  [ch metadata payload]
  (let [source (event/decode-message metadata payload)]
    (log/debugf "deliver: %s" metadata)
    (log/debugf "content: %s" source)
    (tile/process source)
    (lb/ack worker-channel (metadata :delivery-tag))))

(defn handle-consume
  [consumer-tag]
  (log/debugf "consume ok: %s" consumer-tag))

;; Worker related state

(defstate worker-channel
  :start (let [channel (lch/open event/amqp-connection)]
           (lb/qos channel 1)
           channel)
  :stop (lch/close worker-channel))

(defstate worker-exchange
  :start (let [exchange-name (get-in config [:worker :exchange :name])]
           (log/debugf "creating worker exchange: %s" exchange-name)
           (le/declare worker-channel exchange-name "topic" {:durable true})))

(defstate worker-queue
  :start (let [queue-name (get-in config [:worker :queue :name])]
           (log/debugf "creating worker queue: %s" queue-name)
           (lq/declare worker-channel queue-name {:durable true
                                                      :exclusive false
                                                      :auto-delete false})))

(defstate worker-binding
  :start (let [dest     (get-in config [:worker :queue :name])
               bindings (get-in config [:worker :queue :bind])]
           (log/debugf "parsed config: %s" config)
           (doseq [[src routing-key] bindings]
             (log/debugf "binding %s to %s" dest src)
             (lq/bind worker-channel
                      dest
                      src
                      {:routing-key routing-key}))))

(defstate worker-consumer
  :start (let [f {:handle-delivery-fn handle-delivery
                  :handle-consume-ok-fn handle-consume}
               queue-name (:queue worker-queue)
               worker-fn (lcons/create-default event/amqp-channel f)]
           (log/debugf "starting worker consumer: %s" queue-name)
           (lb/consume worker-channel queue-name worker-fn))
  :stop  (let []
           (log/debug "stopping worker consumer: %s" worker-consumer)
           (lb/cancel worker-channel worker-consumer)))

(def worker worker-consumer)
