(ns lcmap.aardvark.problem
  "Project specific problem resources."
  (:require [clojure.tools.logging :as log]
            [compojure.core :refer :all]
            [ring.middleware.problem :as problem])
  (:import (java.net URI)))

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
  [problem db]
  (log/debug "Save problem to db")
  problem)

(defn find-problem
  "Retrieve problem, both type or instance."
  [db request]
  (log/debug request)
  (problem/problem (ex-info "Example" {})))

(defn resource
  "Handlers for problem resource"
  [db msg]
  (context "/problems" req
   (ANY "/" [] (problem/as-json problem/default-problems))
   (ANY "/example" [] (throw (ex-info "Some Clojure exception info" {})))))

(defn transformer
  "Used with problem/wrap-problem to transform problem before
  building response."
  [problem req db]
  (-> problem
      (link-problem req)
      (save-problem db))
  problem)
