(ns lcmap.aardvark.tile-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all]
            [lcmap.aardvark.fixtures :as fixtures]
            [lcmap.aardvark.tile :as tile]
            [lcmap.aardvark.tile-spec :as tile-spec]
            [lcmap.aardvark.util :as util]))

(use-fixtures :once fixtures/with-services)

(use-fixtures :each fixtures/with-data)

(def tile-spec-opts {:data_shape [128 128]
                     :name "conus"})

(def L5 {:id  "LT50460272000005"
         :uri (-> "ESPA/CONUS/ARD/LT50460272000005-SC20160826121722.tar.gz" io/resource io/as-url str)
         :checksum "9aa16eac2b9b8a20301ad091ceb9f3f4"})

(def L7 {:id  "LE70460272000029"
         :uri (-> "ESPA/CONUS/ARD/LE70460272000029-SC20160826120223.tar.gz" io/resource io/as-url str)
         :checksum "e1d2f9b28b1f55c13ee2a4b7c4fc52e7"})

(def invalid-source {:id "invalid-archive"
                     :uri (-> "data/test-archive.tar.gz" io/resource io/as-url str)
                     :checksum "d2769e6390074dd52d88e82475a74d79"})

(def corrupt-source {:id "corrupt-archive"
                     :uri (-> "data/bad-archive.tar.gz" io/resource io/as-url str)
                     :checksum "bcde049c7be432bb359bab9097a3dcf0"})

(def missing-source {:id "missing-archive"
                     :uri (-> "file:///data/missing-archive.tar.gz" io/as-url str)
                     :checksum "c7aae8568ee8be9347373dd44d7e14c4"})

(deftest landsat-tile-processing-tests
  (doall (map #(tile-spec/process % tile-spec-opts) [L5 L7]))
  (testing "a Landsat 5 archive"
    (is (= :done (tile/process L5))))
  (testing "a Landsat 7 archive"
    (is (= :done (tile/process L7))))
  (testing "an valid archive with an invalid checksum"
    (is (= :fail (tile/process (assoc L5 :checksum "is-different")))))
  (testing "an archive that does not exist"
    (is (= :fail (tile/process missing-source))))
  (testing "an archive that can't be decompressed"
    (is (= :fail (tile/process corrupt-source))))
  (testing "an archive that isn't ESPA output"
    (is (= :fail (tile/process corrupt-source)))))

(def space-time {:x -2062080 :y 2952960 :acquired ["2000-01-05" "2000-01-30"]})

(def one-ubid {:ubids ["LANDSAT_7/ETM/toa_qa"]})

(def two-ubid {:ubids ["LANDSAT_7/ETM/toa_qa"
                       "LANDSAT_5/TM/sr_band1"]})

(def bad-ubid {:ubids ["Not a ubid"]})

(def mux-ubid {:ubids ["Not a ubid"
                       "LANDSAT_7/ETM/toa_qa"
                       "LANDSAT_5/TM/sr_band1"]})

;; This test uses data that no longer conforms with the tile-specs
;; of an operational system. Consequently, tile-specs have to be
;; saved that will work with the ingested data.

(deftest find-tests
  (testing "Testing a variety of queries on ingested data"
    (doall (map #(tile-spec/process % tile-spec-opts) [L5 L7]))
    (doall (map #(tile/process %) [L5 L7]))
    (is (= 1 (count (tile/find (merge space-time one-ubid)))))
    (is (= 2 (count (tile/find (merge space-time two-ubid)))))
    (is (= 0 (count (tile/find (merge space-time bad-ubid)))))
    (is (= 2 (count (tile/find (merge space-time mux-ubid)))))))

(deftest query-tests
  (testing "correct query"
    (let [query (tile/conform {:ubid "LANDSAT_5/TM/sr_band1"
                               :x "-123"
                               :y "123"
                               :acquired "2010/2012"})]
      (is (= -123 (query :x)))
      (is (= 123 (query :y)))
      (is (= ["LANDSAT_5/TM/sr_band1"] (query :ubids)))
      (is (str (-> query :acquired first)))
      (is (str (-> query :acquired last)))))
  (testing "conformance and validation"
    (let [errors (-> {:x "0" :y "0" :ubid "LANDSAT_5/TM/sr_band1"}
                     tile/conform
                     tile/validate)]
      (is (= errors {:acquired nil}))))
  (testing "conformance of points"
    (is (= 0 (-> {:x "0"} tile/conform :x)))
    (is (= 1 (-> {:y "1"} tile/conform :y)))
    (is (= nil (-> {:x "bacon"} tile/conform :x))))
  (testing "conformance of time interval"
    (let [[t1 t2] (-> {:acquired "2010/2012"} tile/conform :acquired)]
      (is (= org.joda.time.DateTime (type t1)))
      (is (= org.joda.time.DateTime (type t2)))))
  (testing "conformance of UBIDs"
    (is (= ["foo"] (-> {:ubid "foo"} tile/conform :ubids)))))
