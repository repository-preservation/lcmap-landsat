(ns lcmap.aardvark.server
  "Aardvark server related functions.

  This namespace provides isolation for the kind of HTTP server
  used to run the REST API. See `lcmap.aardvark.handler` to see
  how routes and middleware are configured."
  (:require [clojure.tools.logging :as log]
            [lcmap.aardvark.config :refer [config]]
            [lcmap.aardvark.handler :refer [handler]]
            [mount.core :refer [defstate]]
            [ring.adapter.jetty :refer [run-jetty]]))

(defstate server
  :start (let [args (:http config)]
           (log/debugf "starting server: %s" args)
           (run-jetty handler args))
  :stop  (do
           (log/debugf "stopping server")
           (.stop server)))
