(ns active-grafana.cli-test
  (:require [active-grafana.cli :as sut]
            [active-grafana.grafana-api :as api]
            [active-grafana.examples :as examples]
            [active-grafana.grafana-api-stub :as api-stub]
            [clojure.test :as t :refer [deftest testing is]]
            [bond.james :refer [with-stub!]]))

(deftest copy-dashboard-command-test
  (testing "Copy a dashboard."
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
        (is (= {:source {:url       examples/source-url
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
               (sut/-main "copy"
                          "--source-url" examples/source-url
                          "--target-url" examples/target-url
                          "--source-token" examples/source-token
                          "--target-token" examples/target-token
                          "dashboard" examples/dashboard-title)))))))

(deftest copy-alert-command-test
  (testing "Copy an alert rule."
    (let [folder examples/folder
          alert  (assoc examples/alert
                        :folder-uid (:uid folder))
          alerts [alert examples/another-alert]]
      (with-stub!
        [[api/get-alert-rule-by-uid (api-stub/get-alert-rule-by-uid alert)]
         [api/find-folders-by-query (api-stub/find-folders-by-query [folder])]
         [api/create-folder]
         [api/get-folder-by-folder-uid (api-stub/get-folder-by-folder-uid folder)]
         [api/get-all-alert-rules (api-stub/get-all-alert-rules alerts)]
         [api/update-alert-rule (api-stub/update-alert-rule alert)]
         [api/create-alert-rule]]
        (is (= {:source {:url   examples/source-url
                         :alert {:title        examples/alert-title
                                 :uid          examples/alert-uid
                                 :folder-uid   examples/folder-uid
                                 :folder-title examples/folder-title}}
                :target {:url   examples/target-url
                         :alert {:title        examples/alert-title
                                 :uid          examples/alert-uid
                                 :folder-uid   examples/folder-uid
                                 :folder-title examples/folder-title}}}
               (sut/-main "copy"
                          "--source-url" examples/source-url
                          "--target-url" examples/target-url
                          "--source-token" examples/source-token
                          "--target-token" examples/target-token
                          "alert" examples/alert-title)))))))
