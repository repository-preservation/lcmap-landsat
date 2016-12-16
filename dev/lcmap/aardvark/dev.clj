(ns lcmap.aardvark.dev
  (:require
            [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [clojure.java.io :as io]
            [mount.core :as mount]
            [lcmap.aardvark.state :as state]
            [lcmap.aardvark.config :as config]
            [lcmap.aardvark.server :as server]
            [lcmap.aardvark.worker :as worker]
            [uberconf.core :as uberconf]
            [lcmap.aardvark.fixtures :refer [load-tile-spec]]))

(defn start
  "Start dev system with a replacement config namespace"
  []
  (let [cfg (config/build {:edn (io/resource "lcmap-landsat.edn")})]
    (mount/start-with {#'lcmap.aardvark.config/config cfg})))

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
