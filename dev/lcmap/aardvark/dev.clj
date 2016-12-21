(ns lcmap.aardvark.dev
  (:require
            [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [mount.core :as mount]
            [lcmap.aardvark.state :as state]
            [lcmap.aardvark.config :as config]
            [lcmap.aardvark.server :as server]
            [lcmap.aardvark.worker :as worker]
            [uberconf.core :as uberconf]
            [lcmap.aardvark.fixtures :as fixtures]))

(defn start
  "Start dev system with a replacement config namespace"
  []
  (let [cfg (edn/read-string (slurp (io/resource "lcmap-landsat.edn")))]
    (-> (mount/with-args {:config cfg})
        (mount/start))))

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
