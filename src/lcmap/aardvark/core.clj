(ns lcmap.aardvark.core
  "Entrypoint for app."
  (:require [mount.core :as mount]
            [clojure.tools.logging :as log]
            [lcmap.aardvark.state :as state]
            [lcmap.aardvark.config :as config])
  (:gen-class))

(def cli-spec [[nil "--http.port VALUE"]
               [nil "--http.join? VALUE"]
               [nil "--http.daemon? VALUE"]
               [nil "--database.contact-points VALUE"]
               [nil "--event.host VALUE"]
               [nil "--event.port VALUE"]])

(defn -main
  "Start the app."
  [& args]
  (mount/start (mount/with-args {:config {:cli {:args args
                                                :spec cli-spec}
                                          :env {:prefix "lcmap.landsat."
                                                :separator #"\."}
                                          :edn "lcmap-landsat.edn"}})))
