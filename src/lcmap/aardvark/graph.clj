(ns lcmap.aardvark.graph
  (:require [lcmap.aardvark.state :refer [db-session]]
            [qbits.alia :as alia]
            [clojure.tools.logging :as log]
            [clojure.string :as str]))

(defn tile-specs []
  "Retrieves tile-specs"
  (let [query (str "select ubid from lcmap.tile_specs")
        specs (try
                (alia/execute db-session query)
                (catch Throwable t
                  (log/error
                   (apply str (interpose "\n" (.getStackTrace t))))))]
    specs))

(defn spec-tokens [spec-loader]
  "Yields a lazy sequence of tokens from tile spec loader function"
  (map #(str/split % #"/")
       (map :ubid (spec-loader))))

(defn populate []
  "Loads and returns a tile spec graph"
  (spec-tokens #(tile-specs)))
