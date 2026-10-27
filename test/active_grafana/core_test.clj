(ns active-grafana.core-test
  (:require [active-grafana.core :as sut]
            [active-grafana.helper :as helper]
            [active-grafana.grafana-api :as api]
            [active-grafana.examples :as examples]
            [active-grafana.grafana-api-stub :as api-stub]
            [active-grafana.grafana-api-responses :as api-responses]
            [clojure.string :as str]
            [clojure.test :as t :refer [deftest testing is]]
            [bond.james :refer [with-stub!]]
            [active-grafana.convenient-bond :refer [called-once? not-called?]]))

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

(deftest clean-whitespace-test
  (testing "white space between words is reduced to a single SPACE"
    (let [thing-name "Simple   dashboard    title"]
      (is (= "Simple dashboard title"
             (sut/clean-whitespace thing-name)))))
  (testing "white space before/after the first/last word is trimmed"
    (let [thing-name "   Simple dashboard title     "]
      (is (= "Simple dashboard title"
             (sut/clean-whitespace thing-name))))))

(deftest name-matcher-test
  (testing "matching is case insensitive"
    (let [query-string  (sut/clean-whitespace "SIMPLE DASHBOARD TITLE")
          name-matches? (sut/name-matcher query-string
                                          #(get % "title"))]
      (is (name-matches? {"title" "Simple dashboard title"})))))

(deftest choose-dashboard-metadata-test
  (let [dashboard-title         examples/dashboard-title
        another-dashboard-title examples/another-dashboard-title
        all-dashboards          [{:title dashboard-title}
                                 {:title another-dashboard-title}]]
    (testing "no dashboard is found"
      (with-stub!
        [[api/find-dashboards-by-query
          (api-stub/find-dashboards-by-query [])]
         [api/get-dashboards (api-stub/get-dashboards all-dashboards)]]
        (is (thrown-with-msg? clojure.lang.ExceptionInfo
                              #"No dashboard was found using the search query"
                              (sut/choose-dashboard-metadata examples/grafana-a-instance
                                                             dashboard-title)))
        (is (called-once? api/get-dashboards))))
    (testing "1 dashboard is found (perfect match)"
      (with-stub!
        [[api/find-dashboards-by-query
          (api-stub/find-dashboards-by-query [{:title dashboard-title}])]]
        (is (= dashboard-title
               (get (sut/choose-dashboard-metadata examples/grafana-a-instance
                                                   dashboard-title)
                    "title")))))
    (testing "1 dashboard is found (fuzzy match)"
      (let [search-query "   SIMPLE    dashBoard     title "
            dashboard-title " simple Dashboard   title  "]
        (with-stub!
          [[api/find-dashboards-by-query
            (api-stub/find-dashboards-by-query [{:title dashboard-title}])]]
          (is (= dashboard-title
                 (get (sut/choose-dashboard-metadata examples/grafana-a-instance
                                                     search-query)
                      "title"))))))
    (testing "more than 1 dashboard is found"
      (with-stub!
        [[api/find-dashboards-by-query
          (api-stub/find-dashboards-by-query [{:title dashboard-title}
                                              {:title dashboard-title}])]]
        (is (thrown-with-msg? clojure.lang.ExceptionInfo
                              #"More than one dashboard was found"
                              (sut/choose-dashboard-metadata examples/grafana-a-instance
                                                             dashboard-title)))))))

(deftest choose-folder-uid-test
  (let [folder-title       examples/folder-title
        folder-uid         examples/folder-uid
        another-folder-uid examples/another-folder-uid]
    (testing "no folder is found"
      (with-stub!
        [[api/find-folders-by-query
          [(api-stub/find-folders-by-query [])
           (api-stub/find-folders-by-query [{:title folder-title
                                             :uid   folder-uid}])]]
         [api/create-folder (api-stub/create-folder {:title folder-title})]]
        (is (= folder-uid
               (sut/choose-folder-uid examples/grafana-b-instance
                                      folder-title)))))
    (testing "1 folder is found (perfect match)"
      (with-stub!
        [[api/find-folders-by-query
          (api-stub/find-folders-by-query [{:title folder-title
                                            :uid   folder-uid}])]]
        (is (= folder-uid
               (sut/choose-folder-uid examples/grafana-b-instance
                                      folder-title)))))
    (testing "1 folder is found (fuzzy match)"
      (let [search-query "   My FOLDER   title "
            folder-title "  My  folDer  title  "]
        (with-stub!
          [[api/find-folders-by-query
            (api-stub/find-folders-by-query [{:title folder-title
                                              :uid   folder-uid}])]]
          (is (= folder-uid
                 (sut/choose-folder-uid examples/grafana-b-instance
                                        search-query))))))
    (testing "more than 1 folder is found"
      (with-stub!
        [[api/find-folders-by-query
          (api-stub/find-folders-by-query [{:title folder-title
                                            :uid   folder-uid}
                                           {:title folder-title
                                            :uid   another-folder-uid}])]]
        (is (thrown-with-msg? clojure.lang.ExceptionInfo
                              #"More than one folder was found"
                              (sut/choose-folder-uid examples/grafana-b-instance
                                                     folder-title)))))))

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
  (let [folder-id         {:folder-uid  examples/folder-uid
                           :folder-name examples/folder-title}
        another-folder-id {:folder-uid  examples/another-folder-uid
                           :folder-name examples/another-folder-title}]
    (testing "all panels are located in same folder"
      (is (= (:folder-name folder-id)
             (sut/check-and-choose-panels-folder-title
              (api-responses/make-dashboard-related-panels [folder-id folder-id])))))
    (testing "not all panels are located in same folder"
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           #"The panels are located in different folders"
           (sut/check-and-choose-panels-folder-title
            (api-responses/make-dashboard-related-panels [folder-id another-folder-id])))))))

(deftest check-and-choose-alert-folder-uid-test
  (let [folder-id         {:folder-uid  examples/folder-uid
                           :folder-name examples/folder-title}
        another-folder-id {:folder-uid  examples/another-folder-uid
                           :folder-name examples/another-folder-title}]
    (testing "all alerts are located in same folder"
      (is (= (:folder-uid folder-id)
             (sut/check-and-choose-alert-folder-uid
              (api-responses/make-dashboard-related-alerts [folder-id folder-id])))))
    (testing "not all alerts are located in same folder"
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           #"The alerts are located in different folders"
           (sut/check-and-choose-alert-folder-uid
            (api-responses/make-dashboard-related-alerts [folder-id another-folder-id])))))))

(deftest copy-panel-test
  (testing "existing panel is updated"
    (let [folder-uid        examples/folder-uid
          panel-description {:uid        examples/panel-uid
                             :name       examples/panel-name
                             :folder-uid folder-uid}
          panel-to-copy     (api-responses/make-library-element
                             1
                             panel-description)
          existing-panels   [panel-description
                             {:name examples/another-panel-name}]]
      (with-stub!
        [[api/get-library-panels (api-stub/get-library-panels existing-panels)]
         [api/get-library-element-by-uid (api-stub/get-library-element-by-uid 1 panel-description)]
         [api/update-library-element (api-stub/update-library-element panel-description)]
         [api/create-library-element]]
        (is (-> examples/grafana-b-instance
                (sut/copy-panel panel-to-copy
                                folder-uid)
                (helper/json->clj)
                (contains? "result")))
        (is (not-called? api/create-library-element))
        (is (called-once? api/update-library-element)))))
  (testing "panel is created if it doesn't exist yet"
    (let [folder-uid        examples/folder-uid
          panel-description {:uid        examples/panel-uid
                             :name       examples/panel-name
                             :folder-uid folder-uid}
          panel-to-copy     (api-responses/make-library-element
                             1
                             panel-description)
          existing-panels   []]
      (with-stub!
        [[api/get-library-panels (api-stub/get-library-panels existing-panels)]
         [api/get-library-element-by-uid (api-stub/get-library-element-by-uid 1 panel-description)]
         [api/create-library-element (api-stub/create-library-element panel-description)]
         [api/update-library-element]]
        (is (-> examples/grafana-b-instance
                (sut/copy-panel panel-to-copy
                                folder-uid)
                (helper/json->clj)
                (contains? "result")))
        (is (not-called? api/update-library-element))
        (is (called-once? api/create-library-element))))))

(deftest copy-alert-test
  (testing "existing alert is updated"
    (let [folder-uid        examples/folder-uid
          alert-description {:uid        examples/alert-uid
                             :title      examples/alert-title
                             :folder-uid folder-uid
                             :version    1}
          alert-to-copy     (api-responses/make-alert-rule
                             1
                             alert-description)
          existing-alerts   [alert-description
                             {:title examples/another-alert-title}]]
      (with-stub!
        [[api/get-all-alert-rules (api-stub/get-all-alert-rules existing-alerts)]
         [api/update-alert-rule (api-stub/update-alert-rule alert-description)]
         [api/create-alert-rule]]
        (is (-> examples/grafana-b-instance
                (sut/copy-alert folder-uid
                                alert-to-copy)
                (helper/json->clj)
                (contains? "data")))
        (is (not-called? api/create-alert-rule))
        (is (called-once? api/update-alert-rule)))))
  (testing "alert is created if it doesn't exist yet"
    (let [folder-uid        examples/folder-uid
          alert-description {:uid        examples/alert-uid
                             :title      examples/alert-title
                             :folder-uid folder-uid
                             :version    2}
          alert-to-copy     (api-responses/make-alert-rule
                             1
                             alert-description)
          existing-alerts   []]
      (with-stub!
        [[api/get-all-alert-rules (api-stub/get-all-alert-rules existing-alerts)]
         [api/create-alert-rule (api-stub/create-alert-rule alert-description)]
         [api/update-alert-rule]]
        (is (-> examples/grafana-b-instance
                (sut/copy-alert folder-uid
                                alert-to-copy)
                (helper/json->clj)
                (contains? "data")))
        (is (not-called? api/update-alert-rule))
        (is (called-once? api/create-alert-rule))))))

(deftest source-dashboard->target-dashboard-test
  (testing "The target-dashboard is constructed correctly."
    (let [source-dashboard-uid        examples/dashboard-uid
          source-dashboard-folder-uid "test-folder-uid"
          target-dashboard-folder-uid "target-folder-uid"
          source-dashboard            {:uid        source-dashboard-uid
                                       :folder-uid source-dashboard-folder-uid}
          message                     examples/change-dashboard-message]
      (with-stub!
        [[api/get-dashboard-by-uid (api-stub/get-dashboard-by-uid source-dashboard)]]
        (let [dashboard-request-body (sut/source-dashboard->dashboard-request-body examples/grafana-a-instance
                                                                                   source-dashboard-uid
                                                                                   target-dashboard-folder-uid
                                                                                   message)
              new-dashboard          (get dashboard-request-body "dashboard")]
          (is (contains? dashboard-request-body "dashboard"))
          (is (true? (get dashboard-request-body "overwrite")))
          (is (= message
                 (get dashboard-request-body "message")))
          (is (= target-dashboard-folder-uid
                 (get dashboard-request-body "folderUid")))
          (is (not (nil? new-dashboard)))
          (is (not (contains? new-dashboard "version")))
          (is (not (contains? new-dashboard "id")))
          (is (= source-dashboard-uid
                 (get new-dashboard "uid"))))))))

(deftest copy-dashboard-test
  (testing "dashboard is copied"
    (let [dashboard-uid           examples/dashboard-uid
          folder-uid              "before-update-folder-uid"
          dashboard-before-update {:uid        dashboard-uid
                                   :folder-uid folder-uid}
          updated-folder-uid      "updated-folder-uid"
          updated-dashboard       {:uid        dashboard-uid
                                   :folder-uid updated-folder-uid}
          message                 examples/change-dashboard-message]
      (with-stub!
        [[api/get-dashboard-by-uid (api-stub/get-dashboard-by-uid dashboard-before-update)]
         [api/create-update-dashboard (api-stub/create-update-dashboard updated-dashboard)]]
        (let [copy-dashboard-response (-> (sut/copy-dashboard examples/grafana-a-instance
                                                              examples/grafana-b-instance
                                                              dashboard-uid
                                                              updated-folder-uid
                                                              message)
                                          (helper/json->clj))]
          (is (= "success" (get copy-dashboard-response "status")))
          (is (= dashboard-uid (get copy-dashboard-response "uid")))
          (is (= updated-folder-uid (get copy-dashboard-response "folderUid"))))))))

(deftest convenient-dashboard-copy-test
  (testing "convenient-copy to a folder that existxs and update the panels and alerts it uses"
    (let [folder     examples/folder
          dashboard  (assoc examples/dashboard
                            :folder-uid (:uid folder)
                            :folder-title (:title folder))
          dashboards [dashboard examples/another-dashboard]
          panel      (assoc examples/panel
                            :folder-uid (:uid folder)
                            :folder-name (:title folder))
          panels     [panel examples/another-panel]
          alert      (assoc examples/alert
                            :folder-uid (:uid folder)
                            :dashboard-uid (:uid dashboard))
          alerts     [alert examples/another-alert]]
      (with-stub!
        [[api/get-dashboard-by-uid (api-stub/get-dashboard-by-uid dashboard)]
         [api/find-dashboards-by-query (api-stub/find-dashboards-by-query dashboards)]
         [api/find-folders-by-query (api-stub/find-folders-by-query [folder])]
         [api/create-folder]
         [api/get-folder-by-folder-uid (api-stub/get-folder-by-folder-uid folder)]
         [api/get-library-element-by-uid (api-stub/get-library-element-by-uid 1 panel)]
         [api/get-library-panels (api-stub/get-library-panels panels)]
         [api/update-library-element (api-stub/update-library-element panel)]
         [api/create-library-element]
         [api/get-dashboard-by-uid (api-stub/get-dashboard-by-uid dashboard)]
         [api/create-update-dashboard (api-stub/create-update-dashboard dashboard)]
         [api/get-all-alert-rules (api-stub/get-all-alert-rules alerts)]
         [api/update-alert-rule (api-stub/update-alert-rule alert)]
         [api/create-alert-rule]]
        (is  (= {:source {:url       examples/source-url
                          :dashboard {:title        examples/dashboard-title
                                      :uid          examples/dashboard-uid
                                      :folder-title examples/folder-title
                                      :folder-uid   examples/folder-uid}
                          :panels    {:list         [{:uid  examples/panel-uid
                                                      :name examples/panel-name}]
                                      :folder-title examples/folder-title
                                      :folder-uid   examples/folder-uid}
                          :alerts    {:list         [{:uid   examples/alert-uid
                                                      :title examples/alert-title}]
                                      :folder-title examples/folder-title
                                      :folder-uid   examples/folder-uid}}
                 :target {:url       examples/target-url
                          :dashboard {:title        examples/dashboard-title
                                      :uid          examples/dashboard-uid
                                      :folder-title examples/folder-title
                                      :folder-uid   examples/folder-uid}
                          :panels    {:list         [{:uid  examples/panel-uid
                                                      :name examples/panel-name}]
                                      :folder-title examples/folder-title
                                      :folder-uid   examples/folder-uid}
                          :alerts    {:list         [{:uid   examples/alert-uid
                                                      :title examples/alert-title}]
                                      :folder-title examples/folder-title
                                      :folder-uid   examples/folder-uid}}}
                (sut/convenient-dashboard-copy examples/grafana-a-instance
                                               examples/grafana-b-instance
                                               examples/dashboard-title)))
        (is (not-called? api/create-folder))
        (is (not-called? api/create-library-element))
        (is (not-called? api/create-alert-rule))))))
