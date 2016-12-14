(ns lcmap.aardvark.db-test
  (:require [clojure.test :refer :all]
            [lcmap.aardvark.shared :as shared]
            [lcmap.aardvark.db :as db]))

(deftest testing-event-state
  (shared/with-system
    (testing "cluster connection is open"
      (is (not (.isClosed db/db-cluster))))
    (testing "session is open"
      (is (not (.isClosed db/db-session))))))
