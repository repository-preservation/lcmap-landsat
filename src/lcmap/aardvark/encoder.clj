(ns lcmap.aardvark.encoder
  "Provide automatic encoding of native Java types into JSON.

  This alleviates JSON representation functions from having to
  perform tedious updates of values when producing a response."
  (:require [cheshire.generate :as json-gen :refer [add-encoder]]
            [clojure.tools.logging :as log]
            [mount.core :refer [defstate] :as mount])
  (:import [org.joda.time.DateTime]
           [org.apache.commons.codec.binary Base64]))

(defn iso8601-encoder
  "Transform a Joda DateTime object into an ISO8601 string."
  [date-time generator]
  (.writeString generator (str date-time)))

(defn base64-encoder
  "Base64 encode a byte-buffer, usually raster data from Cassandra."
  [buffer generator]
  (log/debug "encoding HeapByteBuffer")
  (let [size (- (.limit buffer) (.position buffer))
        copy (byte-array size)]
    (.get buffer copy)
    (.writeString generator (Base64/encodeBase64String copy))))

(defstate json-encoders
  :start (do
           (json-gen/add-encoder org.joda.time.DateTime iso8601-encoder)
           (json-gen/add-encoder java.nio.HeapByteBuffer base64-encoder)))
