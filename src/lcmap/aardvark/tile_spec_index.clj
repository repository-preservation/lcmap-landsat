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

(defn +tags
  "Creates additional tags from a ubid"
  [tile-spec]
  (let [ubid-tags (str/split (:ubid tile-spec) #"/|_")]
    (if (> (count ubid-tags) 1)
      (update tile-spec :tags conj ubid-tags)
      tile-spec)))

(defn index-entry
   "Creates an index entry."
  [tile-spec]
  (str (json/write-str {"index" {"_retry_on_conflict" 3}}) "\n"
       (json/write-str tile-spec) "\n"))

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

(defn results
  "Returns sequence of tile-specs from search results"
  [search-results]
  (map #(get % "_source")
       (get-in search-results ["hits" "hits"])))

(defn index-spec!
  "Create index entry(s) in ES"
  [tile-specs]
  (log/debugf "creating index entry(s) for %s" tile-specs)
  (es/load! (bulk-api-url)
            (map #(->> % +tags index-entry) (vectorize tile-specs)))
  tile-specs)
