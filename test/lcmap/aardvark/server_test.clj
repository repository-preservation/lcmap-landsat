(ns lcmap.aardvark.server-test
  "The server namespace is light on functionality; it provides
  functions that combine middleware, encoders, and handlers;
  as well as a mount state.
  "
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all]
            [lcmap.aardvark.server :as server]
            [lcmap.aardvark.shared :refer :all]
            [lcmap.aardvark.fixtures :as fixtures]))

(use-fixtures :once fixtures/with-server)

(deftest testing-server-state
  (testing "server is started"
    (is (.isStarted server/server))))
