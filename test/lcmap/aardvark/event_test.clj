(ns lcmap.aardvark.event-test
  (:require [clojure.test :refer :all]
            [lcmap.aardvark.shared :as shared]
            [lcmap.aardvark.event :as event]))

(deftest testing-event-state
  (shared/with-system
    (testing "connection is open"
      (is (.isOpen event/amqp-connection)))
    (testing "channel is open"
      (is (.isOpen event/amqp-channel)))))
