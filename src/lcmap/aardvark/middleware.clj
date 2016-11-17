(ns lcmap.aardvark.middleware
  (:require [clojure.tools.logging :as log]
            [cheshire.core :as json]
            [ring.util.accept :refer [defaccept best-match]]))

  ;;; Representation encoding functions

(defn to-netcdf
  "Encode response body as Netcdf"
  [response]
  (log/debug "to netcdf")
  (assoc response :body "<html></html>"))

(defn to-geotiff
  "Encode response body as GeoTiff"
  [response]
  (log/debug "to geotiff")
  (update response :body json/encode))

(defn to-json
  "Encode response body as JSON"
  [response]
  (log/debug "to json")
  (update response :body json/encode))

(defaccept encoder
  "application/netcdf" to-netcdf
  "application/geotiff" to-geotiff)

(defn wrap-response-body
  "Changes representation of response body based on accept headers"
  [handler]
  (fn [request]
    (log/debug "req - wrap response body")
    (let [response (handler request)]
      (log/debug "resp = wrap response body")
      (encoder request #(merge {} response)))))

(defn wrap-content-type
  "Transform request body and response body using content-type headers"
  [handler]
  (fn [request]
    (log/debug "req - content-type wrapper ...")
    ;; XXX use content-type header to transform the request body
    ;;     into a data-structure
    (let [response (handler request)]
      (log/debug "res - content-type wrapper ...")

      ;; XXX use accept header to transform response data-structure
      ;;     into a response string
      response)))

(defn wrap-authenticate
  "Add Identity to request map"
  [handler]
  (fn [request]
    (log/debug "req - authenticate wrapper ...")
    (let [response (handler request)]
      (log/debug "res - authenticate wrapper ...")
      response)))

(defn wrap-authorize
  "Use Identity in request map to authorize access to resource ..."
  [handler]
  (fn [request]
    (log/debug "req - authorize wrapper ...")
    (let [response (handler request)]
      (log/debug "res - authorize wrapper ...")
      response)))
