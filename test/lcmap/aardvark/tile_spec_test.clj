(ns lcmap.aardvark.tile-spec-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [lcmap.aardvark.fixtures :as fixtures]
            [lcmap.aardvark.shared :as shared]
            [lcmap.aardvark.tile-spec :as tile-spec]
            [lcmap.aardvark.util :as util]))

(use-fixtures :once fixtures/with-services)

(use-fixtures :each fixtures/with-data)

(deftest save-tile-spec-test
  (testing "saving a tile-spec returns true"
    (let [tile-specs (util/read-edn "tile-specs/L5.edn")
          result (tile-spec/save (first tile-specs))]
      (= true result))))

(deftest tile-spec-tagging-test
  (testing "tags include explict data and inferred data"
    (let [tile-specs (util/read-edn "tile-specs/L5.edn")
          result (tile-spec/+tags (first tile-specs))
          tags (set (:tags result))]
      (is (contains? tags "cfmask"))
      (is (contains? tags "LANDSAT")))))

(deftest get-tile-spec-test
  (testing "using document ID to get tile-spec"
    (let [results (tile-spec/get "LANDSAT_5/TM/sr_band1")]
      (is (some? results)))))

(deftest search-tile-spec-test
  (testing "simple search condition"
    (let [results (tile-spec/search "cfmask")
          ubids (set (map :ubid results))]
      (is (contains? ubids "LANDSAT_5/TM/cfmask"))
      (is (contains? ubids "LANDSAT_7/ETM/cfmask_conf"))))
  (testing "complex search condition"
    (let [results (tile-spec/search "red AND NOT toa")
          ubids (set (map :ubid results))]
      (is (contains? ubids "LANDSAT_5/TM/sr_band3"))
      (is (contains? ubids "LANDSAT_7/ETM/sr_band3"))))
  (testing "even more complex search"
    (let [results (tile-spec/search "((tm AND cloud) OR band3) AND NOT shadow AND 5")
          actual (set (map :ubid results))
          expected #{"LANDSAT_5/TM/sr_cloud_qa"
                     "LANDSAT_5/TM/sr_adjacent_cloud_qa"
                     "LANDSAT_5/TM/cfmask_conf"
                     "LANDSAT_5/TM/cfmask"
                     "LANDSAT_5/TM/sr_band3"
                     "LANDSAT_5/TM/toa_band3"}]
      (is (empty? (clojure.set/difference actual expected))))))

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
