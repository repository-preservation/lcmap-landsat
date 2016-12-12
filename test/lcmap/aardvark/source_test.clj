(ns lcmap.aardvark.source-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [lcmap.aardvark.state :as state]
            [lcmap.aardvark.server :as server]
            [lcmap.aardvark.shared :refer :all]))

(def landsat-source {:id "LT50470282005313"
                     :uri (-> "data/landsat.tar.gz" io/resource io/as-url str)
                     :checksum "c7aae8568ee8be9347373dd44d7e14c4"})

(def invalid-source {:id "invalid-source"
                     :checksum "d2769e6390074dd52d88e82475a74d79"})

(deftest source-tests
  (with-system
    (testing "putting an invalid source"
      (let [resp (req :put "http://localhost:5679/landsat/source/invalid-source"
                      :form-params invalid-source)]
        (is (= 403 (:status resp)))))
    (testing "putting a valid source"
      (let [resp (req :put "http://localhost:5679/landsat/source/LT50470282005313"
                      :form-params landsat-source)]
        (is (= 202 (:status resp)))))
    (testing "getting a source that should exist"
      ;; the first PUT request is necessary, it creates the source
      ;; that is subsequenty retrieved.
      (let [_    (req :put "http://localhost:5679/landsat/source/LT50470282005313"
                      :form-params landsat-source)
            resp (req :get "http://localhost:5679/landsat/source/LT50470282005313")]
        (is (= 200 (:status resp)))))
    (testing "getting a source that should not exist"
      (let [resp (req :get "http://localhost:5679/landsat/source/not-found")]
        (is (= 404 (:status resp)))))))
