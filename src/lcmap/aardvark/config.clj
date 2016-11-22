(ns lcmap.aardvark.config
  "Configuration!"
  (:require [uberconf.core :as uberconf]
            [clojure.tools.logging :as log]
            [schema.core :as schema]))

(def config-schema
  {:http     {:port schema/Num }
   :database {:contact-points [schema/Str]}
   :event    {:host schema/Str :port schema/Int}
   schema/Keyword schema/Str})

;;(defn build [{:keys [ini] :or {ini "lcmap-landsat.ini"} :as args}]
;;  (uberconf/init-cfg {:ini ini :schema config-schema}))

(defn build [{:keys [edn] :or {edn "lcmap-landsat.edn"} :as args}]
  (do (log/info "Building configuration from " edn)
      (uberconf/init-cfg {:edn edn :schema config-schema})))
