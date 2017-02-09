(ns lcmap.aardvark.source
  "Resource and functions related to curating Landsat scenes."
  (:require [clojure.tools.logging :as log]
            [compojure.core :refer :all]
            [cheshire.core :as json]
            [clj-time.core :as time]
            [ring.util.accept :refer [accept]]
            [lcmap.aardvark.config :refer [config]]
            [lcmap.aardvark.db :refer [db-session]]
            [lcmap.aardvark.event :refer [amqp-channel]]
            [lcmap.aardvark.middleware :refer [wrap-handler]]
            [mount.core :as mount :refer [defstate]]
            [qbits.alia :as alia]
            [qbits.hayt :as hayt]
            [qbits.hayt.codec.joda-time]
            [langohr.exchange :as le]
            [langohr.basic :as lb]
            [schema.core :as schema]))

(def source-schema
  {:id schema/Str
   :uri schema/Str
   :checksum schema/Str
   (schema/optional-key :progress_at) schema/Any
   (schema/optional-key :progress_name) schema/Str
   (schema/optional-key :progress_desc) schema/Str})

(defn search
  "Query DB for source."
  [source-id]
  (log/debugf "search for source: %s" source-id)
  (->> (hayt/where {:id source-id})
       (hayt/select :sources)
       (alia/execute db-session)
       (seq)))

(defn sample
  ""
  [n]
  (->> (hayt/select :sources
                    (hayt/columns (hayt/distinct* :id))
                    (hayt/limit n))
       (alia/execute db-session)
       (seq)))

(defn validate
  "Produce a map of errors if the job is invalid, otherwise nil."
  [source]
  (log/debugf "validate source %s" source)
  (schema/check source-schema source))

(defn insert
  "Create a source in the DB."
  [source]
  (log/debugf "inserting source: %s" source)
  (alia/execute db-session (hayt/insert :sources (hayt/values source)))
  source)

(defn publish
  "Add source to queue for processing."
  [source]
  (let [exchange (get-in config [:server :exchange])
        routing "ingest"
        payload (json/encode source)]
    (log/debugf "publish '%s' source: %s" routing payload)
    (lb/publish amqp-channel exchange routing payload
                {:content-type "application/json" :persistent true}))
  source)

(defn save
  "Create a source and publish a message."
  [source]
  (let [source+ (merge source {:progress_at (time/now)
                               :progress_name "queue"})]
    (io!
     (insert source+)
     (publish source+))))

(defn progress
  "Insert source row with state information."
  ([source name desc]
   (let [source+ (merge source {:progress_at (time/now)
                                :progress_name name
                                :progress_desc desc})]
     (log/infof "%s %s: %s" (source :id) name desc)
     (insert source+)))
  ([source name]
   (progress source name nil)))
