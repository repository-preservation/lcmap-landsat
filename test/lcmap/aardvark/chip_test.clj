(ns lcmap.aardvark.chip-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all]
            [lcmap.aardvark.fixtures :as fixtures]
            [lcmap.aardvark.chip :as chip]
            [lcmap.aardvark.chip-spec :as chip-spec]
            [lcmap.aardvark.util :as util]))

(use-fixtures :once fixtures/with-services)

(use-fixtures :each fixtures/with-data)

(def L7 {:id "LE07_CU_014007_20150223_20170330_C01_V01_BT"
         :uri (-> "ARD/LE07_CU_014007_20150223_20170330_C01_V01_BT.tar" io/resource io/as-url str)
         :checksum "93ab262902e3199e1372b6f5e2491a98"})

(def invalid-source {:id "invalid-archive"
                     :uri (-> "data/test-archive.tar" io/resource io/as-url str)
                     :checksum "d2769e6390074dd52d88e82475a74d79"})

(def corrupt-source {:id "corrupt-archive"
                     :uri (-> "data/bad-archive.tar" io/resource io/as-url str)
                     :checksum "bcde049c7be432bb359bab9097a3dcf0"})

(def missing-source {:id "missing-archive"
                     :uri (-> "file:///data/missing-archive.tar" io/as-url str)
                     :checksum "c7aae8568ee8be9347373dd44d7e14c4"})

(deftest landsat-chip-processing-tests
  (testing "a Landsat 7 archive"
    (is (= :done (chip/process L7))))
  (testing "an valid archive with an invalid checksum"
    (is (= :fail (chip/process (assoc L7 :checksum "is-different")))))
  (testing "an archive that does not exist"
    (is (= :fail (chip/process missing-source))))
  (testing "an archive that can't be decompressed"
    (is (= :fail (chip/process corrupt-source))))
  (testing "an archive that isn't ESPA output"
    (is (= :fail (chip/process corrupt-source)))))

(def space-time {:x  -321585 :y 2201805 :acquired ["2015-01-01" "2016-01-01"]})
(def one-ubid {:ubids ["LANDSAT_7/ETM/BTB6"]})
(def two-ubid {:ubids ["LANDSAT_7/ETM/BTB6" "LANDSAT_7/ETM/PIXELQA"]})
(def bad-ubid {:ubids ["NONE"]})
(def mux-ubid {:ubids ["NONE" "LANDSAT_7/ETM/BTB6" "LANDSAT_7/ETM/PIXELQA"]})

(deftest find-tests
  (doall (map #(chip/process %) [L7]))
  (testing "single valid UBID"
    (is (= 1 (count (chip/find (merge space-time one-ubid))))))
  (testing "multiple valid UBIDs"
    (is (= 2 (count (chip/find (merge space-time two-ubid))))))
  (testing "single valid UBID"
    (is (= 0 (count (chip/find (merge space-time bad-ubid))))))
  (testing "some valid, some invalid UBIDs"
    (is (= 2 (count (chip/find (merge space-time mux-ubid)))))))

(deftest query-tests
  (testing "correct query"
    (let [query (chip/conform {:ubid "LANDSAT_7/ETM/BTB6"
                               :x "-123"
                               :y "123"
                               :acquired "2010/2012"})]
      (is (= -123 (query :x)))
      (is (= 123 (query :y)))
      (is (= ["LANDSAT_7/ETM/BTB6"] (query :ubids)))
      (is (str (-> query :acquired first)))
      (is (str (-> query :acquired last)))))
  (testing "conformance and validation"
    (let [errors (-> {:x "0" :y "0" :ubid "LANDSAT_7/ETM/BTB6"}
                     chip/conform
                     chip/validate)]
      (is (= errors {:acquired nil}))))
  (testing "conformance of points"
    (is (= 0 (-> {:x "0"} chip/conform :x)))
    (is (= 1 (-> {:y "1"} chip/conform :y)))
    (is (= nil (-> {:x "bacon"} chip/conform :x))))
  (testing "conformance of time interval"
    (let [[t1 t2] (-> {:acquired "2010/2012"} chip/conform :acquired)]
      (is (= org.joda.time.DateTime (type t1)))
      (is (= org.joda.time.DateTime (type t2)))))
  (testing "conformance of UBIDs"
    (is (= ["foo"] (-> {:ubid "foo"} chip/conform :ubids)))))
