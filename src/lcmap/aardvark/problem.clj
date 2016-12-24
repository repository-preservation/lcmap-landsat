(ns lcmap.aardvark.problem
  "Project specific problem resources."
  (:require [clojure.tools.logging :as log]
            [compojure.core :refer :all]
            [ring.middleware.problem :as problem :refer [defproblems]])
  (:import (java.net URI)))

;;; Problem building related functions.

(defn lcmap-class? [element]
  (clojure.string/includes? (.getClassName element) "lcmap"))

(defn app-stack-trace [ex]
  (let [elements (.getStackTrace ex)]
    (filter lcmap-class? elements)))

(defn detail+
  "Add ex-data to problem"
  [problematic exception]
  ;; This isn't exactly how problems are supposed to
  ;; be used; this should only really happen in
  ;; development mode.
  (log/debug "adding exception info to problem")
  (-> problematic
      (problem/make-instance exception)
      (assoc :detail (or (ex-data exception)
                         (.getMessage exception))
             :trace (map str (app-stack-trace exception)))))

(defproblems lcmap-landsat-problems
  [[java.lang.RuntimeException
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
  (log/debugf "save problem to db: %s" problem)
  problem)

(defn find-problem
  "Retrieve problem, both type or instance."
  [request]
  (log/debug request)
  (problem/problem (ex-info "Example" {})))

(defn transformer
  "Used with problem/wrap-problem to transform problem before
  building response."
  [problem request]
  (log/debug "transforming problem")
  (-> problem
      (save-problem)
      (link-problem request)))
