(ns lcmap.aardvark.elasticsearch
  "Utility functions for Elasticsearch indices and documents.

  ## Background

  You don't need to be an Elasticsearch expert to use this library,
  but you should know about indices, types, and documents.

  From Elasticsearch Basic Concepts:

  An index is a collection of documents that have somewhat similar
  characteristics.

  Within an index, you can define one or more types. A type is a
  logical category/partition of your index whose semantics is completely
  up to you. In general, a type is defined for documents that have
  a set of common fields.

  A document is a basic unit of information that can be indexed. For
  example, you can have a document for a single customer, another
  document for a single product, and yet another for a single order.
  This document is expressed in JSON (JavaScript Object Notation) which
  is an ubiquitous internet data interchange format.

  See https://www.elastic.co/guide/en/elasticsearch/reference/current/_basic_concepts.html

  ## Tips and Tricks:

  Most functions expect a URL to an index or type within an index.
  You can change the scope of operations by specifying different
  URLs.

  Use `partial` to definine a set of functions for working with
  a specific index (or type).

  Example:

    (def animal-url \"http://localhost:9200/zoo/animals\")
    (index-create animal-url)

    (def animal-index-fn (partial doc-index animal-url))
    (animal-index-fn \"Lazarus\" {:name \"Lazarus\" :type \"cat\"})

    (def animal-get-fn (partial doc-get animal-url))
    (animal-get-fn \"Lazarus\")

    (def animal-search-fn (partial search animal-url))
    (animal-search-fn \"type:cat\")

  "
  (:require [cheshire.core :as json]
            [org.httpkit.client :as http]))

(comment
  ;; Usage Examples (REPL-friendly)
  ;;
  ;; This basic example demonstrates how to create an index,
  ;; and create/update documents.
  ;;

  ;; Creating an index and getting information about it...
  (def index-url "http://localhost:9200/zoo")
  (index-create index-url)
  (index-get index-url)

  ;; Adding types of documents to an index is automatic, you
  ;; don't need to specify one...
  (def type-index-url "http://localhost:9200/zoo/animals")

  ;; Indexing is an upsert, either creating or updating a document.
  (doc-create type-index-url "Lazarus" {:name "Lazarus" :color ["grey"] :kind "cat"})

  ;; Creating will not succeed if the document already exists.
  (doc-create type-index-url "Fido" {:name "Fido" :color ["brown"] :kind "dog"})

  ;; Bulk indexing using a convenience function, this will index (upsert)
  ;; documents with one request.
  (doc-bulk-index type-index-url :name
                  [{:name "Larry" :color ["gold"] :kind "fish"}
                   {:name "Curly" :color ["orange", "white"] :kind "fish"}
                   {:name "Moe"   :color ["red"]}])

  ;; Retrieving a document using an ID is simple, this will retrieve the
  ;; original source.
  (doc-get type-index-url "Lazarus")

  ;; Searching an index using a query string.
  (search type-index-url "grey") ; => The cat named Lazarus

  ;; Query string supports fields and logical operators.
  (search type-index-url "color:grey|white")

  (search "http://localhost:9200/_all" "color:grey|white")

  ;; Removing an index.
  (index-delete index-url))

;;; Supporting functions

(defn- encode
  "Build options for an Elasticsearch HTTP request."
  [document]
  {:headers {"Content-Type" "application/json"}
   :body    (json/encode document)})

(defn- decode
  "Handle a reply for an Elasticsearch HTTP response."
  [response]
  (try
    (update response :body #(json/parse-string % true))
    (catch java.lang.RuntimeException ex
      (ex-info "could not JSON decode elasticsearch response"
               {:body (:body response)
                :cause ex}))))

;;; Index API functions

(defn index-create
  "Create an index, additional options not currently supported.

  see:
  - https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-create-index.html
  "
  [url]
  (let [response @(http/put url)]
    (-> response decode :body :acknowledged)))

(defn index-delete
  "Delete an index.

  see:
  - https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-delete-index.html
  "
  [url]
  (let [response @(http/delete url)]
    (-> response decode :body :acknowledged)))

(defn index-get
  "Retrieve information about one of more indices.

  params:
  - url: URL to an index

  return: JSON decoded response body

  See https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-get-index.html
  "
  [url]
  (let [response @(http/get url)]
    (-> response decode :body)))

(defn index-refresh
  "Explicitly refresh one or more indices, making all operations performed
  since the last refresh available for search.

  params:
  - url: URL to an index

  return: JSON decoded response body

  see:
  - https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-refresh.html
  "
  [url]
  (let [refresh (str url "/_refresh")
        response @(http/post refresh)]
    (-> response decode :body)))

;;; Single Document API functions

(defn doc-index
  "Save document to index, creating a new document or updating an
  existing document having the same ID. This function will URL encode
  the ID and JSON encode the doc.

  args:
  - url  : url with index-name and document type
  - id   : identity of document (do not url-encode)
  - doc  : data (do not JSON encode)
  - opts : additional http-kit request options

  return: `:created` or `:updated`

  see:
  - https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-index_.html
  - https://www.http-kit.org/client.html#options
  "
  ([url id doc opts]
   (let [doc-url  (str url "/" (http/url-encode id))
         req-opts (merge opts (encode doc))
         response @(http/put doc-url req-opts)]
     (-> response decode :body :result keyword)))
  ([url id doc]
   (doc-index url id doc nil)))

(defn doc-create
  "Index document unless it already exists.

  args:
  - url: url to index or type
  - id: document ID, do not URL encode
  - doc: data to index, do not JSON encode

  return: keyword indicating result, should be `:created`

  see:
  - https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-index_.html
  "
  [url id doc]
  (doc-index url id doc {:query-params {:op_type "create"}}))

(defn doc-get
  "Retrieve document by ID.

  args:
  - url: url to index or type
  - id: document ID, do not URL encode

  return:
    original indexed document if it exists, otherwise nil.

  see:
  - https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-get.html
  "
  [url id]
  (let [doc-url (str url "/" (http/url-encode id))
        req-opts {}
        response @(http/get doc-url req-opts)]
    (-> response decode :body :_source)))

(defn doc-delete
  "Remove document from index.

  args:
  - url: URL to index or type
  - id: unencoded document ID

  return:
    true if document exists and was deleted, otherwise false.

  see:
  - https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-delete.html
  "
  [url id]
  (let [doc-url (str url "/" (http/url-encode id))
        req-opts {}
        response @(http/delete doc-url req-opts)]
    (-> response decode :body :result (= "deleted"))))

;;; Multi-document APIs

(defn doc-multi-get
  "Retrive multiple documents by ID.

  If a document cannot be found for a given ID, an entry will
  be provided indicating the item is missing.

  args:
  - url: URL to index or type.
  - ids: unencoded list of IDs.

  return: list of results

  see:
  - https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-multi-get.html
  "
  [url ids]
  (let [multi-get-url (str url "/_mget")
        req-opts (encode {"ids" ids})
        response @(http/get multi-get-url req-opts)]
    (-> response decode :body :docs)))

(defn- doc-bulk-encoder
  "Provide special encoding for bulk API request body.

  Elasticsearch expects the bulk API request body to contain a list of
  newline separated JSON docs. The list must end with a newline, if it
  doesn't the last request will not be processed."
  [requests]
  (map #(str (json/encode %) "\n") requests))

(defn doc-bulk
  "Perform many index/delete operations in a single call.

  This function expects a list of maps that comport with Elasticsearch's
  bulk API. See the easier to use `doc-bulk-index` function if you need
  to upsert multiple documents to the same index.

  Use this function if you need to interact with multiple indices and/or
  perform a variety of actions (delete, create, update, upsert).

  args:
  - url: index
  - requests: list of unencoded requests, see Elasticsearch docs for
      copious details.

  return: JSON decoded bulk API response body.

  see:
  - https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-bulk.html
  "
  [url requests]
  (let [bulk-url (str url "/_bulk")
        req-opts {:body (doc-bulk-encoder requests)
                  :headers {"Content-Type" "application/json"}}
        response @(http/post bulk-url req-opts)]
    (-> response decode :body)))

;; Don't be alarmed, there is probably a well known name for
;; this combinator... but I don't know it. See `request-action`
;; for how it is used.
(defn- if-apply [f x y] (if (f x) (apply x y) x))

(defn request-action
  "Builds an action/document pair.

  This supports a map of functions and values that can be used
  to produce metadata. If an opts map value is an `ifn?` (e.g.
  keyword, fn, etc...) it will be called using document, else
  the value will be used as-is.

  This provides a way to use a document property as an ID, while
  using a string for the index or type name.

  You shouldn't need to call this function directly, it supports
  `doc-bulk-index`.
  "
  [doc action {:keys [:_index :_type :_id] :as opts}]
  (let [replaced (fn [[k v]] [k (if-apply ifn? v [doc])])
        metadata (->> opts (map replaced) (into {}))]
    [{action metadata} doc]))

(defn doc-bulk-index
  "Convenience function for indexing documents.

  Creating bulk requests can be somewhat tedious; this produces a
  data-structure that works with `doc-bulk`.

  args:
  - url: index
  - get-id-fn: function to produce ID for doc
  - docs: collection of docs

  return: JSON decoded Bulk API response body.

  see:
  - `doc-bulk`
  "
  ([url docs {:keys [:_index :_type :_id] :as opts}]
   (let [index-fn (fn [doc] (request-action doc :index opts))
         requests (mapcat index-fn docs)]
     (doc-bulk url requests))))

;;; Search API

(defn search
  "Find documents at url (an index) matching an Elasticsearch query string.

  This function uses the URI search API. The given query and opts are
  combined into query parameters.

  args:
  - url: index to query
  - query: Elasticsearch query string.
  - opts: URI search options (size, offset, etc...)

  return: list of search results.

  see:
  - https://www.elastic.co/guide/en/elasticsearch/reference/current/search-uri-request.html
  "
  ([url query params]
   (let [search-url (str url "/_search")
         req-opts {:query-params (merge params {:q query})}
         response @(http/get search-url req-opts)]
     (-> response decode :body)))
  ([url query]
   (search url query {})))

(defn hits->sources
  "Transform hits (from `search`) into sources."
  [hits]
  (->> hits :hits :hits (map :_source)))

;;; Cat APIs

(def ^:dynamic *cat-defaults* {:format "json"})

(defn cat-count
  ""
  ([url opts]
   (let [count-url (str url "/_cat/count")
         req-opts {:query-params (merge *cat-defaults* opts)}
         response @(http/get count-url req-opts)]
     (-> response decode :body)))
  ([url]
   (cat-count url {})))

(defn cat-health
  ""
  ([url opts]
   (let [health-url (str url "/_cat/health")
         req-opts {:query-params (merge *cat-defaults* opts)}
         response @(http/get health-url req-opts)]
     (-> response decode :body)))
  ([url]
   (cat-health url {})))

(defn cat-indices
  ""
  ([url opts]
   (let [indices-url (str url "/_cat/indices")
         req-opts {:query-params (merge *cat-defaults* opts)}
         response @(http/get indices-url req-opts)]
     (-> response decode :body)))
  ([url]
   (cat-indices url {})))
