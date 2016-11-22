(ns lcmap.aardvark.landsat
  "Resources and representations."
  (:require [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [compojure.core :refer :all]
            [ring.util.accept :refer [accept]]
            [lcmap.aardvark.middleware :refer [wrap-handler]]))

;;; Response producing functions

(defn search [req db]
  (log/debug "aardvark search ...")
  {:status 200 :body "LANDSAT_8/toa/band1"})

(defn ingest [req db]
  (log/debug "ingest scene ...")
  {:status 201 :body "scene ingest scheduled"})

(defn delete [req db]
  (log/debug "remove scene ...")
  {:status 410 :body "scene deleted"})

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

(defn to-netcdf
  "Encode response body as Netcdf"
  [response]
  (log/debug "to netcdf")
  (assoc response :body "NetCDF"))

(defn to-geotiff
  "Encode response body as GeoTiff"
  [response]
  (log/debug "to geotiff")
  (assoc response :body "geotiff"))

(defn to-html
  ""
  [response]
  (log/debug "to html")
  (assoc response :body "HTML"))

(defn to-json
  "Encode response body as JSON"
  [response]
  (log/debug "to JSON")
  (update response :body json/encode))

(def supported-types (accept "application/netcdf" to-netcdf
                             "application/geotiff" to-geotiff
                             "application/json" to-json
                             "text/html" to-html))

(defn respond-with
  ""
  [request response]
  (log/debug "responding with a supported content type")
  (supported-types request response))

;;; Routes

(defn resource
  "Handlers for landsat resource."
  [db]
  (context "/" req
    (-> (routes
           (GET    "/landsat" [] (search req db))
           (POST   "/landsat" [] (ingest req db))
           (DELETE "/landsat" [] (delete req db))
           (ANY    "/landsat" [] (allow "GET" "POST" "DELETE")))
        (wrap-handler prepare-with respond-with))))
