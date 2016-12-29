(ns lcmap.aardvark.tile-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all]
            [lcmap.aardvark.fixtures]
            [lcmap.aardvark.shared :refer :all :as shared]
            [lcmap.aardvark.tile :as tile]
            [lcmap.aardvark.tile-spec :as tile-spec]
            [lcmap.aardvark.util :as util]))

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

(deftest landsat-worker-support
  (with-system

    (map #(tile-spec/process % tile-spec-opts) [L5 L7])

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
      (is (= :fail (tile/process corrupt-source))))))


(def space-time {:x -2062080 :y 2952960 :acquired ["2000-01-05" "2000-01-30"]})
(def one-ubid {:ubids ["LANDSAT_7/ETM/toa_qa"]})
(def two-ubid {:ubids ["LANDSAT_7/ETM/toa_qa"
                       "LANDSAT_5/TM/sr_band1"]})
(def bad-ubid {:ubids ["Not a ubid"]})
(def mux-ubid {:ubids ["Not a ubid"
                       "LANDSAT_7/ETM/toa_qa"
                       "LANDSAT_5/TM/sr_band1"]})

(deftest find
  (with-system
    (map #(tile-spec/process % tile-spec-opts) [L5 L7])
    (map #(tile/process %) [L5 L7])
    (testing "Test a single ubid"
      (is (= 1 (count (tile/find (merge space-time one-ubid))))))
    (testing "Test two ubids"
      (is (= 2 (count (tile/find (merge space-time two-ubid))))))
    (testing "Test no valid ubid supplied"
      (is (= 0 (count (tile/find (merge space-time bad-ubid))))))
    (testing "Test bad ubid mixed with valid ubids"
      (is (= 2 (count (tile/find (merge space-time mux-ubid))))))))
