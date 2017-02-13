(ns lcmap.aardvark.event
  "Provide RabbitMQ connections, channels, and message handling helpers."
  (:require [camel-snake-kebab.core :refer [->snake_case_keyword]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [dire.core :as dire]
            [langohr.core :as rmq]
            [langohr.basic :as lb]
            [langohr.channel :as lch]
            [langohr.exchange :as le]
            [langohr.queue :as lq]
            [lcmap.aardvark.config :refer [config]]
            [lcmap.aardvark.util :as util]
            [mount.core :refer [args defstate stop] :as mount]))

(defn decode-message
  "Convert byte payload to JSON."
  [metadata payload]
  (transform-keys ->snake_case_keyword (json/decode (String. payload "UTF-8"))))

(dire/with-handler! #'decode-message
  java.lang.Exception
  (fn [e & args]
    (log/debugf "cannot decode message: %s"
                {:metadata (first args) :payload (second args) :exception e})
    nil))

(declare amqp-connection amqp-channel)

(defn start-amqp-connection
  "Open RabbitMQ connection."
  []
  (try
    (let [args (:event config)
          opts (select-keys args [:host :port])]
      (log/debugf "starting RabbitMQ connection: %s" opts)
      (rmq/connect opts))
    (catch java.lang.RuntimeException ex
      (log/fatal "failed to start RabbitMQ connection"))))

(defn stop-amqp-connection
  "Close RabbitMQ connection."
  []
  (try
    (log/debugf "stopping RabbitMQ connection")
    (rmq/close amqp-connection)
    (catch java.lang.RuntimeException ex
      (log/error "failed to stop RabbitMQ connection"))
    (finally
      nil)))

(defstate amqp-connection
  :start (start-amqp-connection)
  :stop  (stop-amqp-connection))

(defn start-amqp-channel
  "Create RabbitMQ channel."
  []
  (try
    (log/debugf "starting RabbitMQ channel")
    (let [channel (lch/open amqp-connection)]
      (lb/qos channel 1)
      channel)
    (catch java.lang.RuntimeException ex
      (log/fatal "failed to start RabbitMQ channel")
      (log/fatal ex))))

(defn stop-amqp-channel
  "Close RabbitMQ channel."
  []
  (try
    (log/debugf "stopping RabbitMQ channel")
    (lch/close amqp-channel)
    (catch com.rabbitmq.client.AlreadyClosedException ex
      (log/warnf "failed to stop RabbitMQ channel")
      (log/warnf ex))
    (finally
      nil)))

(defstate amqp-channel
  :start (start-amqp-channel)
  :stop  (stop-amqp-channel))

(defn create-exchanges
  [exchanges]
  (doseq [exchange exchanges]
    (log/debugf "creating exchange: %s" (:name exchange))
    (le/declare amqp-channel
                (:name exchange)
                (:type exchange)
                (:opts exchange))))

(defn create-queues
  [queues]
  (doseq [queue queues]
    (log/debugf "creating queue: %s" (:name queue))
    (lq/declare amqp-channel (:name queue) (:opts queue))))

(defn create-bindings
  [bindings]
  (doseq [binding bindings]
    (log/debugf "binding %s to %s with opts %s"
                (:exchange binding)
                (:queue binding)
                (:opts binding))
    (lq/bind amqp-channel
             (:queue binding)
             (:exchange binding)
             (:opts binding))))

(defstate event-schema
  :start (do
           (log/debug "creating exchanges, queues, and bindings")
           (let [wiring (util/read-edn "event.setup.edn")]
             (create-exchanges (:exchanges wiring))
             (create-queues (:queues wiring))
             (create-bindings (:bindings wiring))))
  :stop (do
          (log/debug "automatic removal of exchanges, queues, and bindings not supported")))
