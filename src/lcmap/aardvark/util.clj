(ns lcmap.aardvark.util
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [digest]
            [clj-http.client :as client]
            [me.raynes.fs :as fs])
  (:import [org.apache.commons.compress.archivers
            ArchiveInputStream ArchiveStreamFactory]
           [org.apache.commons.compress.compressors
            CompressorInputStream CompressorStreamFactory]))

;;; Archive handling utilities

(defn checksum
  ""
  [uri]
  (-> uri
      (io/as-url)
      (io/as-file)
      (digest/md5)))

(defn verify
  "Compare expected and actual MD5 checksum of URI.

  Raise an exception if different, otherwise return uri."
  [uri expected]
  (let [actual (checksum uri)]
    (if (not= actual expected)
      (throw (ex-info "checksum failed" {:expected expected :actual actual :uri uri}))
      uri)))

(defmulti download
  (fn [uri file]
    (.getProtocol (io/as-url uri))))

(defmethod download "file"
  [uri file]
  (with-open [in (io/input-stream uri)
              out (io/output-stream file)]
    (io/copy in out))
  (io/file file))

(defmethod download "http"
  [uri file]
  (with-open [in (:body (client/get uri {:as :stream}))
              out (io/output-stream file)]
    (io/copy in out))
  (io/file file))

(defn entries
  "Lazily retrieve a list of archive entries."
  [archive]
  (when-let [entry (.getNextEntry archive)]
    (cons entry (lazy-seq (entries archive)))))

(defn create-entry
  "Creates a file at dest from entry in archive."
  [archive entry dest]
  (let [{:keys [:name :file]} (bean entry)
        output-file (fs/file dest name)]
    (cond file (do (-> output-file fs/parent fs/mkdirs)
                   (io/copy archive output-file)))))

(defn unarchive
  "Unpacks archive entries in file at src into dest directory.

  This handles archives, multiple files represented as a single file,
  for example, a tar file."
  ([src]
   (unarchive src (fs/file (fs/base-name src true))))
  ([src dest]
   (with-open [src-stream (io/input-stream src)
               archive (.createArchiveInputStream (new ArchiveStreamFactory) src-stream)]
     (doseq [entry (entries archive)]
       (create-entry archive entry dest)))
   dest))

(defn uncompress
  "Applies decompression function to file at src into dest file.

  This handles compressed files (e.g. gz, xz, bz2) but not archived files
  (e.g. tar, cpio)."
  ([src]
   (uncompress src (fs/file (fs/base-name src true))))
  ([src dest]
   (with-open [src-stream (io/input-stream src)
               dest-stream (io/output-stream dest)]
     (let [csf (new CompressorStreamFactory)
           cis (.createCompressorInputStream csf src-stream)]
       (io/copy cis dest-stream)))
   dest))

(defmacro with-temp
  ""
  [binding & body]
  `(let [temp# (fs/temp-file "lcmap.landsat-")]
     (try
       (log/debugf "created temp-file: %s" (.getAbsolutePath temp#))
       (let [~binding temp#]
         (do ~@body))
       (finally
         (log/debug "removing temp-file: %s" (.getAbsolutePath temp#))
         (fs/delete temp#)))))

(defmacro with-temp-dir
  ""
  [binding & body]
  `(let [temp# (fs/temp-file-dir "lcmap.landsat-")]
     (try
       (log/debugf "created temp-file: %s" (.getAbsolutePath temp#))
       (let [~binding temp#]
         (do ~@body))
       (finally
         (log/debug "removing temp-file: %s" (.getAbsolutePath temp#))
         (fs/delete-dir temp#)))))

(defn vectorize
  "Guarantees value is a vector."
  [value]
  (cond (vector? value) value
        (sequential? value)(into [] value)
        :else (conj [] value)))
