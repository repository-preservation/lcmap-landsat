(ns lcmap.aardvark.server-test
  "Full integration test of server and worker."
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all]
            [clojure.tools.logging :as log]
            [lcmap.aardvark.server :as server]
            [lcmap.aardvark.shared :refer :all]
            [lcmap.aardvark.tile-spec :as tile-spec :as tile-spec]
            [lcmap.aardvark.tile-spec-test :refer [L5 spec-opts]]))

(deftest landsat-tests
  (with-system
    (testing "entry-point"
      (let [resp (req :get "http://localhost:5679/landsat")]
        (is (= 200 (:status resp)))))
    (testing "unsupported verbs"
      (let [resp (req :put "http://localhost:5679/landsat")]
        (is (= 405 (:status resp))))
      (let [resp (req :delete "http://localhost:5679/landsat")]
        (is (= 405 (:status resp)))))))

(deftest landsat-health-resource
  (with-system
    (testing "health check"
      (let [resp (req :get "http://localhost:5679/landsat/health"
                            :headers {"Accept" "*/*"})]
        (is (= 200 (:status resp)))))))

(deftest landsat-tile-resource
  (with-system
    (testing "get tiles as JSON"
      (let [resp (req :get "http://localhost:5679/landsat/tiles"
                      :headers {"Accept" "application/json"}
                      :form-params {:x 0 :y 0
                                    :ubid ["LANDSAT_5/TM/sr_band1"
                                           "LANDSAT_5/TM/sr_band2"]
                                    :acquired "2000-01-01/2005-01-01"})]
        (is (= 200 (:status resp)))))
    (testing "get tiles as an unsupported type"
      (let [resp (req :get "http://localhost:5679/landsat/tiles"
                      :headers {"Accept" "application/foo"}
                      :form-params {:x 0 :y 0
                                    :ubid "LANDSAT_5/TM/sr_band1"
                                    :acquired "2000-01-01/2005-01-01"})]
        (is (= 200 (:status resp)))
        (is (= "application/json" (get-in resp [:headers :content-type])))))))

(deftest landsat-tile-spec-resource
  (with-system
    #_(testing "put a tile-spec as JSON"
       (let [tile-spec (slurp (io/resource "data/sample-tile-spec.json"))
             resp (req :put "http://localhost:5679/landsat/tile-spec/LANDSAT_5/TM/sr_band1"
                       :headers {"Content-Type" "application/json"
                                 "Accept" "application/json"}
                       :body tile-spec)]
         (is (= 202 (:status resp)))))
    (testing "get an existing tile-spec"
      (tile-spec/process L5 spec-opts)
      (let [resp (req :get "http://localhost:5679/landsat/tile-spec/LANDSAT_5/TM/sr_band1"
                      :headers {"Accept" "application/json"})]
        (is (= 200 (:status resp)))))
    (testing "get a non-existent tile-spec"
      (let [resp (req :get "http://localhost:5679/landsat/tile-spec/LANDSAT_5/TM/marklar"
                      :headers {"Accept" "application/json"})]
        (is (= 404 (:status resp)))))
    #_(testing "post multiple tile-specs"
       (let [tile-specs (slurp (io/resource "data/sample-tile-specs.json"))
             resp (req :post "http://localhost:5679/landsat/tile-spec"
                       :headers {"Content-Type" "application/json"
                                 "Accept" "application/json"}
                       :body tile-specs)]
            (is (= 200 (:status resp)))))))

(deftest source-tests
  (with-system
    (let [landsat-source {:id  "LT50460272000005"
                          :uri (-> "ESPA/CONUS/ARD/LT50460272000005-SC20160826121722.tar.gz" io/resource io/as-url str)
                          :checksum "9aa16eac2b9b8a20301ad091ceb9f3f4"}
          invalid-source {:id "invalid-source"
                          :checksum "d2769e6390074dd52d88e82475a74d79"}]
      (testing "putting an invalid source"
        (let [resp (req :put "http://localhost:5679/landsat/source/invalid-source"
                        :form-params invalid-source)]
          (is (= 403 (:status resp)))))
      (testing "putting a valid source"
        (let [resp (req :put "http://localhost:5679/landsat/source/LT50460272000005"
                        :form-params landsat-source)]
          (is (= 202 (:status resp)))))
      (testing "getting a source that should exist"
        ;; the first PUT request is necessary, it creates the source
        ;; that is subsequenty retrieved.
        (let [_    (req :put "http://localhost:5679/landsat/source/LT50460272000005"
                        :form-params landsat-source)
              resp (req :get "http://localhost:5679/landsat/source/LT50460272000005")]
          (is (= 200 (:status resp)))))
      (testing "getting a source that should not exist"
        (let [resp (req :get "http://localhost:5679/landsat/source/not-found")]
          (is (= 404 (:status resp))))))))
