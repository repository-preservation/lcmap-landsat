(ns lcmap.aardvark.event
  "RabbitMQ related helpers and state."
  (:require [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [langohr.core :as rmq]
            [langohr.basic :as lb]
            [langohr.exchange :as le]
            [langohr.queue :as lq]
            [langohr.channel :as lch]
            [lcmap.aardvark.config :refer [config]]
            [lcmap.aardvark.db :refer [db-session]]
            [mount.core :refer [args defstate stop] :as mount]))


;; RabbitMQ helpers

(defn decode-message
  "Convert byte payload to JSON."
  [metadata payload]
  (transform-keys csk/->snake_case_keyword (json/decode (String. payload "UTF-8"))))

;; RabbitMQ related state. This defines only a connection and channel,
;; exchanges, queues, and bindings should be defined in namespaces that
;; correspond to specific behaviors. For example, the HTTP server will
;; publish messages to an exchange, that exchange should be defined in
;; the server namespace.

(defstate amqp-connection
  :start (let [args (:event config)]
           (log/debugf "starting RabbitMQ connection %s" (select-keys args [:host :port]))
           (rmq/connect (select-keys args [:host :port])))
  :stop  (do
           (log/debugf "stopping RabbitMQ connection")
           (rmq/close amqp-connection)))

(defstate amqp-channel
  :start (try
           (log/debugf "starting RabbitMQ channel")
           (lch/open amqp-connection))
  :stop  (try
           (log/debugf "stopping RabbitMQ channel")
           (lch/close amqp-channel)
           (catch com.rabbitmq.client.AlreadyClosedException e
             (log/errorf "failed to stop RabbitMQ channel"))
           (finally
             nil)))

(def event amqp-channel)
