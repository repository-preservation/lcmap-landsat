(ns lcmap.aardvark.es
  "Search index utilities"
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [org.httpkit.client :as http]))

(defn- get-errors
  "Returns errors from json payload if one exists, nil otherwise"
  [response]
  (get-in (json/read-str response :key-fn keyword) [:error]))

(defn load!
  "Loads payload into index at url"
  ([url payload]
   (let [{:keys [body error] :as resp} @(http/post url {:body payload})
         errors (or error (get-errors body))]
     (if errors
       (do (log/debug (str "load! errors:" errors)) errors)
       (do (log/debug (str "load! success:" body)) body)))))

(defn clear!
  "Clears the index specified by index/url"
  [index-url]
  (let [{:keys [body error] :as resp} @(http/delete index-url)
        errors (or error (get-errors body))]
    (if errors
      (do (log/debug (str "clear! errors:" errors)) errors)
      (do (log/debug (str "clear! success:" body)) body))))

(defn search
  "Submits a supplied query to the elastic index.
   Returns a hashmap of raw results"
   [api-url query result-size]
   (let [full-url (str api-url
                       "?q=" (http/url-encode query) "&size=" result-size)
         {:keys [body error] :as resp} @(http/get full-url)
         errors (or error (get-errors body))]
     (if errors
       (do (log/debug (str "search error:" errors "full url:" full-url))
           errors)
       (do (log/debug (str "search success:" body "full url:" full-url))
           (json/read-str body :key-fn keyword)))))
