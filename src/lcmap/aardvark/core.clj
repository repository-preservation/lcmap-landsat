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
            [lcmap.aardvark.config :as config])
  (:gen-class))

(defstate hook
  :start (do
           (log/debugf "registering shutdown handler")
           (.addShutdownHook (Runtime/getRuntime)
                             (Thread. #(mount/stop) "shutdown-handler"))))

(defn args->cfg
  "Transform STDIN args (EDN) to data.

  CLI arguments are automatically split on whitespace; this function
  joins arguments before reading the first form."
  [args]
  (->> args
       (clojure.string/join " ")
       (clojure.edn/read-string)))

(def retry-strategy (again/max-retries 10 (again/constant-strategy 5000)))

(defn -main
  "Start the server, worker, or both."
  [& args]
  (let [cfg (args->cfg args)]
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
      (again/with-retries retry-strategy
        (do (log/info "Stopping mount components")
            (mount/stop)
            (log/info "Starting mount components...")
            (mount/start (mount/with-args {:config cfg}))))
      (catch Exception e
        (log/fatalf e "Could not start landsat... exiting"))
      (finally
        (try
          (mount/stop)
          (catch Exception e
            (log/errorf e "Error stopping mount... exiting")))
        (System/exit 1)))))
