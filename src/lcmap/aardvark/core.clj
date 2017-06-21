(ns lcmap.aardvark.core
  "Functions for starting a server and worker."
  (:require [mount.core :refer [defstate] :as mount]
            [clojure.tools.logging :as log]
            [clojure.walk :as walk]
            [lcmap.aardvark.config :as config]
            [lcmap.aardvark.server :as server]
            [lcmap.aardvark.worker :as worker]
            [lcmap.aardvark.util :as util])
  (:gen-class))

;; A shutdown hook provides a way to intentionally stop mount 'beings'
;; but it isn't guaranteed to run. This is defined here because it is
;; only useful when the main entrypoint is used.

(defstate hook
  :start (do
           (log/debug "registering shutdown handler")
           (.addShutdownHook (Runtime/getRuntime)
                             (Thread. #(mount/stop) "shutdown-handler"))))

;; This nested map contains environment variable names. These values
;; are replaced with actual values during startup and merged into
;; the default configuration map, `default-cfg`.

(def environ-cfg {:database {:cluster {:contact-points "AARDVARK_DB_HOST"
                                       :credentials {:user "AARDVARK_DB_USER"
                                                     :password "AARDVARK_DB_PASS"}}
                             :default-keyspace "AARDVARK_DB_KEYSPACE"}
                  :html     {:base-url  "AARDVARK_BASE_URL"}
                  :http     {:port      "AARDVARK_HTTP_PORT"}
                  :event    {:host      "AARDVARK_EVENT_HOST"
                             :port      "AARDVARK_EVENT_PORT"
                             :user      "AARDVARK_EVENT_USER"
                             :password  "AARDVARK_EVENT_PASS"}
                  :server   {:exchange  "AARDVARK_SERVER_EVENTS"
                             :queue     "AARDVARK_SERVER_EVENTS"}
                  :worker   {:exchange  "AARDVARK_WORKER_EVENTS"
                             :queue     "AARDVARK_WORKER_EVENTS"}
                  :search   {:index-url "AARDVARK_SEARCH_INDEX_URL"
                             :chip-spec-url "AARDVARK_CHIP_SPEC_URL"}})

;; This nested map contains a default configuration. It is updated
;; with `environ-cfg` values during startup.

(def default-cfg {:database  {:cluster {:contact-points "cassandra"
                                        :socket-options {:read-timeout-millis 20000}
                                        :query-options {:consistency :quorum}}
                              :default-keyspace "lcmap_landsat"
                              :schema {:setup false :teardown false}}
                  :http      {:port 5678
                              :join? false
                              :daemon? true}
                  :event     {:host "rabbitmq"
                              :port 5672}
                  :server    {:exchange "lcmap.landsat.server"
                              :queue    "lcmap.landsat.server"}
                  :worker    {:exchange "lcmap.landsat.worker"
                              :queue    "lcmap.landsat.worker"}
                  :search    {:index-url "http://elasticsearch:9200/lcmap-landsat"
                              :chip-spec-url "http://elasticsearch:9200/lcmap-landsat/chip-spec"
                              :max-result-size 1000}})

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
    (log/debugf "start lcmap.aardvark.core using config: '%s'" cfg)
    (try
      (mount/start (mount/with-args {:config cfg}))
      (catch RuntimeException e
        (log/fatalf e "could not start lcmap.aardvark.core; exiting.")
        (System/exit 1)))))
