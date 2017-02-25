(ns lcmap.aardvark.worker-test
  "The worker ns"
  (:require [clojure.test :refer :all]
            [lcmap.aardvark.shared :as shared]
            [lcmap.aardvark.config :as config]
            [lcmap.aardvark.event :as event]
            [lcmap.aardvark.worker :as worker]
            [langohr.queue :as lq]))

;; The worker namespace defines system stated-related
;; behavior. It does not contain any sort of business
;; logic, so few tests are defined. The code itself is
;; run as part of starting/stopping a system, so it
;; is technically covered.

(deftest testing-worker
  (testing "the worker queue has a consumer"
    (shared/with-system
      (let [ch event/amqp-channel
            queue-name (get-in config/config [:worker :queue])]
        (= 1 (lq/consumer-count ch queue-name))))))
