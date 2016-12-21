(ns lcmap.aardvark.fixtures
  (:require [lcmap.aardvark.tile-spec :as tile-spec]
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
                     :keyspace_name "lcmap_landsat_dev"
                     :table_name "conus"})

(defn load-tile-spec []
  (tile-spec/process L5 tile-spec-opts)
  (tile-spec/process L7 tile-spec-opts))
