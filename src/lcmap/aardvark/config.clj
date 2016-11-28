(ns lcmap.aardvark.config
  "Configuration!"
  (:require [uberconf.core :as uberconf]
            [clojure.tools.logging :as log]
            [schema.core :as schema]))

(def config-schema
  {:http     {:port schema/Num
              (schema/optional-key :join?) schema/Bool
              (schema/optional-key :daemon?) schema/Bool}
   :database {:contact-points [schema/Str]}
   :event    {:host schema/Str :port schema/Num}
   schema/Keyword schema/Str})

(defn build [{:keys [edn cli env schema]
              :or {schema config-schema}
              :as args}]
  (log/debugf "using schema: '%s'" schema)
  (log/debugf "build config: '%s'" (uberconf/build-cfg args))
  (uberconf/init-cfg {:edn edn :cli cli :env env :schema schema}))
