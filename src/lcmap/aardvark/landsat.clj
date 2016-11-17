(ns lcmap.aardvark.landsat
  "Resources and representations."
  (:require [clojure.tools.logging :as log]
            [compojure.core :refer :all]
            [clojure.string :as string]))

;;; Response producing functions

(defn search [req db]
  (log/debug "aardvark search ...")
  {:status 200 :body "LANDSAT_8/toa/band1"})

(defn ingest [req db]
  (log/debug "ingest scene ...")
  {:status 201 :body "scene ingest scheduled"})

(defn delete [req db]
  (log/debug "remove scene ...")
  {:status 410 :body "scene deleted"})

(defn notify [cat msg]
  (log/debug "aardvark notify ...")
  "some notification data-structure")

;;; Routes

(defn resource
  "Handlers for landsat resource."
  [db msg]
  (routes
    (context "/" req
     (GET    "/landsat" [] (search req db))
     (POST   "/landsat" [] (ingest req db))
     (DELETE "/landsat" [] (delete req db)))))
