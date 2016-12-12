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
   (schema/optional-key :state_at) schema/Any
   (schema/optional-key :state_name) schema/Str
   (schema/optional-key :state_desc) schema/Str})

(defn- search
  "Query DB for source."
  [source-id]
  (log/debugf "search for source: %s" source-id)
  (->> (hayt/where {:id source-id})
       (hayt/select :sources)
       (alia/execute db-session)
       (seq)))

(defn lookup
  "Search for a source and produce a response map."
  [source-id]
  (log/debugf "lookup source: %s" source-id)
  (if-let [result (search source-id)]
    {:status 200 :body result}
    {:status 404 :body []}))

(defn- validate
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
  (let [exchange (get-in config [:event :server-exchange])
        routing "ingest"
        payload (json/encode source)]
    (log/debugf "publish '%s' source: %s" routing payload)
    (lb/publish amqp-channel exchange routing payload
                {:content-type "application/json"}))
  source)

(defn insert-and-publish
  "Create a source and publish a message."
  [source]
  (let [source+ (merge source {:state_at (time/now)
                               :state_name "queue"})]
    (io!
     (insert source+)
     (publish source+))))

(defn activity
  "Insert source row with state information."
  ([source name desc]
   (let [source+ (merge source {:state_at (time/now)
                                :state_name name
                                :state_desc desc})]
     (insert source+)))
  ([source name]
   (activity source name nil)))

(defn persist
  "Handle request for creating a source and produce a response."
  [source-id {params :params :as req}]
  (let [source (merge {:id source-id :state_name "created"} params)]
    (or (some->> (validate source)
                 (assoc {:status 403} :body))
        (some->> (search source-id)
                 (assoc {:status 409} :body))
        (some->> (insert-and-publish source)
                 (assoc {:status 202} :body)))))

;;; Request-Response Middleware

(defn to-json
  "Encode response body as JSON"
  [response]
  (log/debug "responding with JSON")
  (update response :body json/encode))

(def supported-types
  (accept "application/json" to-json
          "*/*" to-json))

;;; Routing

(defn resource
  "Subordinate Landsat resource for managing source data."
  []
  (context "/landsat/source" request
    (-> (routes
         (GET "/:source-id" [source-id]
              (lookup source-id))
         (PUT "/:source-id" [source-id]
              (persist source-id request)))
        (wrap-handler identity supported-types))))
