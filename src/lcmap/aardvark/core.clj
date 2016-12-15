(ns lcmap.aardvark.core
  "Entrypoint for HTTP mode."
  (:require [mount.core :as mount]
            [clojure.edn :as edn]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.tools.logging :as log]
            [lcmap.aardvark.state :as state]
            [lcmap.aardvark.config :as config])
  (:gen-class))

(defn args->cfg
  "Transform STDIN args (EDN) to data."
  [args]
  (->> args
       (clojure.string/join " ")
       (clojure.edn/read-string)))

(defn -main
  "Start the app."
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
