(ns lcmap.aardvark.tile-spec-index
  "Search index for tile-specs"
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
  "Returns the url to the bulk api"
  []
  (str (url) "/"
       (strip "/" (get-in config [:search :ubid-index-type])) "/_bulk"))

(defn search-api-url
  "Returns the url to the search api"
  []
  (str (url) "/_search"))

(defn +tags
  "Appends additional tags to a tile-spec"
  [tile-spec]
  (let [ubid-tags (str/split (:ubid tile-spec) #"/|_")]
    (if (> (count ubid-tags) 1)
      (update tile-spec :tags conj ubid-tags)
      tile-spec)))

(defn index-entry
   "Converts a tile-spec to an index entry."
  [tile-spec]
  (str (json/write-str {"index" {"_retry_on_conflict" 3}}) "\n"
       (json/write-str tile-spec) "\n"))

(defn clear
  "Clears the tile-spec index"
  []
  (es/clear! (url)))

(defn search
  "Submits a query to the index.  Query should conform to elasticsearch
   querystring query syntax. Returns map of raw results"
  ([query]
   (search (search-api-url) query))

  ([api-url query]
   (es/search api-url query (get-in config [:search :max-result-size]))))

(defn result
  "Extracts tile-specs from search results"
  [search-results]
  (map #(get % "_source")
       (get-in search-results ["hits" "hits"])))

(defn save
  "Saves tile-specs to the ElasticSearch index."
  [tile-specs]
  (log/debugf "creating index entry(s) for %s" tile-specs)
  (es/load! (bulk-api-url)
            (map #(->> % +tags index-entry) (vectorize tile-specs)))
  tile-specs)
