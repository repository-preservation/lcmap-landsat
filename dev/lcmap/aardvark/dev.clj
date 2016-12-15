(ns lcmap.aardvark.dev
  (:require [clojure.edn :as edn]
            [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [clojure.java.io :as io]
            [mount.core :as mount]
            [lcmap.aardvark.state :as state]
            [lcmap.aardvark.config :as config]
            [lcmap.aardvark.server :as server]
            [lcmap.aardvark.worker :as worker]
            [lcmap.aardvark.source :as source]
            [lcmap.aardvark.tile :as tile]
            [lcmap.aardvark.tile-spec :as tile-spec]
            [uberconf.core :as uberconf]))

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

(defn setup
  "Produce tile-specs and tiles for some sample Landsat 5/7 data."
  [source spec-opts]
  (tile-spec/process source spec-opts)
  (tile/process source))

(comment
  "This uses sample data to produce a tile-spec and tiles from
   an ESPA archive."
  (let [L7 {:id  "LE70460272000029"
            :uri (-> "ESPA/CONUS/ARD/LE70460272000029-SC20160826120223.tar.gz" io/resource io/as-url str)
            :checksum "e1d2f9b28b1f55c13ee2a4b7c4fc52e7"}
        spec-opts {:keyspace_name "lcmap_landsat_dev"
                   :table_name "conus"
                   :data_shape [128 128]}]
    (setup L7 spec-opts)))
