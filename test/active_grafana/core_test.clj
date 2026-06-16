(ns active-grafana.core-test
  (:require [active-grafana.core :as sut]
            [active-grafana.grafana-api :as api]
            [active-grafana.examples :as examples]
            [active-grafana.grafana-api-stub :as api-stub]
            [clojure.test :as t :refer [deftest testing is]]
            [bond.james :refer [with-stub!]]))

(deftest ambiguous-candidates
  (testing "no candidates"
    (let [example []]
      (is (= :none (sut/ambiguous-candidates example)))))
  (testing "one candidate"
    (let [example ["one"]]
      (is (= :unambiguous (sut/ambiguous-candidates example)))))
  (testing "more than one candidate"
    (let [example ["one" "two"]]
      (is (= :ambiguous (sut/ambiguous-candidates example))))))

(deftest choose-dashboard-metadata-test
  (testing "no dashboard is found"
    (with-stub!
      [[api/find-dashboards-by-query
        (api-stub/find-dashboards-by-query :none)]
       [api/get-dashboards api-stub/get-dashboards]]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"No dashboard with the following title was found"
                            (sut/choose-dashboard-metadata examples/grafana-a-instance
                                                           examples/dashboard-title)))))
  (testing "1 dashboard is found"
    (with-stub!
      [[api/find-dashboards-by-query
        (api-stub/find-dashboards-by-query :unambiguous)]
       [api/get-dashboards api-stub/get-dashboards]]
      (is (= examples/dashboard-title
             (get (sut/choose-dashboard-metadata examples/grafana-a-instance
                                                 examples/dashboard-title)
                  "title")))))
  (testing "more than 1 dashboard is found"
    (with-stub!
      [[api/find-dashboards-by-query
        (api-stub/find-dashboards-by-query :ambiguous)]
       [api/get-dashboards api-stub/get-dashboards]]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"More than one dashboard was found"
                            (sut/choose-dashboard-metadata examples/grafana-a-instance
                                                           examples/dashboard-title))))))

(deftest choose-folder-uid-test
  (testing "no folder is found"
    (with-stub!
      [[api/find-folders-by-query [(api-stub/find-folders-by-query :none)
                                   (api-stub/find-folders-by-query :unambiguous)]]
       [api/create-folder api-stub/create-folder]]
      (is (= "dflyuecbw3cw0f"
             (sut/choose-folder-uid "test-thingy"
                                    examples/grafana-b-instance
                                    examples/folder-title)))))
  (testing "1 folder is found"
    (with-stub!
      [[api/find-folders-by-query
        (api-stub/find-folders-by-query :unambiguous)]
       [api/create-folder api-stub/create-folder]]
      (is (= "dflyuecbw3cw0f"
             (sut/choose-folder-uid "test-thingy"
                                    examples/grafana-b-instance
                                    examples/folder-title)))))
  (testing "more than 1 folder is found"
    (with-stub!
      [[api/find-folders-by-query
        (api-stub/find-folders-by-query :ambiguous)]
       [api/create-folder api-stub/create-folder]]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"More than one folder was found"
                            (sut/choose-folder-uid "test-thingy"
                                                   examples/grafana-b-instance
                                                   examples/folder-title))))))
