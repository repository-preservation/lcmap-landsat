(ns lcmap.aardvark.elasticsearch-test
  ""
  (:require [clojure.test :refer :all]
            [lcmap.aardvark.fixtures :as fixtures]
            [lcmap.aardvark.elasticsearch :as es]))

;; This value is not derived from a configuration because these tests
;; are used to test fictitious data. Other namespaces that utilize
;; Elasticsearch should provide project-specific data where appropriate.

(def test-index "http://localhost:9200/elastic-test")
(def animal-type "http://localhost:9200/elastic-test/animals")

;; Fixture setup/teardown. The index should be removed between tests,
;; some data is pre-loaded so that tests can evalaute the difference
;; indexing already existing and new data.

(defn load-data []
  (es/doc-create animal-type "Lazarus" {:name "Lazarus"
                                        :type "cat"
                                        :colors ["grey"]})
  (es/doc-create animal-type "Larry"   {:name "Larry"
                                        :type "fish"
                                        :colors ["gold"]})
  (es/doc-create animal-type "Curly"   {:name "Curly"
                                        :type "fish"
                                        :colors ["blue"]})
  (es/doc-create animal-type "Moe"     {:name "Moe"
                                        :type "fish"
                                        :colors ["yellow" "white"]})
  (es/doc-create animal-type "Mya"     {:name "Mya"
                                        :type "dog"
                                        :colors ["black" "brown"]})
  (es/index-refresh test-index))

(defn drop-data []
  (es/index-delete test-index))

(defn with-elasticsearch [test-fn]
  (load-data)
  (test-fn)
  (drop-data))

(use-fixtures :each with-elasticsearch)

(deftest doc-index-tests
  (testing "index an existing document will succeed"
    (let [id "Mya"
          doc {:name "Mya" :type "dog" :colors ["white"]}
          result (es/doc-index animal-type id doc)]
      (is (= :updated result))))
  (testing "index a non-existent document"
    (let [id "Frank"
          doc {:name "Frank" :type "fish" :tags ["red" "blue" "green"]}
          result (es/doc-index animal-type id doc)]
      (is (= :created result)))))

(deftest doc-create-tests
  (testing "create an existing document will fail"
    (let [id "Mya"
          doc {:name "Mya" :tags ["brown"]}
          result (es/doc-create animal-type id doc)]
      (is (= nil result)))))

(deftest doc-get-tests
  (testing "get an existing document"
    (let [animal (es/doc-get animal-type "Mya")]
      (is (= "Mya" (:name animal)))))
  (testing "get a non-existent document"
    (let [animal (es/doc-get animal-type "Frank")]
      (is (nil? animal)))))

(deftest doc-delete-tests
  (testing "delete an existing document"
    (let [id "Mya"
          result (es/doc-delete animal-type id)]
      (is (= true result))))
  (testing "delete a non-existent document"
    (let [id "Frank"
          result (es/doc-delete animal-type id)]
      (is (= false result)))))

(deftest search-tests
  (testing "searching index+type by tags"
    (let [hits (es/search animal-type "type:fish")]
      (is (= 3 (get-in hits [:hits :total])))))
  (testing "searching index+type by name"
    (let [hits (es/search animal-type "type:dog")]
      (is (= 1 (get-in hits [:hits :total])))))
  (testing "searching with limits"
    (let [hits (es/search animal-type "type:fish" {:size 1})]
      (is (= 1 (count (get-in hits [:hits :hits])))))
    (let [hits (es/search animal-type "type:fish" {:size 2})]
      (is (= 2 (count (get-in hits [:hits :hits])))))
    (let [hits (es/search animal-type "type:fish" {:size 3})]
      (is (= 3 (count (get-in hits [:hits :hits])))))))

(deftest doc-multi-get-tests
  (testing "getting multiple animals"
    (let [animals (es/doc-multi-get animal-type ["Mya" "Lazarus"])]
      (is (= 2 (count animals)))))
  (testing "getting multiple animals, some of which are missing"
    (let [animals (es/doc-multi-get animal-type ["Mya" "Frank"])]
      (is (= 2 (count animals))))))

(deftest doc-bulk-tests
  (testing "putting multiple animals"
    (let [requests [{:index {:_id "ant"}}
                    {:name "ant"}
                    {:index {:_id "cow"}}
                    {:name "cow"}]
          result (es/doc-bulk animal-type requests)]
      (is (= 2 (count (result :items))))
      (is (= false (result :errors))))))

(deftest doc-bulk-index-tests
  (testing "putting multiple animals into an index"
    ;; This uses the index URL, it does not specify the type
    ;; so options must specify explicitly.
    (let [animals [{:name "Larry" :tags ["fish"] :color ["red"]}
                   {:name "Curly" :tags ["fish"] :color ["gold"]}
                   {:name "Moe"   :tags ["fish"] :color ["blue"]}]
          options {:_type "animal" :_id :name}
          results (es/doc-bulk-index test-index animals options)]
      (is (= false (results :errors)))))
  ;; This uses an index URL with a type, the options do not
  ;; need an explicity type.
  (testing "putting multiple animals into an index-type"
    (let [animals [{:name "Bessie" :type "cow"}
                   {:name "Chance" :type "horse"}]
          options {:_id :name}
          results (es/doc-bulk-index animal-type animals options)]
      (is (= false (results :errors))))))

(deftest cat-tests
  (testing "health check"
    (let [health (es/cat-health "http://localhost:9200")]
      (is (-> health first :status)))
    (let [doc-count (es/cat-count "http://localhost:9200")]
      (is (-> doc-count first :count)))
    (let [indices (es/cat-indices "http://localhost:9200" {:health "yellow"})]
      (is (< 0 (count indices))))))
