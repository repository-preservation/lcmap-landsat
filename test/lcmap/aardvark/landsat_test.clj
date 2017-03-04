(ns lcmap.aardvark.landsat-test
  "Full integration test of server.

  Most of these tests are integration tests; they require a running
  HTTP server backed by Cassandra, RabbitMQ, and Elasticserch. The
  backing services can be started with `make docker-dev-up`.

  Fixtures are used to selectively start/stop mount beings and load
  seed data into the test system.
  "
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all]
            [lcmap.aardvark.shared :refer [req]]
            [lcmap.aardvark.server :as server]
            [lcmap.aardvark.fixtures :as fixtures]))

(use-fixtures :once fixtures/with-services fixtures/with-server)

(use-fixtures :each fixtures/with-data)

(deftest default-resource-tests
  (testing "entry-point"
    (let [resp (req :get "http://localhost:5679")]
      (is (= 200 (:status resp))))))

(deftest health-resource-tests
  (testing "health check"
    (let [resp (req :get "http://localhost:5679/health"
                    :headers {"Accept" "*/*"})]
      (is (= 200 (:status resp))))))

(deftest tile-resource-tests
  #_(testing "parameters validation"
    (let [resp (req :get "http://localhost:5679/tiles")]
      ()))
  (testing "get tiles as JSON"
    (let [resp (req :get "http://localhost:5679/tiles"
                    :headers {"Accept" "application/json"}
                    :form-params {:x 0 :y 0
                                  :ubid ["LANDSAT_5/TM/sr_band1"
                                         "LANDSAT_5/TM/sr_band2"]
                                  :acquired "2000-01-01/2005-01-01"})]
      (is (= 200 (:status resp)))))
  (testing "get tiles as an unsupported type"
    (let [resp (req :get "http://localhost:5679/tiles"
                    :headers {"Accept" "application/foo"}
                    :form-params {:x 0 :y 0
                                  :ubid "LANDSAT_5/TM/sr_band1"
                                  :acquired "2000-01-01/2005-01-01"})]
      (is (= 200 (:status resp)))
      (is (= "application/json" (get-in resp [:headers :content-type])))))
  (testing "get a single ubid as JSON"
    (let [resp (req :get "http://localhost:5679/tile/LANDSAT_5/TM/sr_band1"
                    :headers {"Accept" "application/json"}
                    :form-params {:x 0 :y 0
                                  :acquired "2000-01-01/2005-01-01"})]
      (is (= 200 (:status resp))))))

(deftest tile-spec-resource-tests
  (testing "get an existing tile-spec"
    (let [resp (req :get "http://localhost:5679/tile-spec/LANDSAT_5/TM/sr_band1"
                    :headers {"Accept" "application/json"})]
      (is (= 200 (:status resp)))))
  (testing "get a non-existent tile-spec"
    (let [resp (req :get "http://localhost:5679/tile-spec/LANDSAT_5/TM/marklar"
                    :headers {"Accept" "application/json"})]
      (is (= 404 (:status resp))))))

(deftest source-tests
  (let [landsat-source {:id  "LT50460272000005"
                        :uri (-> "ESPA/CONUS/ARD/LT50460272000005-SC20160826121722.tar.gz" io/resource io/as-url str)
                        :checksum "9aa16eac2b9b8a20301ad091ceb9f3f4"}]
    (testing "putting a valid source"
      (let [resp (req :put "http://localhost:5679/source/LT50460272000005"
                      :form-params landsat-source)]
        (is (= 202 (:status resp)))))
    (testing "getting a source that should exist"
      ;; the first PUT request is necessary, it creates the source
      ;; that is subsequenty retrieved.
      (let [_    (req :put "http://localhost:5679/source/LT50460272000005"
                      :form-params landsat-source)
            resp (req :get "http://localhost:5679/source/LT50460272000005")]
        (is (= 200 (:status resp)))))
    (testing "getting a source that should not exist"
      (let [resp (req :get "http://localhost:5679/source/not-found")]
        (is (= 404 (:status resp))))))
    (testing "putting an invalid source"
      (let [invalid-source {:id "invalid-source"
                            :checksum "d2769e6390074dd52d88e82475a74d79"}
            resp (req :put "http://localhost:5679/source/invalid-source"
                      :form-params invalid-source)]
        (is (= 403 (:status resp))))))
