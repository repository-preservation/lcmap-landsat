(ns lcmap.aardvark.server
  "Aardvark HTTP server related functions.

  This namespace provides functions and states for running the
  LCMAP-Landsat REST API."
  (:require [cheshire.core :refer :all]
            [cheshire.generate :as json-gen :refer [add-encoder]]
            [compojure.core :refer :all]
            [clojure.tools.logging :as log]
            [langohr.exchange :as le]
            [langohr.queue :as lq]
            [lcmap.aardvark.config :refer [config]]
            [lcmap.aardvark.db :as db :refer [db-session]]
            [lcmap.aardvark.event :as event :refer [amqp-channel]]
            [lcmap.aardvark.landsat :as landsat]
            [lcmap.aardvark.middleware :refer [wrap-authenticate wrap-authorize]]
            [lcmap.aardvark.problem :as problem]
            [lcmap.aardvark.source :as source]
            [lcmap.aardvark.tile :as tile]
            [lcmap.aardvark.tile-spec :as tile-spec]
            [mount.core :refer [defstate] :as mount]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.accept :refer [wrap-accept]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.problem :refer [wrap-problem]]
            [ring.middleware.resource :refer [wrap-resource]])
  (:import [org.joda.time.DateTime]
           [org.apache.commons.codec.binary Base64]))

;;; This is the REST API entrypoint. All general middleware
;;; should be added here. Subordinate resources should be
;;; defined in other namespaces.

(defn make-handler
  "Build a middleware wrapped handler for app. This approach makes
  dependencies (components) available to handling functions."
  []
  (-> (landsat/resource)
      (wrap-resource "public")
      (wrap-accept)
      (wrap-authorize)
      (wrap-authenticate)
      (wrap-keyword-params)
      (wrap-params)
      #_(wrap-problem problem/transformer)))

;;; Server-related state

(defstate ring-handler
  :start (do
           (log/debugf "starting Ring handler")
           (make-handler)))

(defstate server
  :start (let [args (get-in config [:http])]
           (log/debugf "starting Jetty: %s" args)
           (run-jetty ring-handler args))
  :stop  (do
           (log/debugf "stopping Jetty")
           (.stop server)))

;; The exchange to which the server publishes messages. This
;; is used to indirectly notify the worker of events that will
;; result in processing data.

(defstate server-exchange
  :start (let [exchange-name (get-in config [:server :exchange])]
           (log/debugf "creating server exchange: %s" exchange-name)
           (le/declare amqp-channel exchange-name "topic" {:durable true})))

;; The queue from which the server might eventually consume
;; messages. There are currently no bindings or consumers,
;; this is really just a placeholder.

(defstate server-queue
  :start (let [queue-name (get-in config [:server :queue])]
           (log/debugf "creating server queue: %s" queue-name)
           (lq/declare event/amqp-channel queue-name {:durable true
                                                      :exclusive false
                                                      :auto-delete false})))

;; Encoders; turn objects into strings suitable for JSON responses.

(defn iso8601-encoder
  "Transform a Joda DateTime object into an ISO8601 string."
  [date-time generator]
  (.writeString generator (str date-time)))

(defn base64-encoder
  "Base64 encode a byte-buffer, usually raster data from Cassandra."
  [buffer generator]
  (log/debug "encoding HeapByteBuffer")
  (let [size (- (.limit buffer) (.position buffer))
        copy (byte-array size)]
    (.get buffer copy)
    (.writeString generator (Base64/encodeBase64String copy))))

(json-gen/add-encoder org.joda.time.DateTime iso8601-encoder)

(json-gen/add-encoder java.nio.HeapByteBuffer base64-encoder)
