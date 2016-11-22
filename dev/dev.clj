(ns dev
  (:require
            [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [lcmap.aardvark.config :as config]
            [clojure.java.io :as io]
            [mount.core :as mount]
            [lcmap.aardvark.state :refer [config]]
            [uberconf.core :as uberconf]))

(defn start
  "Start dev system with a replacement config namespace"
  []
  (let [cfg (config/build {:edn (io/resource "lcmap-landsat-dev.edn")})]
    (mount/start-with {#'lcmap.aardvark.state/config cfg})))

(defn stop
  "Stop system"
  []
  (mount/stop))

(defn go
  "Prepare and start a system"
  []
  (start)
  :ready)

(defn reset
  "Stop, refresh, and start a system."
  []
  (stop)
  (refresh :after `go))
