(ns lcmap.aardvark.app-test
  (:require [clojure.test :refer :all]
            [org.httpkit.client :as http]
            [lcmap.aardvark.shared :refer :all]))

(defn req [method path]
  @(http/request {:method method
                  :url (str "http://localhost:5678" path)
                  :headers {"Accept" "text/html"}}))

(deftest app-tests
  (with-system [system]
    (testing "search"
      (let [resp (req :get "/landsat")]
        (is (= 200 (:status resp)))))
    (testing "ingest"
      (let [resp (req :post "/landsat")]
        (is (= 201 (:status resp)))))
    (testing "delete"
      (let [resp (req :delete "/landsat")]
        (is (= 410 (:status resp)))))))
