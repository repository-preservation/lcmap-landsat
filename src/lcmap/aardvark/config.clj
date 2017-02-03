(ns lcmap.aardvark.config
  "Config-related state management.

  This application is configured by reading EDN; either from a file
  during development/test or from STDIN when built and deployed.

  Schema is used in tandem with `uberconf` to enforce the presence of
  required values and coerce values into the expected types (numbers,
  booleans, and lists)."
  (:require [uberconf.core :as uberconf]
            [clojure.tools.logging :as log]
            [mount.core :refer [defstate] :as mount]
            [schema.core :as schema]))

(def http {(schema/optional-key :port) schema/Num
           (schema/optional-key :join?) schema/Bool
           (schema/optional-key :daemon?) schema/Bool
           schema/Keyword schema/Str})

;;; event configuration is structured to support AMQP providers
;;; see
(def event {:host schema/Str
            :port schema/Num
            :queues [{:name schema/Str
                      :opts {schema/Keyword schema/Any}}]
            :exchanges [{:name schema/Str
                         :type schema/Str
                         :opts {schema/Keyword schema/Any}}]
            :bindings [{:exchange schema/Str
                        :queue schema/Str
                        :opts {schema/Keyword schema/Str}}]
            schema/Keyword schema/Str})

;;; For full db cluster config reference,
;;; see https://github.com/mpenet/alia/blob/master/docs/guide.md
;;; Holding off describing all options as alia should validate
;;; it's own config.  If this is still necessary, consider waiting
;;; for clojure 1.9 with clojure.spec.
(def database {:cluster {:contact-points [schema/Str]
                         schema/Keyword schema/Any}
               :default-keyspace schema/Str})

(def server {:exchange schema/Str
             :queue schema/Str})

(def worker {:exchange schema/Str
             :queue schema/Str})

(def search {:index-url schema/Str
             :refresh-url schema/Str
             :bulk-api-url schema/Str
             :search-api-url schema/Str
             :max-result-size schema/Num})

(def root-cfg
  {:event event
   :database database
   :search search
   (schema/optional-key :http) http
   (schema/optional-key :server) server
   (schema/optional-key :worker) worker
   schema/Keyword schema/Str})

(defstate config
  :start (let [cfg ((mount/args) :config)]
           (log/debugf "starting config: %s" cfg)
           (->> cfg
                (uberconf.core/coerce-cfg root-cfg)
                (uberconf.core/check-cfg root-cfg))))
