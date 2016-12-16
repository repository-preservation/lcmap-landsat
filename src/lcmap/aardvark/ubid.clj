(ns lcmap.aardvark.ubid
  "ubid-index supports searching for ubids (universal band ids) given a
   set of tokens that make up ubids.  The index can also be loaded from the
   IWDS here, which should be an infrequent and manually initiated function."
  (:require [lcmap.aardvark.db :refer [db-session]]
            [lcmap.aardvark.config :refer [config]]
            [lcmap.commons.string :refer [strip-both]]
            [qbits.alia :as alia]
            [clojure.tools.logging :as log]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [org.httpkit.client :as http]))

(defn get-server-url
  "Returns the url to the search server"
  []
  (strip-both "/" (get-in config [:search :url])))

(defn get-search-index-name
  "Returns the name of the search index"
  []
  (strip-both "/" (get-in config [:search :ubid-index])))

(defn get-search-index-url
  "Returns the url to the search index"
  []
  (str (get-server-url) "/" (get-search-index-name)))

(defn get-bulk-api-url
  "Returns the url to bulk api"
  []
  (str (get-search-index-url) "/"
       (strip-both "/" (get-in config [:search :ubid-index-type])) "/_bulk"))

(defn get-search-api-url
  "Returns the url to the search api"
  []
  (str (get-search-index-url) "/_search"))

(defn get-ubids
  "Returns ubids, which are a sequence of slash separated strings
   such as LANDSAT_5/TM/sr_band1, or nil if none exist."

  ([] (get-ubids (get-in config [:database :default-keyspace])))

  ([db-keyspace]
   (let [query (str "select ubid from " db-keyspace ".tile_specs")
         results (try (alia/execute db-session query)
                   (catch Throwable t
                     (log/debug
                      (apply str (interpose "\n" (.getStackTrace t))))))
         ubids (distinct (map :ubid results))]
    ubids)))

(defn ubid->tags
  "Extracts tags from a sequence of ubids and returns a sequence with the
  ubid as the first element and resulting tags as the rest."
  [ubids]
  (map #(conj (seq (str/split % #"/|_")) %) ubids))

(defn tags->index-payload
  "Creates a bulk index payload from a sequence of ubids"
  [tags]
  (let []
       (apply str
         (map #(str
                (json/write-str {"index" {"_retry_on_conflict" 3}}) "\n"
                (json/write-str {"ubid" (first %) "tags" (rest %)}) "\n")
               tags))))

(defn- get-errors
  "Returns errors from json payload if one exists, nil otherwise"
  [response]
  (get-in (json/read-str response)["error"]))

(defn load-index!
  "Loads index-payload into index-url"
  ([]
   (let [payload (tags->index-payload
                  (ubid->tags
                   (get-ubids
                    (get-in config [:database :default-keyspace]))))
         bulk-url (get-bulk-api-url)]
     (log/debug "Loading payload:" payload " into bulk api at:" bulk-url)
     (load-index! bulk-url payload)))

  ([index-url index-payload]
   (let [{:keys [status headers body error] :as resp} @(http/post index-url
                                                        {:body index-payload})
         errors (or error (get-errors body))]
      (if errors
        (do (log/debug (str "load-index! errors:" errors)) errors)
        (do (log/debug (str "load-index! success:" body)) body)))))

(defn clear-index!
  "Clears the index specified by index-url"
  ([]
   (clear-index! (get-search-index-url)))

  ([index-url]
   (let [{:keys [status headers body error] :as resp} @(http/delete index-url)
         errors (or error (get-errors body))]
     (if errors
       (do (log/debug (str "clear-index! errors:" errors)) errors)
       (do (log/debug (str "clear-index! success:" body)) body)))))

(defn search
  "Submits a supplied query to the ubid index, which should conform to the
   elasticsearch query syntax. Returns a clojure dictionary of the raw results"
 ([query]
  (search (get-search-api-url) query))

 ([search-api-url query]
  (let [full-url (str search-api-url "?q=" (http/url-encode query))
        {:keys [status headers body error] :as resp} @(http/get full-url)
        errors (or error (get-errors body))]
       (if errors
         (do (log/debug (str "search error:" errors "full url:" full-url))
           errors)
         (do (log/debug (str "search success:" body "full url:" full-url))
           (json/read-str body))))))

(defn search->ubids
  "Returns sequence of ubids from search results"
  [search-results]
  (map #(get-in % ["_source" "ubid"])
       (get-in search-results ["hits" "hits"])))
