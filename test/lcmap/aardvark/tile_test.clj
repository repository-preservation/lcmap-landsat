(ns lcmap.aardvark.tile-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all]
            [lcmap.aardvark.shared :refer :all]
            [lcmap.aardvark.tile :as tile]
            [lcmap.aardvark.tile-spec :as tile-spec]
            [lcmap.aardvark.util :as util]))

(def landsat-source {:id "LT50470282005313"
                     :uri (-> "data/landsat.tar.gz" io/resource io/as-url str)
                     :checksum "c7aae8568ee8be9347373dd44d7e14c4"})

(def invalid-source {:id "invalid-archive"
                     :uri (-> "data/test-archive.tar.gz" io/resource io/as-url str)
                     :checksum "d2769e6390074dd52d88e82475a74d79"})

(def corrupt-source {:id "corrupt-archive"
                     :uri (-> "data/bad-archive.tar.gz" io/resource io/as-url str)
                     :checksum "bcde049c7be432bb359bab9097a3dcf0"})

(def missing-source {:id "missing-archive"
                     :uri (-> "file:///data/missing-archive.tar.gz" io/as-url str)
                     :checksum "c7aae8568ee8be9347373dd44d7e14c4"})

(def tile-spec-opts {:data_shape [64 64]
                     :keyspace_name "lcmap_landsat_test"
                     :table_name "conus"})

(deftest landsat-worker-support
  ;; Test cases
  (with-system
    (tile-spec/process landsat-source tile-spec-opts)
    (testing "an archive that does not exist"
      (is (= :fail (tile/process missing-source))))
    (testing "an archive with an invalid checksum"
      (is (= :fail (tile/process (assoc landsat-source :checksum "is-different")))))
    (testing "an archive that can't be decompressed"
      (is (= :fail (tile/process corrupt-source))))
    (testing "an archive that isn't ESPA output"
      (is (= :fail (tile/process corrupt-source))))
    (testing "an archive that is ESPA output"
      (is (= :done (tile/process landsat-source))))))

(deftest landsat-resource
  (with-system
    (tile-spec/process landsat-source tile-spec-opts)
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
