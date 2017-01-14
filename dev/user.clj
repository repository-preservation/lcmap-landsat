(ns user
  (:require
   [cheshire.core :as json]
   [clojure.tools.namespace.repl :refer [refresh refresh-all]]
   [clojure.java.io :as io]
   [clojure.edn :as edn]
   [mount.core :as mount]
   [lcmap.aardvark.config :as config]
   [lcmap.aardvark.server :as server]
   [lcmap.aardvark.worker :as worker]
   [uberconf.core :as uberconf]
   [lcmap.aardvark.tile-spec :as tile-spec]
   [lcmap.aardvark.tile :as tile]
   [clojure.java.io :as io]))

(def L5 {:id  "LT50460272000005"
         :uri (-> "ESPA/CONUS/ARD/LT50460272000005-SC20160826121722.tar.gz" io/resource io/as-url str)
         :checksum "9aa16eac2b9b8a20301ad091ceb9f3f4"})

(def L7 {:id  "LE70460272000029"
         :uri (-> "ESPA/CONUS/ARD/LE70460272000029-SC20160826120223.tar.gz" io/resource io/as-url str)
         :checksum "e1d2f9b28b1f55c13ee2a4b7c4fc52e7"})

(def tile-spec-opts {:data_shape [128 128]
                     :name "conus"})

(defn load-tile-spec []
  (tile-spec/process L5 tile-spec-opts)
  (tile-spec/process L7 tile-spec-opts))

(defn dump-tile-spec [path]
  (let [sorted (map (fn [kvs] (into (sorted-map) kvs)) (tile-spec/all))
        output (json/generate-string {:pretty true})]
    (spit path output)))

(defn load-tiles []
  (tile/process L5)
  (tile/process L7))

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
