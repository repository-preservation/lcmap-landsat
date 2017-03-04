(ns lcmap.aardvark.event-test
  (:require [clojure.test :refer :all]
            [lcmap.aardvark.fixtures :as fixtures]
            [lcmap.aardvark.event :as event]))

(use-fixtures :once fixtures/with-services)

(deftest testing-event-state
  (testing "connection is open"
    (is (.isOpen event/amqp-connection)))
  (testing "channel is open"
    (is (.isOpen event/amqp-channel))))
