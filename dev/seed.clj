(ns seed
  "Provide recipes for seeding Cassandra (and Elasticsearch) with real-world
   congruent data."
  (:require [lcmap.aardvark.tile-spec :as tile-spec]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.pprint :refer [pprint]]))

(comment
  "This will create tile-specs from real-world data that match what
   is seen production systems. The test data was created when the
   input format was not completely decided. Because this uses real
   data, it can take some time to download."
  (let [L8 (edn/read-string (slurp "data/tile-specs/L8.edn"))
        L7 (edn/read-string (slurp "data/tile-specs/L7.edn"))
        L5 (edn/read-string (slurp "data/tile-specs/L5.edn"))
        L4 (edn/read-string (slurp "data/tile-specs/L4.edn"))]
    (map tile-spec/insert (concat L8 L7 L5 L4))))
