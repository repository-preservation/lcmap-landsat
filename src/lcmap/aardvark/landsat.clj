(ns lcmap.aardvark.landsat
  "Resources and representations."
  (:require [cheshire.core :as json]
            [clj-time.format :as time-fmt]
            [clj-time.coerce :as time-coerce]
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [compojure.core :refer :all]
            [ring.util.accept :refer [accept]]
            [lcmap.aardvark.tile :as tile]
            [lcmap.aardvark.middleware :refer [wrap-handler]]))

;;; Response producing functions

(defn allow [& verbs]
  (log/debug "explaining allow verbs")
  {:status 405
   :headers {"Allow" (clojure.string/join "," verbs)}})

;;; Request entity related functions

(defn prepare-with
  ""
  [request]
  (log/debug "preparing request")
  request)

;;; Response entity related functions

(defn to-json
  "Encode response body as JSON"
  [response]
  (log/debug "to JSON")
  (update response :body json/encode))

(def supported-types (accept "application/json" to-json
                             "*/*" to-json))

(defn respond-with
  ""
  [request response]
  (log/debug "responding with a supported content type")
  (supported-types request response))

;;; Routes

(defn resource
  "Handlers for landsat resource."
  []
  (context "/landsat" request
    (-> (routes
         (GET    "/" [] {:body "TBD"})
         (ANY    "/" [] (allow "GET")))
        (wrap-handler prepare-with respond-with))))
