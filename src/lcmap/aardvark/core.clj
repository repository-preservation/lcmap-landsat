(ns lcmap.aardvark.core
  "Functions for starting a server, worker, or both.

  The server is an HTTP handler and the worker is an AMQP consumer.

  Both modes of operation can be run in a single process, although
  in a production environment they should be run separately so that
  each can be scaled independently to handle varying workloads.

  See also:
  * `dev/lcmap/aardvark/dev.clj` for REPL-driven development.
  * `dev/resources/lcmap-landsat.edn` for configuration."
  (:require [again.core :as again]
            [mount.core :refer [defstate] :as mount]
            [clojure.edn :as edn]
            [clojure.tools.logging :as log]
            [clojure.walk :as walk]
            [lcmap.aardvark.config :as config]
            [lcmap.aardvark.util :as util])
  (:gen-class))

(defstate hook
  :start (do
           (log/debugf "registering shutdown handler")
           (.addShutdownHook (Runtime/getRuntime)
                             (Thread. #(mount/stop) "shutdown-handler"))))

;; This nested map contains environment variable names. These values
;; are replaced with actual values during startup and merged into
;; the default configuration map, `def-cfg`.
(def environ-cfg {:database {:default-keyspace "AARDVARK_DB_KEYSPACE"
                             :cluster {:contact-points "AARDVARK_DB_HOST"
                                       :credentials {:user "AARDVARK_DB_USER"
                                                     :password "AARDVARK_DB_PASS"}}}
                  :http    {:port      "AARDVARK_HTTP_PORT"}
                  :event   {:host      "AARDVARK_EVENT_HOST"
                            :user      "AARDVARK_EVENT_USER"
                            :password  "AARDVARK_EVENT_PASS"}
                  :server  {:exchange  "AARDVARK_SERVER_EVENTS"
                            :queue     "AARDVARK_SERVER_EVENTS"}
                  :worker  {:exchange  "AARDVARK_WORKER_EVENTS"
                            :queue     "AARDVARK_WORKER_EVENTS"}})

;; This nested map contains a default configuration. It is updated with `env-cfg`
;; values during startup.
(def default-cfg {:database  {:cluster {:contact-points ["localhost"]
                                        :socket-options {:read-timeout-millis 20000}}
                              :default-keyspace "lcmap_landsat"}
                  :http      {:port 5678
                              :join? false
                              :daemon? true}
                  :event     {:host "localhost"
                              :port 5672}
                  :server    {:exchange "lcmap.landsat.server"
                              :queue    "lcmap.landsat.server"}
                  :worker    {:exchange "lcmap.landsat.worker"
                              :queue    "lcmap.landsat.worker"}
                  :search    {:index-url      "http://localhost:9200/tile-specs"
                              :refresh-url    "http://localhost:9200/tile-specs/_refresh"
                              :bulk-api-url   "http://localhost:9200/tile-specs/local/_bulk"
                              :search-api-url "http://localhost:9200/tile-specs/_search"
                              :max-result-size 10000}})

(defn env-name->env-value
  "Replaces all string values with the matching environment variable."
  [nested-map]
  (walk/prewalk #(if (string? %) (System/getenv %) %) nested-map))

(defn env->cfg
  "Provide a config map with merged environment values."
  []
  (let [nil-value? (comp nil? last)]
    (->> (env-name->env-value environ-cfg)
         (util/deep-remove nil-value?)
         (util/deep-merge default-cfg))))

(defn -main
  "Start the server, worker, or both."
  [& args]
  (let [cfg (env->cfg)]
    (log/debugf "cfg: '%s'" cfg)
    (when (get-in cfg [:server])
      (log/info "HTTP server mode enabled")
      (require 'lcmap.aardvark.server))
    (when (get-in cfg [:worker])
      (log/info "AMQP worker mode enabled")
      (require 'lcmap.aardvark.worker))

    ;;; Retry and try catch are to wait for system resources to become
    ;;; available.
    (try
      (mount/start (mount/with-args {:config cfg}))
      (catch RuntimeException e
        (log/fatalf e "Could not start lcmap.landsat... exiting")
        (System/exit 1)))))
