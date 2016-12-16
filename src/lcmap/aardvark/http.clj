(ns lcmap.aardvark.http
  ""
  (:require [clojure.tools.logging :as log]
            [lcmap.aardvark.config :refer [config]]
            [mount.core :refer [defstate]]
            [ring.adapter.jetty :refer [run-jetty]]
            [lcmap.aardvark.handler :refer [handler]]))

(defstate http
  :start (let [args (:http config)]
           (log/debugf "starting http with: %s" args)
           (run-jetty handler args))
  :stop  (do
           (log/debugf "stopping http")
           (.stop http)))
