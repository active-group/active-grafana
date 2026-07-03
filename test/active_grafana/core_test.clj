(ns active-grafana.core-test
  (:require [active-grafana.core :as sut]
            [active-grafana.helper :as helper]
            [active-grafana.grafana-api :as api]
            [active-grafana.examples :as examples]
            [active-grafana.grafana-api-stub :as api-stub]
            [clojure.test :as t :refer [deftest testing is]]
            [bond.james :refer [with-stub! calls]]))

(defn calls-count= [n f]
  (= n (-> f calls count)))

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
  (let [dashboard-title "Simple dashboard"]
    (testing "no dashboard is found"
      (with-stub!
        [[api/find-dashboards-by-query
          (api-stub/find-dashboards-by-query [])]
         [api/get-dashboards api-stub/get-dashboards]]
        (is (thrown-with-msg? clojure.lang.ExceptionInfo
                              #"No dashboard with the following title was found"
                              (sut/choose-dashboard-metadata examples/grafana-a-instance
                                                             dashboard-title)))))
    (testing "1 dashboard is found"
      (with-stub!
        [[api/find-dashboards-by-query
          (api-stub/find-dashboards-by-query [{:title dashboard-title}])]
         [api/get-dashboards api-stub/get-dashboards]]
        (is (= dashboard-title
               (get (sut/choose-dashboard-metadata examples/grafana-a-instance
                                                   dashboard-title)
                    "title")))))
    (testing "more than 1 dashboard is found"
      (with-stub!
        [[api/find-dashboards-by-query
          (api-stub/find-dashboards-by-query [{:title dashboard-title}
                                              {:title dashboard-title}])]
         [api/get-dashboards api-stub/get-dashboards]]
        (is (thrown-with-msg? clojure.lang.ExceptionInfo
                              #"More than one dashboard was found"
                              (sut/choose-dashboard-metadata examples/grafana-a-instance
                                                             dashboard-title)))))))

(deftest choose-folder-uid-test
  (testing "no folder is found"
    (with-stub!
      [[api/find-folders-by-query [(api-stub/find-folders-by-query :none)
                                   (api-stub/find-folders-by-query :unambiguous)]]
       [api/create-folder api-stub/create-folder]]
      (is (= examples/folder-uid
             (sut/choose-folder-uid examples/grafana-b-instance
                                    examples/folder-title)))))
  (testing "1 folder is found"
    (with-stub!
      [[api/find-folders-by-query
        (api-stub/find-folders-by-query :unambiguous)]
       [api/create-folder api-stub/create-folder]]
      (is (= examples/folder-uid
             (sut/choose-folder-uid examples/grafana-b-instance
                                    examples/folder-title)))))
  (testing "more than 1 folder is found"
    (with-stub!
      [[api/find-folders-by-query
        (api-stub/find-folders-by-query :ambiguous)]
       [api/create-folder api-stub/create-folder]]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"More than one folder was found"
                            (sut/choose-folder-uid examples/grafana-b-instance
                                                   examples/folder-title))))))

(deftest find-dashboard-related-panels-test
  (with-stub!
    [[api/get-dashboard-by-uid (api-stub/get-dashboard-by-uid {})]
     [api/get-library-element-by-uid api-stub/get-library-element-by-uid]]
    (is (seq? (sut/find-dashboard-related-panels examples/grafana-a-instance
                                                 examples/dashboard-uid)))))

(deftest find-dashboard-related-alert-rules-test
  (with-stub!
    [[api/get-all-alert-rules (api-stub/get-all-alert-rules [{} {}])]]
    (is (seq? (sut/find-dashboard-related-alert-rules examples/grafana-a-instance
                                                      examples/dashboard-uid)))))

(deftest check-and-choose-panels-folder-title-test
  (testing "all panels are located in same folder"
    (let [folder-id {:folder-uid "my-folder" :folder-name "My folder"}]
      (is (= (:folder-name folder-id)
             (sut/check-and-choose-panels-folder-title
              (examples/make-dashboard-related-panels [folder-id folder-id]))))))
  (testing "not all panels are located in same folder"
    (let [folder-id-1 {:folder-uid "my-folder" :folder-name "My folder"}
          folder-id-2 {:folder-uid "another-folder" :folder-name "Another folder"}]
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           #"The panels are located in different folders"
           (sut/check-and-choose-panels-folder-title
            (examples/make-dashboard-related-panels [folder-id-1 folder-id-2])))))))

(deftest check-and-choose-alert-folder-uid-test
  (testing "all alerts are located in same folder"
    (let [folder-id {:folder-uid "my-folder" :folder-name "My folder"}]
      (is (= (:folder-uid folder-id)
             (sut/check-and-choose-alert-folder-uid
              (examples/make-dashboard-related-alerts [folder-id folder-id]))))))
  (testing "not all alerts are located in same folder"
    (let [folder-id-1 {:folder-uid "my-folder" :folder-name "My folder"}
          folder-id-2 {:folder-uid "another-folder" :folder-name "Another folder"}]
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           #"The alerts are located in different folders"
           (sut/check-and-choose-alert-folder-uid
            (examples/make-dashboard-related-alerts [folder-id-1 folder-id-2])))))))

(deftest copy-panel-test
  (testing "existing panel is updated"
    (let [folder-uid        "my-folder"
          panel-description {:uid        "my-panel"
                             :name       "My Panel"
                             :folder-uid folder-uid}
          panel-to-copy     (examples/make-library-element
                             1
                             panel-description)
          existing-panels   [panel-description
                             {:name "My second Panel"}]]
      (with-stub!
        [[api/get-library-panels (api-stub/get-library-panels existing-panels)]
         [api/get-library-element-by-uid (api-stub/get-library-element-by-uid 1 panel-description)]
         [api/update-library-element api-stub/update-library-element]]
        (is (-> examples/grafana-b-instance
                (sut/copy-panel panel-to-copy
                                folder-uid)
                (helper/json->clj)
                (contains? "result")))
        (is (calls-count= 1 api/update-library-element)))))
  (testing "panel is created if it doesn't exist yet"
    (let [folder-uid        "my-folder"
          panel-description {:uid        "my-panel"
                             :name       "My Panel"
                             :folder-uid folder-uid}
          panel-to-copy     (examples/make-library-element
                             1
                             panel-description)
          existing-panels   []]
      (with-stub!
        [[api/get-library-panels (api-stub/get-library-panels existing-panels)]
         [api/get-library-element-by-uid (api-stub/get-library-element-by-uid 1 panel-description)]
         [api/create-library-element api-stub/create-library-element]]
        (is (-> examples/grafana-b-instance
                (sut/copy-panel panel-to-copy
                                folder-uid)
                (helper/json->clj)
                (contains? "result")))
        (is (calls-count= 1 api/create-library-element))))))

(deftest copy-alert-test
  (testing "existing alert is updated"
    (let [folder-uid        "my-folder"
          alert-description {:uid        "my-alert"
                             :title      "My Alert"
                             :folder-uid folder-uid}
          alert-to-copy     (examples/make-alert-rule
                             1
                             alert-description)
          existing-alerts   [alert-description
                             {:title "My second Alert"}]]
      (with-stub!
        [[api/get-all-alert-rules (api-stub/get-all-alert-rules existing-alerts)]
         [api/update-alert-rule api-stub/update-alert-rule]]
        (is (-> examples/grafana-b-instance
                (sut/copy-alert folder-uid
                                alert-to-copy)
                (helper/json->clj)
                (contains? "data")))
        (is (calls-count= 1 api/update-alert-rule)))))
  (testing "alert is created if it doesn't exist yet"
    (let [folder-uid        "my-folder"
          alert-description {:uid        "my-alert"
                             :title      "My Alert"
                             :folder-uid folder-uid}
          alert-to-copy     (examples/make-alert-rule
                             1
                             alert-description)
          existing-alerts   []]
      (with-stub!
        [[api/get-all-alert-rules (api-stub/get-all-alert-rules existing-alerts)]
         [api/create-alert-rule api-stub/create-alert-rule]]
        (is (-> examples/grafana-b-instance
                (sut/copy-alert folder-uid
                                alert-to-copy)
                (helper/json->clj)
                (contains? "data")))
        (is (calls-count= 1 api/create-alert-rule))))))

(deftest copy-dashboard-test
  (testing "dashboard is copied"
    (let [dashboard-uid         "my-dashboard"
          dashboard-description {:uid dashboard-uid}
          folder-uid            "my-folder"
          message               "copy-of-my-dashboard"]
      (with-stub!
        [[api/get-dashboard-by-uid (api-stub/get-dashboard-by-uid dashboard-description)]
         [api/create-update-dashboard (api-stub/create-update-dashboard dashboard-description)]]
        (is (= "success"
               (-> (sut/copy-dashboard examples/grafana-a-instance
                                       examples/grafana-b-instance
                                       dashboard-uid
                                       folder-uid
                                       message)
                   (helper/json->clj)
                   (get "status"))))))))
