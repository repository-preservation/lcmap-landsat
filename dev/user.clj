(ns user)

(defn dev
  "Separate function to load the dev namespace.  Provides an extra layer of
  insulation to keep the repl from detonating."
  []
  (require 'lcmap.aardvark.dev)
  (in-ns 'lcmap.aardvark.dev))
