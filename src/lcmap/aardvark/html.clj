(ns lcmap.aardvark.html
  ""
  (:require [cheshire.core :as json]
            [clj-time.format :as time-fmt]
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure.spec :as spec]
            [net.cgrand.enlive-html :as html]))

(defn prep-for-html
  ""
  [source]
  (-> source
      (update :progress_at str)
      (update :progress_name str)
      (update :progress_desc str)))

(defn str-vals [kvs]
  ""
  (->> kvs
       (map (fn [[k v]] [k (str v)]))
       (into {})))

;; Used to produce navigation element, intended for use
;; with all templates.
(html/defsnippet nav "public/application.html"
  [:nav]
  []
  identity)

(html/deftemplate default "public/application.html"
  [entity]
  [:nav] (html/content (nav))
  [:#debug] (html/content (json/encode entity {:pretty true})))

;; Used to produce detailed information about a source.

(html/defsnippet progress "public/source-info.html"
  [:table]
  [sources]
  [:table :> :tr] (html/clone-for
                   [source sources]
                   [:.progress-name] (html/content (:progress_name source))
                   [:.progress-desc] (html/content (:progress_desc source))
                   [:time]           (html/content (str (:progress_at source)))))

(html/deftemplate source-info "public/source-info.html"
  [sources]
  [:nav] (html/content (nav))
  [:#id] (html/content (:id (first sources)))
  [:#uri] (html/content (:uri (first sources)))
  [:#checksum] (html/content (:checksum (first sources)))
  [:table] (html/content (progress sources)))

;; Used to produce a list of links to sources.

(html/deftemplate source-list "public/source-list.html"
  [sources]
  [:nav] (html/content (nav))
  [:tbody :tr] (html/clone-for [source sources]
                               [:a] (html/content (:id source))
                               [:a] (html/set-attr :href (str "/landsat/source/" (:id source)))))

(defn describe-tiles
  ""
  [tiles]
  (let [tally (count tiles)
        tile  (first tiles)
        [ubid x y] (vals (select-keys tile [:ubid :x :y]))]
    (format "%s tiles for band %s contain point (%s, %s)" tally ubid x y)))

(html/deftemplate tile-list "public/tile-list.html"
  [tiles]
  [:nav]       (html/content (nav))
  [:header :p] (html/content (describe-tiles tiles))
  [:tbody :tr] (html/clone-for [tile tiles]
                               [:.id] (html/content (str ((juxt :x :y) tile)))
                               [:.source] (html/content (:source tile))
                               [:time] (html/content (-> tile :acquired str))))

(html/deftemplate tile-info "public/tile-info.html"
  [tiles]
  [:nav] (html/content (nav))
  [:content] (html/content "Tile details"))

(html/deftemplate tile-spec-list "public/tile-spec-list.html"
  [tile-specs]
  [:nav] (html/content (nav))
  [:h2] (html/content "Sources")
  [:table :> :tr] (html/clone-for [tile-spec tile-specs]
                                  [:a] (html/content (:ubid tile-spec))))

(html/deftemplate tile-spec-info "public/tile-spec-info.html"
  [tile-spec]
  [:nav] (html/content (nav))
  [:h2] (html/content "Sources")
  [:content :.list] (html/content "Info for tile-spec"))
