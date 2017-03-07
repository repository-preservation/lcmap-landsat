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

(defn db-cluster-start
  "Open cluster connection.

  See also `db-session`."
  []
  (let [db-cfg (get-in config [:database :cluster])]
    (log/debugf "starting db cluster (connection) with: %s" db-cfg)
    (alia/cluster db-cfg)))

(defn db-cluster-stop
  "Shutdown cluster connection."
  []
  (log/debugf "stopping db cluster (connection)")
  (alia/shutdown db-cluster))

;; After start this refers to com.datastax.driver.core.Cluster, an
;; object that maintains general information about the cluster; use
;; db-session to execute queries.

(defstate db-cluster
  :start (db-cluster-start)
  :stop  (db-cluster-stop))

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

;; After start this will refer to a com.datastax.driver.core.SessionManager
;; object that can be used to execute queries.
;;
;; WARNING: Do not use the same session for multiple keyspaces, functions
;; that rely on this state expect a stable keyspace name!
;;
(defstate db-session
  :start (db-session-start)
  :stop  (db-session-stop))

(defn execute
  "Executes the supplied query."
  [query]
  (log/debugf "executing query: %s" query)
  (alia/execute db-session query))
