(ns lcmap.aardvark.setup.database
  "Provides functions for configuring database and event components."
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [dire.core :as dire :refer [with-handler!]]
            [lcmap.aardvark.config :refer [config]]
            [lcmap.aardvark.util :as util]
            [mount.core :as mount :refer [defstate]]
            [qbits.alia :as alia]))

;; Database setup functions.
