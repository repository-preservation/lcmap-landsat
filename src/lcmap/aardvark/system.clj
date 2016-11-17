(ns lcmap.aardvark.system
  "Define components and function for building a system"
  (:require [com.stuartsierra.component :as component]
            [ring.component.jetty :refer [jetty-server]]
            [clojure.tools.logging :as log]
            [qbits.alia :as alia]
            [langohr.core :as rmq]
            [lcmap.aardvark.app :as app]))

;;;

(defrecord Database [config]
  component/Lifecycle
  (start [component]
    (log/info "starting db component ...")
    (->> (:database config)
         (apply alia/cluster)
         (assoc component :cluster)))
  (stop [component]
    (log/info "stopping db component ...")
    (alia/shutdown (:cluster component))
    (dissoc component :cluster)))

(defn new-database [config]
  (map->Database {:config config}))


(defrecord DatabaseSession [database]
  component/Lifecycle
  (start [component]
    (log/info "starting db session component ...")
    (assoc component :db-session (alia/connect (:db component))))
  (stop [component]
    (log/info "stopping db session component ...")
    (alia/shutdown (:db-session component))
    (dissoc component :db-session)))

(defn new-database-session [database]
  (map->DatabaseSession {:db database}))

;;;

(defrecord Event [config]
  component/Lifecycle
  (start [component]
    (log/info "starting event component ...")
    (->> (:event config)
         (rmq/connect)
         (assoc component :connection)))
  (stop [component]
    (log/info "stopping event component ...")
    (rmq/close (:connection component))
    (dissoc component :connection)))

(defn new-event [config]
  (->Event config))

;;;

(defrecord App [config db msg]
  component/Lifecycle
  (start [component]
    (log/info "starting app component ...")
    (assoc component :handler (app/new-handler db msg)))
  (stop [component]
    (log/info "stopping app component ...")
    (dissoc component :handler)))

(defn new-app [config]
  (map->App {:config config :db nil :msg nil}))

;;;

(defrecord Logging [config]
  component/Lifecycle
  (start [component]
    component)
  (stop [component]
    component))

(defn new-logger [config]
  (map->Logging {:config config}))

;;;

(defn system [cfg]
  (component/system-map
   :log  (new-logger cfg)
   :db   (new-database cfg)
   :msg  (new-event cfg)
   :app  (component/using (new-app cfg) [:db :msg])
   :http (component/using (jetty-server (:http cfg)) [:app])))
