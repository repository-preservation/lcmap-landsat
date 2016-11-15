(ns landsat.cat
  "Resources and representations."
  (:require [clojure.tools.logging :as log]
            [compojure.core :refer :all]
            [cheshire.core :as json]
            [clojure.data.xml :as xml]
            [hiccup.core :as html]
            [clojure.string :as string]
            [ring.util.accept :refer [defaccept best-match]]))

;;; Response producing functions

(defn search-cat [req db]
  (log/debug "feline search ...")
  (let [cat {:name "Lazarus"}]
    {:status 200 :body [cat]}))

(defn lookup-cat [req db]
  (log/debug "feline search ...")
  (let [cat {:name "Lazarus"}]
    {:status 200 :body cat}))

(defn create-cat [req db]
  (log/debug "feline create ...")
  {:status 201 :body "cat created"})

(defn delete-cat [req db]
  (log/debug "feline remove ...")
  {:status 410 :body "cat deleted"})

(defn update-cat [req db]
  (log/debug "feline replace ...")
  {:status 200 :body "cat updated"})

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
  "Handlers for feline resource."
  [db msg]
  (routes
    (context "/" req
     (GET    "/cat" [] (encoder req #(search-cat req db)))
     (POST   "/cat" [] (encoder req #(create-cat req db))))
    (context "/cat/:id" req
     (GET    "/" req (encoder req #(lookup-cat req db)))
     (PUT    "/" req (encoder req #(update-cat req db)))
     (PATCH  "/" req (encoder req #(update-cat req db)))
     (DELETE "/" req (encoder req #(delete-cat req db))))))
