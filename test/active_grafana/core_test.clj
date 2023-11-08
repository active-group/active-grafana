(ns active-grafana.core-test
  (:require [clojure.test :refer :all]
            [active-grafana.core :refer :all]))

(deftest a-test
  (testing "I'm a happy test."
    (is (= (foo "Active")
           nil))))
