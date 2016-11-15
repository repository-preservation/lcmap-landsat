(ns lcmap.aardvark.app
  "Build app from middleware and handlers."
  (:require [clojure.tools.logging :as log]
            [compojure.core :refer :all]
            [ring.middleware.accept :refer [wrap-accept]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.problem :refer [wrap-problem]]
            [lcmap.aardvark.landsat :as landsat]
            [lcmap.aardvark.middleware :as middleware]
            [lcmap.aardvark.problem :as problem]))


(defn new-handler
  "Build a middleware wrapped handler for app. This approach makes
  dependencies (components) available to handling functions."
  [db msg]
  (context "/" req
    (-> (routes (cat/resource db msg)
                (problem/resource db msg))
        (wrap-accept)
        (middleware/wrap-content-type)
        (middleware/wrap-authorize)
        (middleware/wrap-authenticate)
        (wrap-keyword-params)
        (wrap-params)
        (wrap-problem (problem/transformer req db msg)))))
