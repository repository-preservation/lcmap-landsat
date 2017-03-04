(ns lcmap.aardvark.setup.event
  "RabbitMQ setup functions.

  Creates queues, exchanges, and bindings."
  (:require [clojure.tools.logging :as log]
            [lcmap.aardvark.event :as event]
            [lcmap.aardvark.util :as util]
            [langohr.exchange :as le]
            [langohr.queue :as lq]))

(defn create-exchanges
  [ch exchanges]
  (doseq [{:keys [:name :type :opts]} exchanges]
    (log/debugf "creating exchange: %s" name)
    (le/declare ch name type opts)))

(defn create-queues
  [ch queues]
  (doseq [{:keys [:name :opts]} queues]
    (log/debugf "creating queue: %s" name)
    (lq/declare event/amqp-channel name opts)))

(defn create-bindings
  [ch bindings]
  (doseq [{:keys [:exchange :queue :opts]} bindings]
    (log/debugf "binding %s to %s with opts %s"
                exchange queue opts)
    (lq/bind ch queue exchange opts)))

(defn setup
  ""
  ([edn-path ch]
   (if-let [wiring (util/read-edn edn-path)]
     (do
       (log/infof "creating exchanges, queues, and bindings")
       (create-exchanges ch (:exchanges wiring))
       (create-queues    ch (:queues wiring))
       (create-bindings  ch (:bindings wiring)))))
  ([edn-path]
   (setup edn-path event/amqp-channel)))
