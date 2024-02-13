(ns active-grafana.pod
  (:refer-clojure :exclude [read-string read])
  (:require [bencode.core      :as bencode]
            [clojure.walk      :as walk]
            [cognitect.transit :as transit]
            [instaparse.core   :as insta]

            [active-grafana.main-adjust :as adjust]
            [active-grafana.main-copy   :as copy])
  (:import [java.io PushbackInputStream])
  (:gen-class))

(set! *warn-on-reflection* true)

(def stdin (PushbackInputStream. System/in))

(defn read-string [^"[B" v]
  (String. v))

(defn read []
  (bencode/read-bencode stdin))

(defn write [v]
  (bencode/write-bencode System/out v)
  (.flush System/out))

(def describe-map
  (walk/postwalk
   (fn [v] (if (ident? v) (name v) v))
   {:format :transit+json
    :namespaces [{:name "active-grafana.pod"
                  :vars [{"name" "adjust"}
                         {"name" "copy"}]
                  :ops {"shutdown" {}}}]}))

(defn read-transit [^String v]
  (transit/read
   (transit/reader
    (java.io.ByteArrayInputStream. (.getBytes v "utf-8"))
    :json)))

(defn serialize- [x]
  (if (instance? instaparse.auto_flatten_seq.AutoFlattenSeq x)
    (seq x)
    x))

(defn serialize [x]
  (clojure.walk/prewalk serialize- x))

(defn write-transit [v]
  (let [baos (java.io.ByteArrayOutputStream.)]
    (transit/write (transit/writer baos :json) v)
    (.toString baos "utf-8")))

(defn -main [& _args]
  (loop []
    (let [message (try (read)
                       (catch java.io.EOFException _
                         ::EOF))]
      (when-not (identical? ::EOF message)
        (let [op (get message "op")
              op (read-string op)
              op (keyword op)
              id (some-> (get message "id")
                         read-string)
              id (or id "unknown")]
          (case op
            :describe (do (write describe-map)
                          (recur))
            :invoke (do (try
                          (let [var (get message "var")
                                var (read-string var)
                                args (get message "args")
                                args (read-string args)
                                args (read-transit args)
                                res-map (case var
                                          "active-grafana.pod/adjust" {"value"  (write-transit (serialize nil))
                                                                       "id"     id
                                                                       "out"    (with-out-str (apply adjust/-main args))
                                                                       "status" ["done"]}
                                          "active-grafana.pod/copy"   {"value"  (write-transit (serialize nil))
                                                                       "id"     id
                                                                       "out"    (with-out-str (apply copy/-main args))
                                                                       "status" ["done"]}
                                          (throw (ex-info (str "Var not found: " var) {})))]
                            (write res-map))
                          (catch Throwable e
                            (let [reply {"ex-message" (ex-message e)
                                         "ex-data"    (write-transit (assoc (ex-data e) :type (str (class e))))
                                         "id"         id
                                         "status"     ["done" "error"]}]
                              (write reply))))
                        (recur))
            :shutdown (System/exit 0)
            (do
              (let [reply {"ex-message" "Unknown op"
                           "ex-data" (pr-str {:op op})
                           "id"      id
                           "status"  ["done" "error"]}]
                (write reply))
              (recur))

            ))))))
