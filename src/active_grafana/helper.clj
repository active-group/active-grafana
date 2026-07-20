(ns active-grafana.helper
  (:require [cheshire.core :as json]))

(set! *warn-on-reflection* true)

;; POD vs PROGRAM behaviour

(defn error-logic
  []
  (if (System/getenv "BABASHKA_POD")
    (throw (ex-info (str "Something went wrong. That's what happened before:\n" *out*) {}))
    (System/exit -1)))

;; LOG/DEBUG

(defn log
  [& log-strs]
  (apply println log-strs))

(defn debug
  [deb]
  (prn deb)
  deb)

;; JSON/CLJ

(defn json-string? [x]
  (boolean
   (and (string? x)
        (try (json/parse-string x)
             (catch clojure.lang.ExceptionInfo _e
               false)))))

(defn json->clj
  [request]
  (json/parse-string (:body request)))

;; json from clojure map
(defn clj->json
  [body]
  (json/generate-string body))
