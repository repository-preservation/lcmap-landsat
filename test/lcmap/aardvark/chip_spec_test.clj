(ns lcmap.aardvark.chip-spec-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [lcmap.aardvark.fixtures :as fixtures]
            [lcmap.aardvark.shared :as shared]
            [lcmap.aardvark.chip-spec :as chip-spec]
            [lcmap.aardvark.util :as util]))

(use-fixtures :once fixtures/with-services)

(use-fixtures :each fixtures/with-data)

(def L7 {:id "LE07_CU_014007_20150223_20170330_C01_V01_SR"
         :uri (-> "ARD/LE07_CU_014007_20150223_20170330_C01_V01_SR.tar" io/resource io/as-url str)
         :checksum "6c06e8b4ce5e8bafb1fe02c26c704237"})

(def spec-opts {:name "conus"
                :data_shape [100 100]
                :chip_x   3000
                :chip_y  -3000
                :pixel_x  30.0
                :pixel_y -30.0
                :shift_x  2415.0
                :shift_y -195.0})

(deftest save-chip-spec-test
  (testing "saving a chip-spec returns true"
    (let [chip-specs (util/read-edn "chip-specs/L7.edn")
          result (chip-spec/save (first chip-specs))]
      (= true result))))

(deftest chip-spec-tagging-test
  (testing "tags include explict data and inferred data"
    (let [chip-specs (util/read-edn "chip-specs/L7.edn")
          result (chip-spec/+tags (first chip-specs))
          tags (set (:tags result))]
      (is (contains? tags "LINEAGEQA"))
      (is (contains? tags "LANDSAT")))))

(deftest get-chip-spec-test
  (testing "using document ID to get chip-spec"
    (let [results (chip-spec/get "LANDSAT_7/ETM/SRB1")]
      (is (some? results))
      results)))

(deftest search-chip-spec-test
  (testing "simple search condition"
    (let [results (chip-spec/search "SRCLOUDQA")
          ubids (set (map :ubid results))]
      (is (contains? ubids "LANDSAT_7/ETM/SRCLOUDQA"))))
  (testing "complex search condition"
    (let [results (chip-spec/search "red AND NOT toa")
          ubids (set (map :ubid results))]
      (is (contains? ubids "LANDSAT_7/ETM/SRB3"))))
  (testing "even more complex search"
    (let [results (chip-spec/search "((etm AND cloud) OR SRB3) AND NOT shadow AND 7")
          actual (set (map :ubid results))
          expected #{"LANDSAT_7/ETM/SRCLOUDQA"
                     "LANDSAT_7/ETM/SRB3"
                     "LANDSAT_7/ETM/TAB3"}]
      (is (empty? (clojure.set/difference actual expected))))))

(deftest test-processing
  (testing "processing Landsat 7 archive"
    (let [result (chip-spec/process L7 spec-opts)]
      (is (= :done result))))
  (testing "processing Landsat 7 archive"
    (let [result (chip-spec/process L7 spec-opts)]
      (is (= :done result))))
  (testing "finding by UBID"
    (let [results (chip-spec/query {:ubid "LANDSAT_7/ETM/SRB1"})]
      (is (= 1 (count results)))
      (is (= "LANDSAT_7" (-> results first :satellite)))
      (is (= "ETM" (-> results first :instrument))))))
