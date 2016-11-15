(ns lcmap.aardvark.landsat
  "Resources and representations."
  (:require [clojure.tools.logging :as log]
            [compojure.core :refer :all]
            [cheshire.core :as json]
            [clojure.data.xml :as xml]
            [hiccup.core :as html]
            [clojure.string :as string]
            [ring.util.accept :refer [defaccept best-match]]))

;;; Response producing functions

(defn search [req db]
  (log/debug "ard search ...")
  (let [ard {:ubids ["LANDSAT_8/toa/band1", "LANDSAT_8/toa/band2"]}]
    {:status 200 :body [ard]}))

(defn lookup-cat [req db]
  (log/debug "feline search ...")
  (let [cat {:name "Lazarus"}]
    {:status 200 :body cat}))

(defn ingest [req db]
  (log/debug "ingest scene ...")
  {:status 201 :body "scene ingest scheduled"})

(defn delete [req db]
  (log/debug "remove scene ...")
  {:status 410 :body "scene deleted"})

(defn notify [cat msg]
  (log/debug "feline notify ...")
  "some notification data-structure")

;;; Representation encoding functions

(defn cat-to-html
  "Encode response body as HTML"
  [response]
  (log/debug "to html")
  (assoc response :body "<html></html>"))

(defn cat-to-json
  "Encode response body as JSON"
  [response]
  (log/debug "to json")
  (update response :body json/encode))

(defn cat-to-xml
  "Encode response body as XML"
  [response]
  (log/debug "to xml")
  (let [doc (xml/sexp-as-element (response :body))]
    (assoc response :body doc)))

(defaccept encoder
  "text/html" cat-to-html
  "application/json" cat-to-json
  "application/xml" cat-to-xml)

;;; Routes

(defn resource
  "Handlers for landsat resource."
  [db msg]
  (routes
    (context "/" req
     (GET    "/landsat" [] (encoder req #(search req db)))
     (POST   "/landsat" [] (encoder req #(ingest req db)))
     (DELETE "/landsat" [] (encoder req #(delete req db))))))

    ;; Other resources to support inventory, scene to tile mapping reports, etc.
    ;; (context "/landsat/:id" req
    ;; (GET    "/" req (encoder req #(details req db)))
    ;; (POST   "/" req (encoder req #(ingest req db)))
    ;; (PUT    "/" req (encoder req #(update req db)))
    ;; (PATCH  "/" req (encoder req #(update req db)))
    ;; (DELETE "/" req (encoder req #(delete req db))))))
