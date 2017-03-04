(ns lcmap.aardvark.db-test
  (:require [clojure.test :refer :all]
            [lcmap.aardvark.fixtures :as fixtures]
            [lcmap.aardvark.db :as db]))

(use-fixtures :once fixtures/with-services)

(deftest testing-db-state
  (testing "cluster connection is open"
    (is (not (.isClosed db/db-cluster))))
  (testing "session is open"
    (is (not (.isClosed db/db-session)))))
