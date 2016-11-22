(ns lcmap.aardvark.app-test
  (:require [clojure.test :refer :all]
            [org.httpkit.client :as http]
            [lcmap.aardvark.state :refer [config]]
            [lcmap.aardvark.shared :refer :all]))

(defn req
  ""
  ([method path headers]
   @(http/request {:method method
                   :url (str "http://localhost:5678" path)
                   :headers headers}))
  ([method path]
   (req method path {"Accept" "application/json"})))

(deftest app-tests
  (with-system
    (testing "search"
      (let [resp (req :get "/landsat")]
        (is (= 200 (:status resp)))))
    (testing "ingest"
      (let [resp (req :post "/landsat")]
        (is (= 201 (:status resp)))))
    (testing "delete"
      (let [resp (req :delete "/landsat")]
        (is (= 410 (:status resp)))))
    (testing "unsupported verbs"
      (let [resp (req :put "/landsat")]
        (is (= 405 (:status resp)))))
    (testing "search for unsupported type"
      (let [resp (req :get "/landsat" {"Accept" "application/foo"})]
        (is (= 406 (:status resp)))))))
