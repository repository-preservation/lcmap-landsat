(ns lcmap.aardvark.util-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [lcmap.aardvark.shared :refer :all]
            [lcmap.aardvark.util :as util]
            [me.raynes.fs :as fs]))

(deftest archive-tests
  (testing "temporarily expanding .tar.xz"
    (util/with-temp [dir (io/resource "data/test-archive.tar.xz")]
      (is (fs/exists? dir))))
  (testing "temporarily expanding .tar.gz"
    (util/with-temp [dir (io/resource "data/test-archive.tar.gz")]
      (is (fs/exists? dir))))
  (testing "temporarily expanding .tar.bz2"
    (util/with-temp [dir (io/resource "data/test-archive.tar.bz2")]
      (is (fs/exists? dir)))))

(deftest checksum-tests
  (testing "checksum matches"
    (let [source {:uri (str (io/as-url (io/resource "data/test-archive.tar.gz")))
                  :checksum "d2769e6390074dd52d88e82475a74d79"}]
      (is (= (source :checksum) (util/checksum source)))))
  (testing "checksum does not match"
    (let [source {:uri (str (io/as-url (io/resource "data/test-archive.tar.gz")))
                  :checksum "obviously-not-a-checksum"}]
      (is (not= (source :checksum) (util/checksum source))))))
