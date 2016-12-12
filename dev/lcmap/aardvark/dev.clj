(ns lcmap.aardvark.dev
  (:require
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

(defn setup
  "Produce tile-specs and tiles for some sample Landsat 5/7 data."
  [source spec-opts]
  (tile-spec/process source spec-opts)
  (tile/process source))

;; This is included in the developer namespace for convenience.

(comment
  (let [L5 {:id  "LT50460272000005"
            :uri (-> "ESPA/CONUS/ARD/LT50460272000005-SC20160826121722.tar.gz" io/resource io/as-url str)
            :checksum "9aa16eac2b9b8a20301ad091ceb9f3f4"}
        L7 {:id  "LE70460272000029"
            :uri (-> "ESPA/CONUS/ARD/LE70460272000029-SC20160826120223.tar.gz" io/resource io/as-url str)
            :checksum "e1d2f9b28b1f55c13ee2a4b7c4fc52e7"}
        spec-opts {:keyspace_name "lcmap_landsat_dev"
                   :table_name "conus"
                   :data_shape [128 128]}]
    (setup L5 spec-opts)
    (setup L7 spec-opts))
  ;; Get the L5/L7 tile-specs
  (tile-spec/query {:ubid "LANDSAT_5/TM/sr_band1"})
  (tile-spec/query {:ubid "LANDSAT_7/ETM/sr_band1"})
  ;; Get some L5/L7 tiles
  (let [tile (tile/find {:x -2062080
                         :y  2952960
                         :ubid "LANDSAT_5/TM/sr_band1"
                         :acquired ["2000-01-01" "2006-01-01"]})]
    (-> tile first :data))
  (let [tile (tile/find {:x -2062080
                         :y  2952960
                         :ubid "LANDSAT_7/ETM/sr_band1"
                         :acquired ["2000-01-01" "2006-01-01"]})]
    (-> tile first :data))
  (count (tile-spec/all)))
