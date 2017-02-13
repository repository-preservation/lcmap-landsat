(ns lcmap.aardvark.db
  "Cassandra connections and helper functions.

  Three states are provided: cluster, schema, and session."
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [dire.core :as dire :refer [with-handler!]]
            [lcmap.aardvark.config :refer [config]]
            [mount.core :refer [defstate] :as mount]
            [qbits.alia :as alia]))

(declare db-cluster db-schema db-session)

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

(defn execute-cql [path session]
  (doseq [query (cql->stmts path)]
    (alia/execute session query)))

(defn execute
  "Executes the supplied query."
  [query]
  (log/debugf "Executing query: %s" query)
  (alia/execute db-session query))

;;; States

(defn db-cluster-start
  "Open cluster connection.

  See also `db-session`."
  []
  (let [db-cfg (get-in config [:database :cluster])]
    (log/debugf "starting db with: %s" db-cfg)
    (alia/cluster db-cfg)))

(defn db-cluster-stop
  "Shutdown cluster connection."
  []
  (log/debugf "stopping db")
  (alia/shutdown db-cluster))

(defstate db-cluster
  :start (db-cluster-start)
  :stop  (db-cluster-stop))

;; Potentially destructive, config must have a value set

(defn db-schema-setup
  []
  (if (= true (get-in config [:database :schema :setup]))
         (let [session (alia/connect db-cluster)]
           (execute-cql "schema.setup.cql" session)
           (alia/shutdown session))))

(defn db-schema-teardown
  []
  (if (= true (get-in config [:database :schema :teardown]))
    (let [session (alia/connect db-cluster)]
      (execute-cql "schema.teardown.cql" session)
      (alia/shutdown session))))

(defstate db-schema
  :start (db-schema-setup)
  :stop  (db-schema-teardown))

(defn db-session-start
  "Create session that uses the default keyspace."
  []
  (log/debugf "starting db session")
  (alia/connect db-cluster (get-in config [:database :default-keyspace])))

(defn db-session-stop
  "Close Cassandra session."
  []
  (log/debugf "stopping db session")
  (alia/shutdown db-session))

(defstate db-session
  :start (db-session-start)
  :stop  (db-session-stop))
