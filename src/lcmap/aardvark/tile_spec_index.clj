(ns lcmap.aardvark.tile-spec-index
  "Search index for tile-spec ubids (universal band ids)."
  (:require [lcmap.aardvark.config :refer [config]]
            [lcmap.aardvark.es :as es]
            [lcmap.aardvark.tile-spec :as tile-spec]
            [lcmap.commons.string :refer [strip]]
            [lcmap.commons.collections :refer [vectorize]]
            [clojure.tools.logging :as log]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [org.httpkit.client :as http]))

(defn server-url
  "Returns the url to the search server"
  []
  (strip "/" (get-in config [:search :url])))


(defn index-name
  "Returns the name of the search index"
  []
  (strip "/" (get-in config [:search :ubid-index])))


(defn url
  "Returns the url to the search index"
  []
  (str (server-url) "/" (index-name)))


(defn bulk-api-url
  "Returns the url to bulk api"
  []
  (str (url) "/"
       (strip "/" (get-in config [:search :ubid-index-type])) "/_bulk"))


(defn search-api-url
  "Returns the url to the search api"
  []
  (str (url) "/_search"))

(defn universal-band-ids []
  "Retrieves all tile-spec ubids"
  (distinct (map #(:ubid %) (tile-spec/all [:ubid]))))

(defn ubid->tags
  "Extracts tags from a sequence of ubids and returns a map with the
  ubid and tags."
  [ubids]
  (log/debugf "UBID Payload coming in:%s" ubids)
  (map #(assoc {} :ubid % :tags (str/split % #"/|_")) (vectorize ubids)))


(defn tags->index-payload
  "Creates a bulk index payload from a sequence of tags"
  [tags]
  (let []
    (apply str
           (map #(str
                  (json/write-str {"index" {"_retry_on_conflict" 3}}) "\n"
                  (json/write-str {"ubid" (:ubid %) "tags" (:tags %)}) "\n")
                tags))))

(defn load!
  "Loads index payload into index/url"
  ([]
   (load! (universal-band-ids)))

 ([ubids]
  (log/debugf "UBIDS COMING INTO LOAD!:%s" ubids)
  (let [payload (-> ubids ubid->tags tags->index-payload)]
    (log/debug "Loading payload:" payload " into bulk api at:" (bulk-api-url))
    (es/load! (bulk-api-url) payload))))

(defn clear!
  "Clears the tile-spec-index"
  []
  (es/clear! (url)))

(defn search
  "Submits a supplied query to the ubid index, which should conform to the
   elasticsearch query syntax. Returns a clojure dictionary of the raw results"
  ([query]
   (search (search-api-url) query))

  ([api-url query]
   (es/search api-url query (get-in config [:search :max-result-size]))))

(defn search->ubids
  "Returns sequence of ubids from search results"
  [search-results]
  (map #(get-in % ["_source" "ubid"])
       (get-in search-results ["hits" "hits"])))

(defn index-spec!
  "Create index entry in ES"
  [tile-spec]
  (log/debugf "XXXX creating index entry for %s" tile-spec)
  (load! (:ubid tile-spec))
  tile-spec)
