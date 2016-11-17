(ns lcmap.aardvark.middleware
  (:require [clojure.tools.logging :as log]
            [cheshire.core :as json]
            [ring.util.accept :refer [defaccept best-match]]))

(defn wrap-handler
  "Generic request/response transformer.

  The response transform fn is given both the request and
  response map because the response may be transformed in
  a way that depends on request values; e.g. content-type."
  [handler req-tf res-tf]
  (fn [request]
    (log/debug "req - wrap handler ...")
    (let [request (req-tf request)
          response (handler request)]
      (log/debug "res - wrap handler ...")
      (res-tf request response))))

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
