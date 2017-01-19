(ns lcmap.aardvark.event
  "Provide RabbitMQ connections, channels, and message handling
  helpers.

  Exchanges, queues, bindings, and  consumers are created in
  namespaces that define function responsible for producing
  and handling messages."
  (:require [camel-snake-kebab.core :refer [->snake_case_keyword]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [langohr.core :as rmq]
            [langohr.basic :as lb]
            [langohr.channel :as lch]
            [langohr.exchange :as le]
            [langohr.queue :as lq]
            [lcmap.aardvark.config :refer [config]]
            [mount.core :refer [args defstate stop] :as mount]))

(defn decode-message
  "Convert byte payload to JSON."
  [metadata payload]
  (transform-keys ->snake_case_keyword (json/decode (String. payload "UTF-8"))))

(declare amqp-connection amqp-channel)

(defn start-amqp-connection
  "Open RabbitMQ connection."
  []
  (try
    (let [args (:event config)
          opts (select-keys args [:host :port])]
      (log/debugf "starting RabbitMQ connection: %s" opts)
      (rmq/connect (select-keys args [:host :port])))
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
    (lch/open amqp-connection)
    (catch java.lang.RuntimeException ex
      (log/fatal "failed to start RabbitMQ channel"))))

(defn stop-amqp-channel
  "Close RabbitMQ channel."
  []
  (try
    (log/debugf "stopping RabbitMQ channel")
    (lch/close amqp-channel)
    (catch com.rabbitmq.client.AlreadyClosedException e
      (log/warnf "failed to stop RabbitMQ channel"))
    (finally
      nil)))

(defstate amqp-channel
  :start (start-amqp-channel)
  :stop  (stop-amqp-channel))

(defstate configure-amqp-channel
  :start (do (lb/qos amqp-channel 1) true))

(defstate exchanges
  :start (let [configs (get-in config [:event :exchanges])]
           (doseq [exchange configs]
             (log/debugf "Creating Exchange: %s" (:name exchange))
             (le/declare amqp-channel
                         (:name exchange)
                         (:type exchange)
                         (:opts exchange)))
           configs))

(defstate queues
  :start (let [configs (get-in config [:event :queues])]
           (doseq [queue configs]
             (log/debugf "Creating Queue: %s" (:name queue))
             (lq/declare amqp-channel (:name queue) (:opts queue)))
           configs))

(defstate bindings
  :start (let [exchanges_state exchanges
               queues_state    queues
               configs         (get-in config [:event :bindings])]
           (doseq [binder configs]
             (log/debugf "Binding %s to %s with opts %s"
                         (:exchange binder)
                         (:queue binder)
                         (:opts binder))
             (lq/bind amqp-channel
                      (:queue binder)
                      (:exchange binder)
                      (:opts binder)))
           configs))
