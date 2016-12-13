(ns lcmap.aardvark.fixtures
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [lcmap.aardvark.tile-spec :as tile-spec]
            [mount.core :refer [defstate]]))

(defstate tile-spec-data
  :start (let [ts (read-string (slurp (io/resource "tile-specs.edn")))]
           (log/debugf "loading fixture data")
           (doall (map tile-spec/insert ts))))
