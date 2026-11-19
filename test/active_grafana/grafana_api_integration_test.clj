(ns active-grafana.grafana-api-integration-test
  (:require [active-grafana.grafana-api :as sut]
            [active-grafana.settings :as settings]
            [active-grafana.examples :as examples]
            [active-grafana.helper :as helper]
            [babashka.http-client  :as http]
            [babashka.process :refer [shell]]
            [clojure.test :as t :refer [deftest testing is]]
            [clojure.string :as str]))

(def admin-user "admin")
(def admin-password "admin-password")

(def grafana-a-instance
  (settings/->Grafana-Instance "http://localhost:3000"
                               nil))

(def grafana-b-instance
  (settings/->Grafana-Instance "http://localhost:3001"
                               nil))

(def ^:dynamic *running-grafana-instances* [])

(defn grafana-container-name [grafana-instance]
  (str "BABASHKA-GRAFANA-INSTANCE__"
       (-> (settings/grafana-instance-url grafana-instance)
           (str/replace #"[:]" "-")
           (str/replace #"[:\/\/]" "-"))))

(defn gf-security-env [var val]
  (str "GF_SECURITY_" (str/upper-case var) "=" val))

(defn grafana-env-admin-user [user-name]
  ;; https://grafana.com/docs/grafana/latest/setup-grafana/configure-grafana/#admin_user
  (gf-security-env "admin_user" user-name))

(defn grafana-env-admin-password [password]
  ;; https://grafana.com/docs/grafana/latest/setup-grafana/configure-grafana/#admin_password
  (gf-security-env "admin_password" password))

(defn wait-for-grafana! [attempt grafana-instance]
  (if (> attempt 25)
    (throw (ex-info "Grafana did not become available"
                    {:grafana-instance grafana-instance}))
    (try
      (http/get (str (settings/grafana-instance-url grafana-instance)
                     "/api/health")
                {})
      (println "Grafana is running.")
      (catch Exception _
        (Thread/sleep 200)
        (wait-for-grafana! (inc attempt)
                           grafana-instance)))))

(defn create-service-account! [grafana-instance]
  (let [response (http/post (str (settings/grafana-instance-url
                                  grafana-instance)
                                 "/api/serviceaccounts")
                            {:basic-auth [admin-user admin-password]
                             :headers    {"Content-Type" "application/json"}
                             :body       (helper/clj->json
                                          {:name "api-admin"
                                           :role "Admin"})})

        service-account-id (-> response
                               (helper/json->clj)
                               (get "id"))]
    (println "Created service account: " service-account-id)
    service-account-id))

(defn create-api-token! [grafana-instance service-account-id]
  (let [response (http/post (str (settings/grafana-instance-url
                                  grafana-instance)
                                 "/api/serviceaccounts/"
                                 service-account-id
                                 "/tokens")
                            {:basic-auth [admin-user admin-password]
                             :headers    {"Content-Type" "application/json"}
                             :body       (helper/clj->json
                                          {:name "api-token"})})

        token (-> response
                  (helper/json->clj)
                  (get "key"))]
    (println "Grafana API token:")
    (println token)
    token))

(defn setup-grafana-instance! [grafana-instance]
  (println "Setting up the following grafana instance: ")
  (println (pr-str grafana-instance))
  (let [docker-command ["docker" "run" "-d"
                        "--name" (grafana-container-name grafana-instance)
                        "-p" (str (settings/grafana-instance-port grafana-instance) ":3000")
                        "-e" (grafana-env-admin-user admin-user)
                        "-e" (grafana-env-admin-password admin-password)
                        ;; TODO: pin the grafana image version in the next line
                        "grafana/grafana"]
        #_["docker" "run" "-d" "--name" (grafana-container-name grafana-instance) "-p" (str (settings/grafana-instance-port grafana-instance) ":3000") "-e" "GF_SECURITY_ADMIN_USER=admin" "-e" "GF_SECURITY_ADMIN_PASSWORD=admin-password" grafana/grafana]
        _ (println "Using the following docker command: " (str/join " " docker-command))
        result         (apply shell docker-command)]
    (if (zero? (:exit result))
      (do (wait-for-grafana! 1 grafana-instance)
          (let [service-account-id       (create-service-account! grafana-instance)
                api-token                (create-api-token! grafana-instance service-account-id)
                running-grafana-instance (settings/grafana-instance-token grafana-instance api-token)]
            (println "Grafana instance is running: " (pr-str running-grafana-instance))
            running-grafana-instance))
      (throw (ex-info "Failed to start the grafana instance"
                      {:exit-code      (:exit result)
                       :result         result
                       :docker-command docker-command})))))

(defn teardown-grafana-instance! [grafana-instance]
  (println "Tearing down the following grafana instance: ")
  (println (pr-str grafana-instance))
  (let [docker-remove-command ["docker" "rm" "-f" (grafana-container-name grafana-instance)]
        remove-result         (apply shell docker-remove-command)]
    (if (zero? (:exit remove-result))
      (println "Grafana instace was teared down successfully")
      (throw (ex-info "Failed to stop the grafana instance"
                      {:exit                  (:exit remove-result)
                       :remove-result         remove-result
                       :docker-remove-command docker-remove-command})))))

(defn with-grafana-instances! [& grafana-instances]
  (fn grafana-instance-fixture [f]
    (println "Setting up grafana instances using docker....")
    (let [running-grafana-instances (mapv setup-grafana-instance! grafana-instances)]
      (println "All grafana instances were set up.")
      (binding [*running-grafana-instances* running-grafana-instances]
        (f)))
    (println "Tearing down docker grafana instances....")
    (run! teardown-grafana-instance! grafana-instances)
    (println "All grafana instances were teared down.")))

(defn with-data! [& {:keys [grafana-instance
                            folders
                            dashboards
                            panels
                            alerts]}]
  (fn data-fixture [f]
    #_(http/post (sut/make-request-url (settings/grafana-instance-url grafana-instance)
                                       "TODO: endpoint url")
                 (sut/make-request-map :token (settings/grafana-instance-token grafana-instance)
                                       :body "TODO: request body, should be somehting from example"))
    (println "Setup data on the grafana instance using its http api.")
    (println "TODO: Setup the data...")
    (f)
    ;; NOTE: we do not tear down anything here, since the data in the docker
    ;; grafana instance is not persisted, since we do not use a docker volume
    ))

(t/use-fixtures :once
  (with-grafana-instances! grafana-a-instance)
  (with-data!
    :grafana-instance grafana-a-instance
    :folders examples/folders
    :dashboards examples/dashboards
    :panels examples/panels
    :alerts examples/alerts))

(deftest grafana-instance-running?-test
  (testing "The first grafana instance is running"
    (is (= "http://localhost:3000" (settings/grafana-instance-url (first *running-grafana-instances*))))
    (let [token-probe (settings/grafana-instance-token (first *running-grafana-instances*))]
      (is (string? token-probe))
      (is (not (str/blank? token-probe))))))

(deftest get-all-alert-rules-test
  (let [probe (sut/get-all-alert-rules
               (settings/grafana-instance-url (first *running-grafana-instances*))
               (settings/grafana-instance-token (first *running-grafana-instances*)))]
    (testing "TODO"
      (is (= "DONE" "TODO")))))

(deftest get-alert-rule-by-uid-test
  (let [probe (sut/get-alert-rule-by-uid
               (settings/grafana-instance-url (first *running-grafana-instances*))
               (settings/grafana-instance-token (first *running-grafana-instances*))
               "irrelevant-uid")]
    (testing "TODO"
      (is (= "DONE" "TODO")))))

(deftest create-alert-rule-test
  (let [probe (sut/create-alert-rule
               (settings/grafana-instance-url (first *running-grafana-instances*))
               (settings/grafana-instance-token (first *running-grafana-instances*))
               "{}")]
    (testing "TODO"
      (is (= "DONE" "TODO")))))

(deftest update-alert-rule-test
  (let [probe (sut/update-alert-rule
               (settings/grafana-instance-url (first *running-grafana-instances*))
               (settings/grafana-instance-token (first *running-grafana-instances*))
               "irrelevant-uid"
               "{}")]
    (testing "TODO"
      (is (= "DONE" "TODO")))))

(deftest get-dashboards-test
  (let [probe (sut/get-dashboards
               (settings/grafana-instance-url (first *running-grafana-instances*))
               (settings/grafana-instance-token (first *running-grafana-instances*)))]
    (testing "TODO"
      (is (= "DONE" "TODO")))))

(deftest get-dashboard-by-uid-test
  (let [probe (sut/get-dashboard-by-uid
               (settings/grafana-instance-url (first *running-grafana-instances*))
               (settings/grafana-instance-token (first *running-grafana-instances*))
               "irrelevant-uid")]
    (testing "TODO"
      (is (= "DONE" "TODO")))))

(deftest create-update-dashboard-test
  (let [probe (sut/create-update-dashboard
               (settings/grafana-instance-url (first *running-grafana-instances*))
               (settings/grafana-instance-token (first *running-grafana-instances*))
               "{}")]
    (testing "TODO"
      (is (= "DONE" "TODO")))))

(deftest find-dashboards-by-query-test
  (let [query "query"
        probe (sut/find-dashboards-by-query
               (settings/grafana-instance-url (first *running-grafana-instances*))
               (settings/grafana-instance-token (first *running-grafana-instances*))
               query)]
    (testing "TODO"
      (is (= "DONE" "TODO")))))

(deftest get-folders-test
  (let [probe (sut/get-folders
               (settings/grafana-instance-url (first *running-grafana-instances*))
               (settings/grafana-instance-token (first *running-grafana-instances*)))]
    (testing "TODO"
      (is (= "DONE" "TODO")))))

(deftest get-folder-by-folder-uid-test
  (let [probe (sut/get-folder-by-folder-uid
               (settings/grafana-instance-url (first *running-grafana-instances*))
               (settings/grafana-instance-token (first *running-grafana-instances*))
               "irrelevant-uid")]
    (testing "TODO"
      (is (= "DONE" "TODO")))))

(deftest find-folders-by-query-test
  (let [query "query"
        probe (sut/find-folders-by-query
               (settings/grafana-instance-url (first *running-grafana-instances*))
               (settings/grafana-instance-token (first *running-grafana-instances*))
               query)]
    (testing "TODO"
      (is (= "DONE" "TODO")))))

(deftest create-folder-test
  (let [probe (sut/create-folder
               (settings/grafana-instance-url (first *running-grafana-instances*))
               (settings/grafana-instance-token (first *running-grafana-instances*))
               "title")]
    (testing "TODO"
      (is (= "DONE" "TODO")))))

(deftest get-library-panels-test
  (let [probe (sut/get-library-panels
               (settings/grafana-instance-url (first *running-grafana-instances*))
               (settings/grafana-instance-token (first *running-grafana-instances*)))]
    (testing "TODO"
      (is (= "DONE" "TODO")))))

(deftest get-library-element-by-uid-test
  (let [probe (sut/get-library-element-by-uid
               (settings/grafana-instance-url (first *running-grafana-instances*))
               (settings/grafana-instance-token (first *running-grafana-instances*))
               "irrelevant-uid")]
    (testing "TODO"
      (is (= "DONE" "TODO")))))

(deftest create-library-element-test
  (let [probe (sut/create-library-element
               (settings/grafana-instance-url (first *running-grafana-instances*))
               (settings/grafana-instance-token (first *running-grafana-instances*))
               "{}")]
    (testing "TODO"
      (is (= "DONE" "TODO")))))

(deftest update-library-element-test
  (let [probe (sut/update-library-element
               (settings/grafana-instance-url (first *running-grafana-instances*))
               (settings/grafana-instance-token (first *running-grafana-instances*))
               "irrelevant-uid"
               "{}")]
    (testing "TODO"
      (is (= "DONE" "TODO")))))
