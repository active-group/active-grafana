(ns active-grafana.convenient-bond
  (:require [bond.james :refer [calls]]))

(defn calls-count= [n f]
  (= n (-> f calls count)))

(def not-called? (partial calls-count= 0))
(def called-once? (partial calls-count= 1))

