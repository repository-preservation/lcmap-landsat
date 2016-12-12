(ns lcmap.aardvark.tile-spec-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [lcmap.aardvark.shared :as shared]
            [lcmap.aardvark.tile-spec :as tile-spec]))

(def data-path "test/resources/data/landsat/")

(def landsat-source {:id "LT50470282005313"
                     :uri (-> "data/landsat.tar.gz" io/resource io/as-url str)
                     :checksum "c7aae8568ee8be9347373dd44d7e14c4"})

(def tile-spec-opts {:data_shape [256 256]
                     :keyspace_name "lcmap_landsat_test"
                     :table_name "conus"})

(deftest test-processing
  (shared/with-system
    (testing "saving an ESPA archive"
      (let [results (tile-spec/process-scene data-path tile-spec-opts)]
        (is (= 32 (count results)))))
    (testing "finding by UBID"
      (let [params {:ubid "LANDSAT_5/TM/sr_band1"}
            results (tile-spec/query params)]
        (is (= 1 (count results)))
        (is (= "LANDSAT_5" (-> results first :satellite)))
        (is (= "TM" (-> results first :instrument)))))))
