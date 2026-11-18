(ns active-grafana.helper-test
  (:require [active-grafana.helper :as sut]
            [clojure.test :as t :refer [deftest testing is]]))

(deftest json-string?-test
  (testing "The number 1 is no json-string."
    (is (not (sut/json-string? 1))))
  (testing "The number 1 is no json-string."
    (is (not (sut/json-string? 1.2))))
  (testing "The number 1 is no json-string."
    (is (not (sut/json-string? 1/2))))
  (testing "nil is no json-string."
    (is (not (sut/json-string? nil))))
  (testing "[] is no json-string."
    (is (not (sut/json-string? []))))
  (testing "{} is no json-string."
    (is (not (sut/json-string? {}))))
  (testing "#{} is no json-string."
    (is (not (sut/json-string? #{}))))
  (testing "'() is no json-string."
    (is (not (sut/json-string? '())))))
