(ns lcmap.aardvark.landsat
  "Resources and representations."
  (:require [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [compojure.core :refer :all]
            [ring.util.accept :refer [accept]]
            [lcmap.aardvark.source :as source]
            [lcmap.aardvark.tile :as tile]
            [lcmap.aardvark.tile-spec :as tile-spec]
            [lcmap.aardvark.middleware :refer [wrap-handler]]))

;;; Response producing functions

(defn allow [& verbs]
  (log/debug "explaining allow verbs")
  {:status 405
   :headers {"Allow" (clojure.string/join "," verbs)}})

(defn get-source
  "Search for a source and produce a response map."
  [source-id]
  (log/debugf "lookup source: %s" source-id)
  (if-let [result (seq (source/search source-id))]
    {:status 200 :body result}
    {:status 404 :body []}))

(defn put-source
  "Handle request for creating a source and produce a response."
  [source-id {params :params :as req}]
  (let [source (merge {:id source-id :state_name "created"} params)]
    (or (some->> (source/validate source)
                 (assoc {:status 403} :body))
        (some->> (source/search source-id)
                 (assoc {:status 409} :body))
        (some->> (source/insert-and-publish source)
                 (assoc {:status 202} :body)))))

(defn get-tiles
  "Get tiles containing point for given UBID and ISO8601 time range."
  [{{:keys [:ubid :x :y :acquired]} :params :as req}]
  (let [tile+    {:ubid ubid
                  :x (Integer/parseInt x)
                  :y (Integer/parseInt y)
                  :acquired (clojure.string/split acquired #"/")}
        tiles (tile/find tile+)]
    (log/debugf "GET /landsat/tiles")
    {:status 200 :body tiles}))

(defn get-tile-spec
  "Search for a source and produce a response map."
  [ubid {params :params :as req}]
  (if-let [results (seq (tile-spec/query (merge {:ubid ubid} params)))]
    {:status 200 :body results}
    {:status 404 :body "none"}))

(defn put-tile-spec
  "Handle request for creating a tile-spec."
  [ubid {params :params :as req}]
  (let [tile-spec (merge {:ubid ubid} params)]
    (or (some->> (tile-spec/validate tile-spec)
                 (assoc {:status 403} :body))
        (some->> (tile-spec/insert tile-spec)
                 (assoc {:status 202} :body)))))

;;; Request entity transformers.

(defn prepare-with
  "Request transform placeholder."
  [request]
  (log/debug "preparing request")
  request)

;;; Response entity transformers.

(defn to-json
  "Encode response body as JSON."
  [response]
  (log/debug "responding with json")
  (update response :body json/encode))

(def supported-types (accept "application/json" to-json
                             "*/*" to-json))

(defn respond-with
  ""
  [request response]
  (supported-types request response))

;;; Routes

(defn resource
  "Handlers for landsat resource."
  []
  (context "/landsat" request
    (-> (routes
         (GET "/" [] {:body "TBD"})
         (ANY "/" [] (allow "GET"))
         (GET "/source/:source-id" [source-id] (get-source source-id))
         (PUT "/source/:source-id" [source-id] (put-source source-id request))
         (GET "/tiles" [] (get-tiles request))
         (GET "/tile-spec/:ubid{.+}" [ubid] (get-tile-spec ubid request))
         (PUT "/tile-spec/:ubid{.+}" [ubid] (put-tile-spec ubid request)))
        (wrap-handler prepare-with respond-with))))
