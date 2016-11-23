(ns lcmap.aardvark.state
  "Stateful system components are defined here (for convienience.)
To use, simply (require :refer [item]) from other namespaces.  Stateful items
must be started prior to using with mount.core/start.  Individual
states may be started/stopped as well with (mount/start #'the.namespace/item).
At dev/test time, namespaces can be replaced by using
(mount/start-with {#'the.namespace/item replacement_item}) instead of plain old
mount/start.  See https://github.com/tolitius/mount/blob/master/README.md"

  (:require [mount.core :refer [defstate]]
            [qbits.alia :as alia]
            [langohr.core :as rmq]
            [lcmap.aardvark.handler :as handler]
            [lcmap.aardvark.config :as cfg]
            [ring.adapter.jetty :refer [run-jetty]]))

(defstate config
  :start (cfg/build))

(defstate db
  :start (apply alia/cluster (:database config))
  :stop  (alia/shutdown db))

(defstate db-session
  :start (alia/connect db)
  :stop  (alia/shutdown db-session))

(defstate event
  :start (rmq/connect (:event config))
  :stop  (rmq/close event))

(defstate graph
  :start 33)
  ;;:stop  (.close graph))

(defstate tile-search
  "Simple map index of tile-spec terms."
  :start 33
  :stop 34)

(defstate handler
  :start (handler/landsat db))

(defstate http
  :start (run-jetty handler (:http config))
  :stop  (.stop http))
