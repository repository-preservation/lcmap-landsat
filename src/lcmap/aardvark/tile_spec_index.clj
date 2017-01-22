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
  (log/tracef "returning the server-url")
  (strip "/" (get-in config [:search :url])))

(defn index-name
  "Returns the name of the search index"
  []
  (log/tracef "returning the index-name")
  (strip "/" (get-in config [:search :ubid-index])))

(defn url
  "Returns the url to the search index"
  []
  (log/debugf "returning the search index url")
  (str (server-url) "/" (index-name)))

(defn bulk-api-url
  "Returns the url to the bulk api"
  []
  (log/debugf "returning the bulk api url")
  (str (url) "/"
       (strip "/" (get-in config [:search :ubid-index-type])) "/_bulk"))

(defn search-api-url
  "Returns the url to the search api"
  []
  (log/debugf "returning the search-api-url")
  (str (url) "/_search"))

(defn +tags
  "Appends additional tags to a tile-spec"
  [tile-spec]
  (log/debugf "appending additional tags to tile-spec")
  (let [ubid-tags (str/split (:ubid tile-spec) #"/|_")]
    (log/tracef "====> for tile-spec: %s" tile-spec)
    (if (> (count ubid-tags) 1)
      (update tile-spec :tags conj ubid-tags)
      tile-spec)))

(defn index-entry
   "Converts a tile-spec to an index entry."
  [tile-spec]
  (log/debugf "converting tile-spec to index entry")
  (log/tracef "====> for tile-spec: %s" tile-spec)
  (str (json/write-str {"index" {"_retry_on_conflict" 3}}) "\n"
       (json/write-str tile-spec) "\n"))

(defn clear
  "Clears the tile-spec index"
  []
  (log/infof "clearing the tile-spec index")
  (es/clear! (url)))

(defn search
  "Submits a query to the index.  Query should conform to elasticsearch
   querystring query syntax. Returns map of raw results"
  ([query]
   (search (search-api-url) query))

  ([api-url query]
   (log/debugf "querying the index")
   (log/debugf "====> api-url: %s query: %s" api-url query)
   (es/search api-url query (get-in config [:search :max-result-size]))))

(defn result
  "Extracts tile-specs from search results"
  [search-results]
  (log/debugf "extracting tile-specs from search results")
  (log/tracef "====> for search-results: %s" search-results)
  (map #(get % "_source")
       (get-in search-results ["hits" "hits"])))

(defn save
  "Saves tile-specs to the ElasticSearch index."
  [tile-specs]
  (log/infof "saving index entries")
  (log/tracef "====> for tile-spec: %s" tile-specs)
  (es/load! (bulk-api-url)
            (pmap #(->> % +tags index-entry) (vectorize tile-specs)))
  tile-specs)
