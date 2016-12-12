(ns lcmap.aardvark.server-test
  "Full integration test of server and worker."
  (:require [clojure.test :refer :all]
            [lcmap.aardvark.server :as server]
            [lcmap.aardvark.shared :refer :all]))

(deftest landsat-tests
  (with-system
    (testing "entry-point"
      (let [resp (req :get "http://localhost:5679/landsat")]
        (is (= 200 (:status resp)))))
    (testing "unsupported verbs"
      (let [resp (req :put "http://localhost:5679/landsat")]
        (is (= 405 (:status resp))))
      (let [resp (req :delete "http://localhost:5679/landsat")]
        (is (= 405 (:status resp)))))
    (testing "search for unsupported type"
      (let [resp (req :get "http://localhost:5679/landsat"
                      :headers {"Accept" "application/foo"})]
        (is (= 406 (:status resp)))))))
