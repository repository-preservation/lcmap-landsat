(ns lcmap.aardvark.util-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [lcmap.aardvark.shared :refer :all]
            [lcmap.aardvark.util :as util]
            [me.raynes.fs :as fs]))

(deftest checksum-tests
  (testing "checksum matches"
    (let [uri (str (io/as-url (io/resource "data/test-archive.tar.gz")))
          checksum "d2769e6390074dd52d88e82475a74d79"]
      (is (= checksum (util/checksum uri)))
      (is (util/verify uri checksum))))
  (testing "checksum does not match"
    (let [uri (str (io/as-url (io/resource "data/test-archive.tar.gz")))
          checksum "obviously-not-a-checksum"]
      (is (not= checksum (util/checksum uri))))))
