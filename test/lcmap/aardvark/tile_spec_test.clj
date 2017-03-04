(ns lcmap.aardvark.tile-spec-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [lcmap.aardvark.fixtures :as fixtures]
            [lcmap.aardvark.tile-spec :as tile-spec]))

(use-fixtures :once fixtures/with-services)

(use-fixtures :each fixtures/with-data)

(def L5 {:id  "LT50460272000005"
         :uri (-> "ESPA/CONUS/ARD/LT50460272000005-SC20160826121722.tar.gz" io/resource io/as-url str)
         :checksum "9aa16eac2b9b8a20301ad091ceb9f3f4"})

(def L7 {:id  "LE70460272000029"
         :uri (-> "ESPA/CONUS/ARD/LE70460272000029-SC20160826120223.tar.gz" io/resource io/as-url str)
         :checksum "e1d2f9b28b1f55c13ee2a4b7c4fc52e7"})

(def spec-opts {:name "conus"
                :data_shape [128 128]})

(deftest test-processing
  (testing "processing Landsat 5 archive"
    (let [result (tile-spec/process L5 spec-opts)]
      (is (= :done result))))
  (testing "processing Landsat 7 archive"
    (let [result (tile-spec/process L7 spec-opts)]
      (is (= :done result))))
  (testing "finding by UBID"
    (let [results (tile-spec/query {:ubid "LANDSAT_5/TM/sr_band1"})]
      (is (= 1 (count results)))
      (is (= "LANDSAT_5" (-> results first :satellite)))
      (is (= "TM" (-> results first :instrument))))))
