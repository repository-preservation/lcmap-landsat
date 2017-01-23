(ns lcmap.aardvark.tile-spec-index
  "Search index for tile-specs"
  (:require [lcmap.aardvark.config :refer [config]]
            [lcmap.aardvark.es :as es]
            [lcmap.aardvark.tile-spec :as tile-spec]
            [lcmap.commons.collections :refer [vectorize]]
            [clojure.tools.logging :as log]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [org.httpkit.client :as http]))

(defn +tags
  "Appends additional tags to a tile-spec"
  [tile-spec]
  (log/debugf "appending additional tags to tile-spec")
  (let [ubid-tags (str/split (:ubid tile-spec) #"/|_")]
    (if (> (count ubid-tags) 1)
      (update tile-spec :tags conj ubid-tags)
      tile-spec)))

(defn index-entry
   "Converts a tile-spec to an index entry."
  [tile-spec]
  (log/debugf "converting tile-spec to index entry")
  (str (json/write-str {"index" {"_retry_on_conflict" 3}}) "\n"
       (json/write-str tile-spec) "\n"))

(defn clear
  "Clears the tile-spec index"
  []
  (log/infof "clearing the tile-spec index")
  (log/spy :trace (es/clear! (get-in config [:search :index-url]))))

(defn search
  "Submits a query to the index.  Query should conform to elasticsearch
   querystring query syntax. Returns map of raw results"
  ([query]
   (search (get-in config [:search :search-api-url]) query))

  ([api-url query]
   (log/debugf "querying the index")
   (es/search api-url query (get-in config [:search :max-result-size]))))

(defn result
  "Extracts tile-specs from search results"
  [search-results]
  (log/debugf "extracting tile-specs from search results")
  (log/spy :trace
           (map #(get % :_source)(get-in search-results [:hits :hits]))))

(defn save
  "Saves tile-specs to the ElasticSearch index."
  [tile-specs]
  (log/infof "saving index entries")
  (log/spy :trace
           (es/load! (get-in config [:search :bulk-api-url])
                     (pmap #(->> % +tags index-entry) (vectorize tile-specs))))
  tile-specs)
