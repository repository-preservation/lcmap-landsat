(ns lcmap.aardvark.db
  ""
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [dire.core :as dire :refer [with-handler!]]
            [lcmap.aardvark.config :refer [config]]
            [mount.core :refer [defstate] :as mount]
            [qbits.alia :as alia]))


;;; Cassandra schema management utilities

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

(defn execute-cql [path db]
  (let [setup-session (alia/connect db)]
    (doseq [query (cql->stmts path)]
      (do (log/debugf "executing CQL: %s" query)
          (alia/execute setup-session query)))
    (alia/shutdown setup-session)))

(dire/with-handler! #'execute-cql
  [java.io.FileNotFoundException
   java.lang.IllegalArgumentException]
  (fn [e & [source]]
    (log/warnf "Optional CQL file '%s' does not exist." source)
    ;; ...results in an empty collection of statements
    []))

;;; DB-related state

(defstate db-conn
  :start (let [db-cfg (:database config)]
           (log/debugf "starting db with: %s" db-cfg)
           (apply alia/cluster (select-keys db-cfg [:contact-points])))
  :stop  (do
           (log/debugf "stopping db")
           (alia/shutdown db-conn)))

(defstate db-schema
  :start (execute-cql "schema.setup.cql" db-conn)
  :stop  (execute-cql "schema.teardown.cql" db-conn))

(defstate db-session
  :start (do
           (log/debugf "starting db session")
           (alia/connect db-conn (get-in config [:database :default-keyspace])))
  :stop  (do
           (log/debugf "stopping db session")
           (alia/shutdown db-session)))
