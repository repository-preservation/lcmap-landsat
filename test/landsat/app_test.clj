(ns landsat.app-test
  (:require [clojure.test :refer :all]
            [org.httpkit.client :as http]
            [landsat.shared :refer :all]))

(defn req [method path]
  @(http/request {:method method
                  :url (str "http://localhost:5679/cat" path)
                  :headers {"Accept" "text/html"}}))

(deftest app-tests
  (with-system [system]
    (testing "search"
      (let [resp (req :get "/")]
        (is (= 200 (:status resp)))))
    (testing "create"
      (let [resp (req :post "/")]
        (is (= 201 (:status resp)))))
    (testing "lookup"
      (let [resp (req :get "/felix")]
        (is (= 200 (:status resp)))))
    (testing "delete"
      (let [resp (req :delete "/felix")]
        (is (= 410 (:status resp)))))
    (testing "update"
      (let [resp (req :patch "/felix")]
        (is (= 200 (:status resp)))))
    (testing "replace"
      (let [resp (req :put "/felix")]
        (is (= 200 (:status resp)))))
    (testing "unsupported content type"
      ;; there is no hanlder for text/meow
      (let [args {:method :get
                  :url "http://localhost:5679/landsat/felix"
                  :headers {"Accept" "text/meow"}}
            resp @(http/request args)]
        (is (= 406 (:status resp)))))
    (testing "wild-card media ranges"
      (let [args {:method :get
                  :url "http://localhost:5679/landsat/felix"
                  :headers {"Accept" "text/html;q=0.5, application/json;q=0.9"}}
            resp @(http/request args)]
        (is (= 200 (:status resp)))))
    (testing "exceptions become problems"
      (let [args {:method :get
                  :url "http://localhost:5679/landsat/problem"
                  :headers {"Accept" "application/json"}}
            resp @(http/request args)]
        (is (= 500 (:status resp)))))))

(comment
  (with-system [system]
    (let [args {:method :get
                  :url "http://localhost:5679/problem"
                  :headers {"Accept" "application/json"}}
            resp @(http/request args)]
        resp))
  #_(let [args {:method :get
                :url "http://localhost:5679/landsat/felix"
                :headers {"Accept" "application/xml"}}
          resp @(http/request args)]
     resp))
