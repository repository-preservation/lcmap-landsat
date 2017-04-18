(ns lcmap.aardvark.worker
  "Message handling related functions.

  A worker creates chips from ESPA archives. Each archive can take
  several minutes to process; handling these requests synchronously
  as part of an HTTP request is not feasible.

  This namespace only provides a function that handles messages
  routed to the worker queue.
  "
  (:require [clojure.tools.logging :as log]
            [langohr.basic :as lb]
            [langohr.consumers :as lcons]
            [lcmap.aardvark.config :refer [config]]
            [lcmap.aardvark.event :as event]
            [lcmap.aardvark.chip :as chip]
            [mount.core :as mount :refer [defstate]]))

(defn handle-consume-ok
  "Callback fn; worker consumer is registered successfully."
  [consumer-tag]
  (log/debugf "worker consumer registered ok: %s" consumer-tag))

(defn handle-delivery
  "Called whenever a message is available on the worker's queue."
  [ch metadata payload]
  (try
    (let [source (event/decode-message metadata payload)]
      (log/debugf "consuming message: %s" metadata)
      (chip/process source)
      (lb/ack event/amqp-channel (metadata :delivery-tag)))
    (catch java.lang.RuntimeException ex
      (log/errorf "failed to process message %s" metadata))))

;; This handler is here for completeness; cancellation is part of
;; the worker lifecycle, and this provides information to a developer
;; about the state of the system.
(defn handle-cancel
  ""
  [consumer-tag]
  (log/debugf "consumer cancelled: %s" consumer-tag))

(defn handle-cancel-ok
  ""
  [& args]
  (log/debugf "cancel-ok")
  nil)

(defn start-worker
  "Create consumer of AMQP messages for worker."
  ;; This makes implicit/default configuration explicit. The built-in
  ;; default options are well documented elsewhere and are suitable
  ;; for our purposes, but warrant some explanation about why they
  ;; are used.
  ;;
  ;; * auto-ack is false because any unhandled errors will cause the
  ;;   worker process to fail and the message to return to the queue.
  ;; * exclusive is false because many workers should consume messages
  ;;   from the same queue.
  ;; * no-local is used to prevent messages from being sent to a consumer
  ;;   on the same connection that was used to send them. This application
  ;;   can run a server and worker in a single process, so it should
  ;;   be set to false.
  ;; * when the consumer-tag is blank it will be automatically assigned
  ;;   by RabbitMQ.
  ;;
  []
  (let [queue-name (get-in config [:worker :queue])
        callbacks  {:handle-cancel-fn handle-cancel
                    :handle-cancel-ok-fn handle-cancel-ok
                    :handle-consume-ok-fn handle-consume-ok
                    :handle-delivery-fn handle-delivery}
        consumer   (lcons/create-default event/amqp-channel callbacks)
        options    {:consumer-tag ""
                    :auto-ack     false
                    :exclusive    false
                    :no-local     false}]
    (log/debugf "bind consumer to queue: %s" queue-name)
    (lb/consume event/amqp-channel queue-name consumer options)))

(defn stop-worker
  "Cancel consumer, returns unack'd messages to queue."
  [consumer]
  (let [queue-name (get-in config [:worker :queue])]
    (try
      (log/debugf "cancel queue consumer: %s" queue-name)
      (lb/cancel event/amqp-channel consumer)
      (catch java.lang.RuntimeException ex
        (log/warn "could not gracefully cancel consumer")
        nil))))

;; After starting, this will contain an auto-assigned consumer-tag
;; string value (e.g. "amq.ctag--ShMiMaeEhswKop2ccp9Lg"). This tag
;; is tracked so that it can be used to stop consumer gracefully.

(defstate worker
  :start (do
           (log/info "start worker")
           (start-worker))
  :stop  (do
           (log/info "stop worker")
           (stop-worker worker)))
