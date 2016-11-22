(ns lcmap.aardvark.state
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
  :start (str "hello")
  :stop  (str "goodbye"))

(defstate handler
  :start (handler/landsat db))

(defstate http
  :start (run-jetty handler (:http config))
  :stop  (.stop http))
