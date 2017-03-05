(ns lcmap.aardvark.shared
  (:require [clojure.edn :as edn]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [lcmap.aardvark.config :as config]
            [lcmap.aardvark.elasticsearch :as es]
            [lcmap.aardvark.server :as server]
            [lcmap.aardvark.worker :as worker]
            [lcmap.aardvark.fixtures :as fixtures]
            [lcmap.aardvark.util :as util]
            [mount.core :as mount]
            [org.httpkit.client :as http]))

(defn req
  "Convenience function for making HTTP requests."
  [method url & {:keys [headers query-params form-params body]
                 :as   opts}]
  (let [defaults {:headers {"Accept" "application/json"}}]
    @(http/request (merge {:url url :method method} defaults opts))))
