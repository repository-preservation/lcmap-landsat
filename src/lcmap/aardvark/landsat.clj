(ns lcmap.aardvark.landsat
  "Resources and representations.

  This namespace contains all route definitions and functions
  transforming/validating requests. Other namespaces handle
  implementation details related to persistence and messaging."
  (:require [camel-snake-kebab.core :refer [->snake_case_keyword]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [clojure.string :as str]
            [compojure.core :refer :all]
            [ring.util.accept :refer [accept]]
            [lcmap.aardvark.health :as health]
            [lcmap.aardvark.html :as html]
            [lcmap.aardvark.source :as source]
            [lcmap.aardvark.chip :as chip]
            [lcmap.aardvark.chip-spec :as chip-spec]
            [lcmap.commons.collections :refer [vectorize]]
            [lcmap.commons.numbers :refer [numberize]]
            [lcmap.aardvark.middleware :refer [wrap-handler]]))

;;; Response producing functions

(defn allow
  "Indicate allowed verbs."
  [& verbs]
  (log/debugf "explaining allow verbs")
  {:status 405
   :headers {"Allow" (str/join "," verbs)}})

(defn check-health
  "Indicate status of backing services."
  []
  (log/debugf "checking app health")
  (let [service-status (health/health-status)]
    (if (every? (fn [[_ {is :healthy}]] is) service-status)
      {:status 200 :body service-status}
      {:status 503 :body service-status})))

(defn get-source
  "Search for source by ID."
  [source-id]
  (log/debugf "get source: %s" source-id)
  (if-let [result (seq (source/search source-id))]
    {:status 200 :body result}
    {:status 404 :body []}))

(defn put-source
  "Handle request for creating a source and produce a response."
  [source-id {params :params :as req}]
  (log/debugf "find or create source '%s' with %s" source-id params)
  (let [source (merge {:id source-id :progress_name "created"} params)]
    (or (some->> (source/validate source)
                 (assoc {:status 403} :body))
        (some->> (source/search source-id)
                 (assoc {:status 409} :body))
        (some->> (source/save source)
                 (assoc {:status 202} :body)))))

(defn get-chips
  "Get chips containing point for given UBID and ISO8601 time range."
  [{{:keys [:ubid :x :y :acquired] :as params} :params :as req}]
  (let [query (chip/conform params)]
    (or (some->> (chip/validate query)
                 (seq)
                 (assoc {:status 403} :body))
        (some->> (chip/find query)
                 (assoc {:status 200} :body))
        {:status 404 :body []})))

(defn get-chip
  "Gets a chip given x, y, acquired, satellite, sensor and band"
  [satellite sensor band request]
  (->> (str/join "/" [satellite sensor band])
       (update-in request [:params :ubid] str)
       (get-chips)))

(defn get-chip-spec
  "Search for a source and produce a response map."
  [ubid {params :params :as req}]
  (log/debugf "get chip-spec for '%s' with %s" ubid params)
  (let [result (chip-spec/get ubid)]
    (if (= (:ubid result) ubid)
      {:status 200 :body result}
      {:status 404 :body nil})))

(defn get-chip-specs
  "Get chip-specs."
  [{{q :q :or {q "*"}} :params}]
  (log/debug "get chip-specs")
  {:status 200 :body (chip-spec/search q)})

;;; Request entity transformers.

(defn decode-json
  "Transform request entity from JSON into a data structure."
  [body]
  (log/debug "decode JSON request body")
  (->> body
       (slurp)
       (json/decode)
       (transform-keys ->snake_case_keyword)))

(defn prepare-with
  "Transform request entity into data structure."
  [request]
  (log/debugf "decode %s request body" (get-in request [:headers "content-type"]))
  (if (= "application/json" (get-in request [:headers "content-type"]))
    (update request :body decode-json)
    request))

;;; Response entity transformers.

(defn to-html
  "Transform response body into HTML."
  [response]
  (log/debug "responding with HTML")
  (let [template-fn (:template (meta response) html/default)]
    (update response :body template-fn)))

(defn to-json
  "Transform response body into JSON."
  [response]
  (log/debug "responding with json")
  (-> response
      (update :body json/encode)
      (assoc-in [:headers "Content-Type"] "application/json")))

;; This is a list of supported types. By default a response will be
;; produced as JSON, even if no accept media-ranges are provided.
;; Even though this isn't technically correct, it's good-enough for
;; the time being. It might be better to handle and empty accept header
;; differently than an accept header that only request unsupported
;; content-types.
;;
;; This uses a utility function provided by ring-accept instead
;; of a macro so that debugging is somewhat easier.
(def supported-types (accept :default to-json
                             "application/json" to-json
                             "text/html" to-html))

(defn respond-with
  "Transform a data structure into a suitable representation."
  [request response]
  (supported-types request response))

;;; Routes

(defn resource
  "Handlers for landsat resource."
  []
  (wrap-handler
   (context "/" request
     (GET "/" []
       (with-meta {:status 200}
         {:template html/default}))
     (ANY "/" []
       (with-meta (allow ["GET"])
         {:template html/default}))
     (GET "/health" []
       (with-meta (check-health)
         {:template html/status-list}))
     (GET  "/source/:source-id{.+}" [source-id]
       (with-meta (get-source source-id)
         {:template html/source-info}))
     (PUT "/source/:source-id{.+}" [source-id]
       (with-meta (put-source source-id request)
         {:template html/source-info}))
     (ANY "/source" []
       (with-meta (allow ["GET" "PUT"])
         {:template html/default}))
     (GET "/chips" []
       (with-meta (get-chips request)
         {:template html/chip-list}))
     (GET "/chip/:satellite/:sensor/:band" [satellite sensor band]
       (with-meta (get-chip satellite sensor band request)
         {:template html/chip-info}))
     (ANY "/chip" []
       (with-meta (allow ["GET"])
         {:template html/default}))
     (GET "/chip-specs" []
       (with-meta (get-chip-specs request)
         {:template html/chip-spec-list}))
     (ANY "/chip-specs" []
       (with-meta (allow ["GET"])
         {:template html/default}))
     (GET "/chip-spec/:ubid{.+}" [ubid]
       (with-meta (get-chip-spec ubid request)
         {:template html/chip-spec-info}))
     (ANY "/chip-spec/:ubid{.+}" []
       (with-meta (allow ["GET"])
         {:template html/default})))
   prepare-with respond-with))
