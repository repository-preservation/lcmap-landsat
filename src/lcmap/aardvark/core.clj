(ns lcmap.aardvark.core
  "Entrypoint for HTTP mode."
  (:require [mount.core :as mount]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.tools.logging :as log]
            [lcmap.aardvark.state :as state]
            [lcmap.aardvark.config :as config])
  (:gen-class))

(def cli-spec [[nil "--http.port VALUE"]
               [nil "--http.join? VALUE"]
               [nil "--http.daemon? VALUE"]
               [nil "--database.contact-points VALUE"]
               [nil "--event.host VALUE"]
               [nil "--event.port VALUE"]
               [nil "--server"]
               [nil "--worker"]])

(defn -main
  "Start the app."
  [& args]
  (let [cli (parse-opts args cli-spec)]
    (when (get-in cli [:options :server])
      (log/info "HTTP server mode enabled")
      (require 'lcmap.aardvark.server))
    (when (get-in cli [:options :worker])
      (log/info "AMQP worker mode enabled")
          (require 'lcmap.aardvark.worker)))
  (mount/start (mount/with-args {:config {:cli {:args args
                                                :spec cli-spec}
                                          :env {:prefix "lcmap.landsat."
                                                :separator #"\."}
                                          :edn "lcmap-landsat.edn"}})))
