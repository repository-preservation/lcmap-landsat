(ns lcmap.aardvark.middleware
  (:require [clojure.stacktrace :as stacktrace]
            [clojure.tools.logging :as log]
            [cheshire.core :as json]
            [ring.util.accept :refer [defaccept best-match]]))

(defn wrap-handler
  "Generic request/response transformer.

  The response transform fn is given both the request and
  response map because the response may be transformed in
  a way that depends on request values; e.g. content-type."
  [handler req-tf res-tf]
  (fn [request]
    (res-tf request (handler (req-tf request)))))

(defn wrap-request-debug
  "Logs request at DEBUG level"
  [handler]
  (fn [request]
    (log/debugf "request-debug: %s" request)
    (handler request)))

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

(defn ex->resp
  "Produce Ring-style response body from exception."
  [ex]
  (try
    (let [info (-> (bean ex)
                   (update :class str)
                   (select-keys [:class :cause :message]))]
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body (json/generate-string info)})
    (catch RuntimeException ex
      {:status 500
       :body "Another exception occurred converting exception into JSON response. :sad:"})))

(defn wrap-exception
  [handler]
  (fn [req]
    (try
      (handler req)
      (catch Throwable ex
        ;; this will print a stack trace, useful for debugging.
        (log/debug ex (stacktrace/root-cause ex))
        ;; this will not print a stack trace, just a short message.
        (log/errorf "%s: %s" (.getMessage ex) (ex-data ex))
        ;; produce a ring style response map
        (ex->resp ex)))))
