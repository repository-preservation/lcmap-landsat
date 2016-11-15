(ns landsat.middleware
  (:require [clojure.tools.logging :as log]))

;;; These wrappers ought to be defined in a separate project.

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
