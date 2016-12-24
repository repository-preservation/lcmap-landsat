(ns lcmap.aardvark.dev
  (:require
   [clojure.tools.namespace.repl :refer [refresh refresh-all]]
   [clojure.java.io :as io]
   [clojure.edn :as edn]
   [mount.core :as mount]
   [lcmap.aardvark.config :as config]
   [lcmap.aardvark.server :as server]
   [lcmap.aardvark.worker :as worker]
   [uberconf.core :as uberconf]
   [lcmap.aardvark.tile-spec :as tile-spec]
   [clojure.java.io :as io]))

(def data-path "ESPA/CONUS/ARD")

(def L5 {:id "LT50470282005313"
         :uri (-> "ESPA/CONUS/ARD/LT50470282005313-SC20160826122108.tar.gz"
                  io/resource io/as-url str)
         :checksum "c7aae8568ee8be9347373dd44d7e14c4"})

(def L7 {:id "LE70460272005314"
         :uri (-> "ESPA/CONUS/ARD/LE70460272005314-SC20160826120705.tar.gz"
                  io/resource io/as-url str)
         :checksum "2e8f29fb6cc66d5162a043a9a2c7eba5"})

(def tile-spec-opts {:data_shape [128 128]
                     :name "conus"})

(defn load-tile-spec []
  (tile-spec/process L5 tile-spec-opts)
  (tile-spec/process L7 tile-spec-opts))

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
