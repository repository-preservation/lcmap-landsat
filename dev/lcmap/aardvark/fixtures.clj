(ns lcmap.aardvark.fixtures
  (:require [lcmap.aardvark.tile-spec :as tile-spec]
            [clojure.java.io :as io]))

(def data-path "dev/resources/data/landsat/")

(def landsat-source {:id "LT50470282005313"
                     :uri (-> "data/landsat.tar.gz" io/resource io/as-url str)
                     :checksum "c7aae8568ee8be9347373dd44d7e14c4"})

(def tile-spec-opts {:data_shape [256 256]
                     :keyspace_name "lcmap_landsat_dev"
                     :table_name "conus"})

(defn load-tile-spec []
  (tile-spec/process-scene data-path tile-spec-opts))
