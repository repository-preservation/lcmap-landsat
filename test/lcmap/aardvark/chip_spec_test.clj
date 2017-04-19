(ns lcmap.aardvark.chip-spec-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [lcmap.aardvark.fixtures :as fixtures]
            [lcmap.aardvark.shared :as shared]
            [lcmap.aardvark.chip-spec :as chip-spec]
            [lcmap.aardvark.util :as util]))

(use-fixtures :once fixtures/with-services)

(use-fixtures :each fixtures/with-data)

(deftest save-chip-spec-test
  (testing "saving a chip-spec returns true"
    (let [chip-specs (util/read-edn "chip-specs/L5.edn")
          result (chip-spec/save (first chip-specs))]
      (= true result))))

(deftest chip-spec-tagging-test
  (testing "tags include explict data and inferred data"
    (let [chip-specs (util/read-edn "chip-specs/L5.edn")
          result (chip-spec/+tags (first chip-specs))
          tags (set (:tags result))]
      (is (contains? tags "cfmask"))
      (is (contains? tags "LANDSAT")))))

(deftest get-chip-spec-test
  (testing "using document ID to get chip-spec"
    (let [results (chip-spec/get "LANDSAT_5/TM/sr_band1")]
      (is (some? results)))))

(deftest search-chip-spec-test
  (testing "simple search condition"
    (let [results (chip-spec/search "cfmask")
          ubids (set (map :ubid results))]
      (is (contains? ubids "LANDSAT_5/TM/cfmask"))
      (is (contains? ubids "LANDSAT_7/ETM/cfmask_conf"))))
  (testing "complex search condition"
    (let [results (chip-spec/search "red AND NOT toa")
          ubids (set (map :ubid results))]
      (is (contains? ubids "LANDSAT_5/TM/sr_band3"))
      (is (contains? ubids "LANDSAT_7/ETM/sr_band3"))))
  (testing "even more complex search"
    (let [results (chip-spec/search "((tm AND cloud) OR band3) AND NOT shadow AND 5")
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
    (let [result (chip-spec/process L5 spec-opts)]
      (is (= :done result))))
  (testing "processing Landsat 7 archive"
    (let [result (chip-spec/process L7 spec-opts)]
      (is (= :done result))))
  (testing "finding by UBID"
    (let [results (chip-spec/query {:ubid "LANDSAT_5/TM/sr_band1"})]
      (is (= 1 (count results)))
      (is (= "LANDSAT_5" (-> results first :satellite)))
      (is (= "TM" (-> results first :instrument))))))
