(ns lcmap.aardvark.db
  "Cassandra connections and helper functions.

  This namespace provide states for the DB cluster, session,
  and schema setup and teardown. States refer to a `:database`
  key/value of `lcmap.aardvark.config/config` for connection
  information."
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [dire.core :as dire :refer [with-handler!]]
            [lcmap.aardvark.config :refer [config]]
            [mount.core :refer [defstate] :as mount]
            [qbits.alia :as alia]))

;; Declare vars so that functions can be defined that refer to them
;; in a more clear way.
(declare db-cluster db-schema db-session)

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
  "Execute CQL statement contained in file at path.

  This funciton establishes its own session with the database so
  that any changes to session state, like switching keyspaces,
  do not affect other functions that make use of the session."
  [path db]
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

(defn db-cluster-start
  "Open cluster connection.

  This connection maintains information about the topology of the
  cluster.

  See also `db-session`."
  []
  (let [db-cfg (:database config)]
    (log/debugf "starting db with: %s" db-cfg)
    (apply alia/cluster (select-keys db-cfg [:contact-points :credentials]))))

(defn db-cluster-stop
  "Shutdown cluster connection."
  []
  (log/debugf "stopping db")
  (alia/shutdown db-cluster))

(defstate db-cluster
  :start (db-cluster-start)
  :stop  (db-cluster-stop))

(defstate db-schema
  :start (execute-cql "schema.setup.cql" db-cluster)
  :stop  (execute-cql "schema.teardown.cql" db-cluster))

(defn db-session-start
  "Create Cassandra session.

  The db-session is used to execute queries. The object refered to
  by this state maintains multiple connections to cluster nodes.
  However, a session can only operate within the context of a single
  keyspace at a time.

  PLEASE NOTE: The intent is that an application only operate on
  tables in its own keyspace (e.g., lcmap_landsat_dev). Switching
  keyspaces to perform arbitrary operations could cause strange
  behavior. If multiple keyspace are used, then a separate session
  should be created for each one."
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

;; TODO - Add Dire error handling.
(defn execute
  "Executes the supplied query."
  [query]
  (log/debugf "Executing query:%s" query)
  (alia/execute db-session query))
