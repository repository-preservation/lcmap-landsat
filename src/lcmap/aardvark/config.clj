(ns lcmap.aardvark.config
  "Configuration!"
  (:require [uberconf.core :as uberconf]
            [schema.core :as schema]))

(def config-schema
  {:http     {:port schema/Str}
   :database {:contact-points [schema/Str]}
   :event    {:host schema/Str :port schema/Str}
   schema/Keyword schema/Str})

(defn build [{:keys [ini] :or {ini "lcmap-landsat.ini"} :as args}]
  (uberconf/init-cfg {:ini ini :schema config-schema}))
