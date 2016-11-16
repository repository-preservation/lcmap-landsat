(ns lcmap.aardvark.landsat
  "Resources and representations."
  (:require [clojure.tools.logging :as log]
            [compojure.core :refer :all]
            [clojure.string :as string]))

;;; Response producing functions

(defn search [req db]
  (log/debug "aardvark search ...")
  (let [ard {:ubids ["LANDSAT_8/toa/band1", "LANDSAT_8/toa/band2"]}]
    {:status 200 :body [ard]}))

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

    ;; Other resources to support inventory, scene to tile mapping reports, etc.
    ;; (context "/landsat/:id" req
    ;; (GET    "/" req (encoder req #(details req db)))
    ;; (POST   "/" req (encoder req #(ingest req db)))
    ;; (PUT    "/" req (encoder req #(update req db)))
    ;; (PATCH  "/" req (encoder req #(update req db)))
    ;; (DELETE "/" req (encoder req #(delete req db))))))
