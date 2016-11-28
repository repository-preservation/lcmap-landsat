(ns lcmap.aardvark.state
  "Stateful system components are defined here (for convienience.)
To use, simply (require :refer [item]) from other namespaces.  Stateful items
must be started prior to using with mount.core/start.  Individual
states may be started/stopped as well with (mount/start #'the.namespace/item).
At dev/test time, namespaces can be replaced by using
(mount/start-with {#'the.namespace/item replacement_item}) instead of plain old
mount/start.  See https://github.com/tolitius/mount/blob/master/README.md"
  (:require [clojure.tools.logging :as log]
            [mount.core :refer [args defstate stop] :as mount]
            [qbits.alia :as alia]
            [langohr.core :as rmq]
            [lcmap.aardvark.handler :as handler]
            [lcmap.aardvark.config :as cfg]
            [ring.adapter.jetty :refer [run-jetty]]))

(defstate config
  :start (let [args ((mount/args) :config)]
           (log/debugf "starting config with: %s" args)
           (cfg/build args)))

(defstate db
  :start (let [args (:database config)]
           (log/debugf "starting db with: %s" args)
           (apply alia/cluster args))
  :stop  (do
           (log/debugf "stopping db")
           (alia/shutdown db)))

(defstate db-session
  :start (do
           (log/debugf "starting db session")
           (alia/connect db))
  :stop  (do
           (log/debugf "stopping db session")
           (alia/shutdown db-session)))

(defstate event
  :start (let [args (:event config)]
           (log/debugf "starting event %s" args)
           (rmq/connect args))
  :stop  (do
           (log/debugf "stopping event")
           (rmq/close event)))

(defstate graph
  :start 33)

(defstate tile-search
  :start 33
  :stop  34)

(defstate http-handler
  :start (handler/landsat db))

(defstate http
  :start (let [args (:http config)]
           (log/debugf "starting http with: %s" args)
           (run-jetty http-handler args))
  :stop  (do
           (log/debugf "stopping http")
           (.stop http)))

(defstate hook
  :start (do
           (log/debugf "registering shutdown handler")
           (.addShutdownHook (Runtime/getRuntime)
                             (Thread. #(mount/stop)))))
