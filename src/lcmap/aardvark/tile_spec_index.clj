(ns lcmap.aardvark.tile-spec-index
  "Search index for tile-spec ubids (universal band ids)."
  (:require [lcmap.aardvark.db :refer [db-session]]
            [lcmap.aardvark.config :refer [config]]
            [lcmap.aardvark.tile-spec :refer [universal-band-ids]]
            [lcmap.commons.string :refer [strip]]
            [qbits.alia :as alia]
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

(defn ubid->tags
  "Extracts tags from a sequence of ubids and returns a sequence with the
  ubid as the first element and resulting tags as the rest."
  [ubids]
  (map #(conj (seq (str/split % #"/|_")) %) ubids))

(defn tags->index-payload
  "Creates a bulk index payload from a sequence of tags"
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
  (get-in (json/read-str response) ["error"]))

(defn load!
  "Loads index payload into index/url"
  ([]
   (let [payload (tags->index-payload (ubid->tags (universal-band-ids)))
         bulk-url (bulk-api-url)]
     (log/debug "Loading payload:" payload " into bulk api at:" bulk-url)
     (load! bulk-url payload)))

  ([url payload]
   (let [{:keys [status headers body error] :as resp} @(http/post url
                                                                  {:body payload})
         errors (or error (get-errors body))]
     (if errors
       (do (log/debug (str "load! errors:" errors)) errors)
       (do (log/debug (str "load! success:" body)) body)))))

(defn clear!
  "Clears the index specified by index/url"
  ([]
   (clear! (url)))

  ([index-url]
   (let [{:keys [status headers body error] :as resp} @(http/delete index-url)
         errors (or error (get-errors body))]
     (if errors
       (do (log/debug (str "clear! errors:" errors)) errors)
       (do (log/debug (str "clear! success:" body)) body)))))

(defn search
  "Submits a supplied query to the ubid index, which should conform to the
   elasticsearch query syntax. Returns a clojure dictionary of the raw results"
  ([query]
   (search (search-api-url) query))

  ([api-url query]
   (let [full-url (str api-url "?q=" (http/url-encode query))
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
