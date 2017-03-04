(ns lcmap.aardvark.setup.database
  "Provides functions for configuring database and event components."
  (:require [qbits.alia :as alia]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [dire.core :as dire :refer [with-handler!]]
            [lcmap.aardvark.config :refer [config]]
            [lcmap.aardvark.db :as db]
            [lcmap.aardvark.util :as util]
            [mount.core :as mount :refer [defstate]]
            [qbits.alia :as alia]))

;; Database setup functions.

;; utility functions

(defn cql->stmts
  "Convert file containing CQL statements into a vector.

  Splits statements, removes comments, trims whitespace,
  and removes blank statements."
  [path]
  (let [cql (-> path
                (io/resource)
                (slurp)
                (clojure.string/split #";"))
        tf (comp (map #(clojure.string/replace % #"--.+\n" ""))
                 (map clojure.string/trim)
                 (remove clojure.string/blank?))]
    (into [] tf cql)))

(defn execute-cql
  "Executes a CQL string, useful for schema setup or teardown.

  This does not use the lcmap.aardvark.db-session because the CQL
  may refer to a keyspace that does not yet exist, as is the case
  when bootstrapping a fresh development environment or automated
  test build."
  ([path cluster]
   (let [session (alia/connect cluster)]
     (doseq [query (cql->stmts path)]
       (alia/execute session query))
     (alia/shutdown session)))
  ([path]
   (execute-cql path db/db-cluster)))

;; Aliases make reading code that executes CQL just a little
;; more clear about the intention.
;;
(def setup execute-cql)
(def teardown execute-cql)
