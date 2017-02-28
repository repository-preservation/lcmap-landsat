(ns lcmap.aardvark.tile-spec-index
  "Search index for tile-specs"
  (:require [lcmap.aardvark.config :refer [config]]
            [lcmap.aardvark.es :as es]
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
  (let [index-url (get-in config [:search :index-url])]
    (log/infof "clearing the tile-spec index")
    (log/spy :trace (es/clear! index-url))))

(defn search
  "Submits a query to the index.  Query should conform to elasticsearch
   querystring query syntax. Returns map of raw results"
  ([query]
   (let [index-url (get-in config [:search :index-url])
         search-url (str index-url "/_search")]
     (search search-url query)))
  ([api-url query]
   (log/infof "querying the index: %s" api-url)
   (es/search api-url query (get-in config [:search :max-result-size]))))

(defn result
  "Extracts tile-specs from search results"
  [search-results]
  (log/info "extracting tile-specs from search results")
  (log/spy :trace
           (map #(get % :_source)
                (get-in search-results [:hits :hits]))))

(defn refresh
  "Refreshes the search index"
  []
  (let [index-url (get-in config [:search :index-url])
        refresh-url (str index-url "/_refresh")]
    (comment "This should be working but isn't.  Elasticsearch results
              are not immediately available until they are flushed to disk.
              Caller can either wait 1 second (per the docs) or call refresh
              endpoint"
             (es/refresh! refresh-url))
    (log/tracef "Refreshing search index")
    #_(Thread/sleep 1000)
    (es/refresh! refresh-url))
  true)

(defn save
  "Saves tile-specs to the ElasticSearch index."
  [tile-specs]
  (log/infof "saving index entries")
  (let [index-url (get-in config [:search :index-url])
        bulk-url (str index-url "/tile-specs/_bulk")]
    (log/spy :trace
             (es/load! bulk-url
                       (pmap #(->> % +tags index-entry) (vectorize tile-specs)))))
  (refresh)
  tile-specs)
