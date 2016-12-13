(ns lcmap.aardvark.tile-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all]
            [lcmap.aardvark.fixtures]
            [lcmap.aardvark.shared :refer :all :as shared]
            [lcmap.aardvark.tile :as tile]
            [lcmap.aardvark.tile-spec :as tile-spec]
            [lcmap.aardvark.util :as util]))

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

(deftest landsat-resource
  (with-system
    (testing "search"
      (let [resp (req :get "http://localhost:5679/landsat/tiles"
                      :headers {"Accept" "application/json"}
                      :form-params {:x 0 :y 0
                                    :ubid "LANDSAT_5/TM/sr_band1"
                                    :acquired "2000-01-01/2005-01-01"})]
        (is (= 200 (:status resp)))))
    (testing "search for unsupported type"
      (let [resp (req :get "http://localhost:5679/landsat/tiles"
                      :form-params {:x 0 :y 0
                                    :ubid "LANDSAT_5/TM/sr_band1"
                                    :acquired "2000-01-01/2005-01-01"}
                      :headers {"Accept" "application/foo"})]
        (is (= 406 (:status resp)))))))
