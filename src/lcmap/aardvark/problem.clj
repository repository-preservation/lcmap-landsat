(ns lcmap.aardvark.problem
  "Project specific problem resources."
  (:require [clojure.tools.logging :as log]
            [compojure.core :refer :all]
            [ring.middleware.problem :as problem :refer [defproblems]])
  (:import (java.net URI)))

;;; Problem building related functions.

(defn detail+
  "Add ex-data to problem"
  [problematic exception]
  (-> problematic
      (problem/make-instance exception)
      (assoc :detail (ex-data exception))))

(defproblems lcmap-landsat-problems
  [[clojure.lang.ExceptionInfo
    {:type "lcmap-landsat-default-problem"
     :title "LCMAP Landsat Problem"
     :status 500}
    detail+]])

;;; Problem representation related functions.

(defn uri-str
  "Build URI for resource, and optionally a request."
  ([{:keys [path query fragment]
     :as resource}
    {:keys [scheme server-name server-port]
     :or {server-port -1}
     :as request}]
   ;; scheme, the 1st param, may be a keyword
   ;; userInfo, the 2nd param, is not supported (yet).
   (str (URI. (if scheme (name scheme) "http")
              nil
              server-name
              server-port
              path
              query
              fragment)))
  ([resource] (uri-str resource {})))

(defn link-problem
  "Turns type and instance properties into URLs. In order to do this,
  :type and :instance are replaced with maps that can be used by
  The `uri` helper function. Including a request provides information
  needed to build an absolute URL."
  [problem request]
  (let [path "/problem"]
    (-> problem
        (update :type (fn [id] {:path path :query (format "type=%s" id)}))
        (update :type uri-str request)
        (update :instance (fn [id] {:path path :query (format "instance=%s" id)}))
        (update :instance uri-str request))))

(defn save-problem
  "Persist instance of a problem for subsequent retrieval."
  [problem]
  (log/debug "Save problem to db")
  problem)

(defn find-problem
  "Retrieve problem, both type or instance."
  [request]
  (log/debug request)
  (problem/problem (ex-info "Example" {})))

(defn transformer
  "Used with problem/wrap-problem to transform problem before
  building response."
  [problem req]
  (-> problem
      (link-problem req)
      (save-problem))
  problem)

(defn resource
  "Handlers for problem resource"
  []
  (context "/problems" request
   (ANY "/" [] (problem/as-json problem/default-problems))
   (ANY "/example" [] (throw (ex-info "Some Clojure exception info" {})))))
