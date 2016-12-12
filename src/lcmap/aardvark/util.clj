(ns lcmap.aardvark.util
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [digest]
            [me.raynes.fs :as fs])
  (:import [org.apache.commons.compress.archivers
            ArchiveInputStream ArchiveStreamFactory]
          [org.apache.commons.compress.compressors
            CompressorInputStream CompressorStreamFactory]))


;;; Archive handling utilities

(defn checksum
  ""
  [source]
  (let [path (source :uri)
        expected (source :checksum)]
    (-> path
        (io/as-url)
        (io/as-file)
        (digest/md5))))

(defn checksum!
  ""
  [source]
  (let [actual (checksum source)
        expected (:checksum source)]
  (if (not= actual expected)
    (throw (ex-info "checksum failed" {:expected expected :actual actual :path (:uri source)}))
    source)))

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
  "Temporarily uncompress and unarchive file at path.

  Provide a binding for the temporary directory to use in body."
  [[binding path] & body]
  `(let [tf# (fs/temp-file "lcmap-")
         td# (fs/temp-dir "lcmap-")]
     (try
      (log/debug "uncompressing" ~path "to" (.getAbsolutePath td#))
      (uncompress ~path tf#)
      (unarchive tf# td#)
      (let [~binding td#]
        (do ~@body))
      (finally
        (log/debug "cleaning up" td#)
        (fs/delete tf#)
        (fs/delete-dir td#)))))
