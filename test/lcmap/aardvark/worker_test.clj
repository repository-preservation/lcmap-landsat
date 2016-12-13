(ns lcmap.aardvark.worker-test
  ""
  (:require [clojure.test :refer :all]
            [lcmap.aardvark.shared :refer :all]))

;; Requiring the worker ns will add state-beings to the
;; test runtime; so each time state is started/stopped,
;; the worker will run as will: exchanges, queues, and
;; consumers executing in the background that are stopped
;; after tests pass (while jobs are running) will produce
;; a variety of strange error messages that have nothing
;; to do with the tests. Because the worker ns is mostly
;; wiring, there isn't a pressing need for automated test.
