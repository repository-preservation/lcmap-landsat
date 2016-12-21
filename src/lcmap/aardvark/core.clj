(ns lcmap.aardvark.core
  "Functions for starting a server, worker, or both.

  The server is an HTTP handler and the worker is an AMQP consumer.

  Both modes of operation can be run in a single process, although
  in a production environment they should be run separately so that
  each can be scaled independently to handle varying workloads.

  See also:
  * `dev/lcmap/aardvark/dev.clj` for REPL-driven development.
  * `dev/resources/lcmap-landsat.edn` for configuration."
  (:require [mount.core :refer [defstate] :as mount]
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
    (mount/start (mount/with-args {:config cfg}))))
