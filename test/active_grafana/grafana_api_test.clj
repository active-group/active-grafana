(ns active-grafana.grafana-api-test
  (:require [active-grafana.grafana-api :as sut]
            [babashka.http-client  :as http]
            [active-grafana.helper :as helper]
            [bond.james :refer [with-stub!]]
            [active-grafana.convenient-bond :refer [called-once?]]
            [clojure.test :as t :refer [deftest testing is]]
            [clojure.string :as str]))

(deftest bearer-token?-test
  (testing "`Bearer irrelevant-token-example` is a bearer token"
    (let [valid-bearer-token "Bearer irrelevant-token-example"]
      (is (sut/bearer-token? valid-bearer-token))))
  (testing "`irrelevant-token-example` is not a bearer token"
    (let [invalid-bearer-token "irrelevant-token-example"]
      (is (not (sut/bearer-token? invalid-bearer-token)))))
  (testing "`` is not a bearer token"
    (let [invalid-bearer-token ""]
      (is (not (sut/bearer-token? invalid-bearer-token))))))

(deftest make-bearer-token-test
  (testing "`(make-bearer-token irrelevant-token-example) ` yields a valid bearer token."
    (let [token "irrelevant-token-example"]
      (is (sut/bearer-token? (sut/make-bearer-token token)))))
  (testing "`(make-bearer-token \"\") ` throws an assertion error."
    (let [token ""]
      (is (thrown? java.lang.AssertionError (sut/make-bearer-token token)))))
  (testing "`(make-bearer-token nil) ` throws an assertion error."
    (let [token nil]
      (is (thrown? java.lang.AssertionError (sut/make-bearer-token token))))))

(deftest make-query-client-headers-test
  (testing "Client headers are constructed without throwing, when client header data is valid."
    (is (= {"Authorization" "Bearer irrelevant-token-example"
            "Accept" "application/json"}
           (sut/make-query-client-headers {:token "irrelevant-token-example"}))))
  (testing "AssertionError is thrown when client header data is invalid"
    (is (thrown? java.lang.AssertionError (sut/make-query-client-headers nil)))))

(deftest make-mutation-client-headers-test
  (testing "Client headers are constructed without throwing, when client header data is valid."
    (is (= {"Authorization" "Bearer irrelevant-token-example"
            "Accept" "application/json"
            "Content-Type" "application/json"
            "X-Disable-Provenance" "true"}
           (sut/make-mutation-client-headers {:token "irrelevant-token-example"
                                              :disable-provenance? true})))
    (is (= {"Authorization" "Bearer irrelevant-token-example"
            "Accept" "application/json"
            "Content-Type" "application/json"}
           (sut/make-mutation-client-headers {:token "irrelevant-token-example"}))))
  (testing "AssertionError is thrown when client header data is invalid"
    (is (thrown? java.lang.AssertionError (sut/make-mutation-client-headers nil)))))

(deftest jsonizy-body-test
  (testing "A map jsonizes"
    (is (helper/json-string? (sut/jsonize-body {:test "test"}))))
  (testing "An integer jsonizes"
    (is (helper/json-string? (sut/jsonize-body 1))))
  (testing "A double jsonizes"
    (is (helper/json-string? (sut/jsonize-body 1.2))))
  (testing "A rational jsonizes"
    (is (helper/json-string? (sut/jsonize-body 1/2))))
  (testing "A vector jsonizes"
    (is (helper/json-string? (sut/jsonize-body ["test"]))))
  (testing "A list jsonizes"
    (is (helper/json-string? (sut/jsonize-body '("test")))))
  (testing "A set jsonizes"
    (is (helper/json-string? (sut/jsonize-body #{"test"})))))

(deftest make-request-map-test
  (testing "Produces a sound request map for babashka.http-client."
    (let [{:keys [headers body]
           :as req-map}  (sut/make-request-map {:token "irrelevant-token-example"
                                                :body (helper/clj->json {:test "test"})})]
      (is (contains? req-map :client))
      (is (contains? req-map :headers))
      (is (contains? headers "Authorization"))
      (is (sut/bearer-token? (get headers "Authorization")))
      (is (contains? headers "Accept"))
      (is (= "application/json" (get headers "Accept")))
      (is (contains? headers "Content-Type"))
      (is (= "application/json" (get headers "Content-Type")))
      (is (contains? req-map :body))
      (is (helper/json-string? body))
      (is (= "{\"test\":\"test\"}" body)))))

(deftest sanitize-base-url
  (testing "A ending slash from the base-url is removed."
    (let [endpoint-url "http://base.url/"
          probe (sut/sanitize-base-url endpoint-url)]
      (is (not (str/ends-with? probe "/")))))
  (testing "More than two subsequent slash is sanitized to one slash"
    (let [endpoint-url "http:///base.url"
          probe (sut/sanitize-base-url endpoint-url)]
      (is (not (re-find #"[/]{3,}" probe))))))

(deftest sanitize-endpoint-url
  (testing "A starting slash from the endpoint-url is removed."
    (let [endpoint-url "/test/second/third"
          probe (sut/sanitize-endpoint-url endpoint-url)]
      (is (not (str/starts-with? probe "/")))))
  (testing "A ending slash from the endpoint-url is removed."
    (let [endpoint-url "test/second/third/"
          probe (sut/sanitize-endpoint-url endpoint-url)]
      (is (not (str/ends-with? probe "/")))))
  (testing "More than one subsequent slash is sanitized to one slash"
    (let [endpoint-url "test/////second//third"
          probe (sut/sanitize-endpoint-url endpoint-url)]
      (is (not (re-find #"[/]{2,}" probe))))))

(deftest make-request-url-test
  (testing "Valid https base-url, endpoint and one more fragment"
    (is (= "http://localhost:3000/api/dashboards/uid/PARAM-DASHBOARD-UID"
           (sut/make-request-url "http://localhost:3000" "/api/dashboards/uid" "PARAM-DASHBOARD-UID"))))
  (testing "Valid http base-url, endpoint and one more fragment"
    (is (= "https://localhost:3000/api/dashboards/uid/PARAM-DASHBOARD-UID"
           (sut/make-request-url "https://localhost:3000" "/api/dashboards/uid" "PARAM-DASHBOARD-UID"))))
  (testing "Numbers are valid fragments"
    (is (= "http://localhost:3000/api/dashboards/uid/1/2/3.2"
           (sut/make-request-url "http://localhost:3000" "/api/dashboards/uid" 1 2 3.2))))
  (testing "query parameters beginning with ? work correcty"
    (is (= "http://localhost:3000/api/search?query=test&type=dash-db"
           (sut/make-request-url "http://localhost:3000" "/api/search" "?" "query" "test" "type" "dash-db"))))
  (testing "No endpoint is not allowed"
    (is (thrown? java.lang.AssertionError (sut/make-request-url "http://localhost:3000" ""))))
  (testing "Don't forget the double slash"
    (is (thrown? java.lang.AssertionError
                 (sut/make-request-url "http:localhost:3000" "/api/dashboards/uid"))))
  (testing "Don't forget the schema!"
    (is (thrown? java.lang.AssertionError
                 (sut/make-request-url "localhost:3000" "/api/dashboards/uid"))))
  (testing "Collections are not valid fragments"
    (is (thrown? java.lang.AssertionError
                 (sut/make-request-url "http://localhost:3000" "/api/dashboards/uid" {})))
    (is (thrown? java.lang.AssertionError
                 (sut/make-request-url "http://localhost:3000" "/api/dashboards/uid" #{})))
    (is (thrown? java.lang.AssertionError
                 (sut/make-request-url "http://localhost:3000" "/api/dashboards/uid" [])))
    (is (thrown? java.lang.AssertionError
                 (sut/make-request-url "http://localhost:3000" "/api/dashboards/uid" '())))))

(defn http-stub [url req]
  {:url    url
   :req    req})

(defn req-without-body? [http-stub-response]
  (not (contains? (:req http-stub-response) :body)))

(defn req-with-body? [http-stub-response]
  (contains? (:req http-stub-response) :body))

(defn url=? [url http-stub-response]
  (= url (:url http-stub-response)))

(deftest get-all-alert-rules-test
  (with-stub!
    [[http/get http-stub]]
    (let [probe (sut/get-all-alert-rules "http://localhost:3000" "irrelevant-token")]
      (testing "http/get is called once."
        (is (called-once? http/get)))
      (testing "It uses the correct url."
        (is (url=? "http://localhost:3000/api/v1/provisioning/alert-rules"
                   probe)))
      (testing "Request does not have a body"
        (is (req-without-body? probe))))))

(deftest get-alert-rule-by-uid-test
  (with-stub!
    [[http/get http-stub]]
    (let [probe (sut/get-alert-rule-by-uid "http://localhost:3000" "irrelevant-token" "irrelevant-uid")]
      (testing "http/get is called once."
        (is (called-once? http/get)))
      (testing "It uses the correct url."
        (is (url=? "http://localhost:3000/api/v1/provisioning/alert-rules/irrelevant-uid"
                   probe)))
      (testing "Request does not have a body"
        (is (req-without-body? probe))))))

(deftest create-alert-rule-test
  (with-stub!
    [[http/post http-stub]]
    (let [probe (sut/create-alert-rule "http://localhost:3000" "irrelevant-token" "{}")]
      (testing "http/post is called once."
        (is (called-once? http/post)))
      (testing "It uses the correct url."
        (is (url=? "http://localhost:3000/api/v1/provisioning/alert-rules"
                   probe)))
      (testing "Request does not have a body"
        (is (req-with-body? probe))))))

(deftest update-alert-rule-test
  (with-stub!
    [[http/put http-stub]]
    (let [probe (sut/update-alert-rule "http://localhost:3000" "irrelevant-token" "irrelevant-uid" "{}")]
      (testing "http/post is called once."
        (is (called-once? http/put)))
      (testing "It uses the correct url."
        (is (url=? "http://localhost:3000/api/v1/provisioning/alert-rules/irrelevant-uid"
                   probe)))
      (testing "Request does not have a body"
        (is (req-with-body? probe))))))

(deftest get-dashboards-test
  (with-stub!
    [[http/get http-stub]]
    (let [probe (sut/get-dashboards "http://localhost:3000" "irrelevant-token")]
      (testing "http/get is called once."
        (is (called-once? http/get)))
      (testing "It uses the correct url."
        (is (url=? "http://localhost:3000/api/search?type=dash-db"
                   probe)))
      (testing "Request does not have a body"
        (is (req-without-body? probe))))))

(deftest get-dashboard-by-uid-test
  (with-stub!
    [[http/get http-stub]]
    (let [probe (sut/get-dashboard-by-uid "http://localhost:3000" "irrelevant-token" "irrelevant-uid")]
      (testing "http/get is called once."
        (is (called-once? http/get)))
      (testing "It uses the correct url."
        (is (url=? "http://localhost:3000/api/dashboards/uid/irrelevant-uid"
                   probe)))
      (testing "Request does not have a body"
        (is (req-without-body? probe))))))

(deftest create-update-dashboard-test
  (with-stub!
    [[http/post http-stub]]
    (let [probe (sut/create-update-dashboard "http://localhost:3000" "irrelevant-token" "{}")]
      (testing "http/post is called once."
        (is (called-once? http/post)))
      (testing "It uses the correct url."
        (is (url=? "http://localhost:3000/api/dashboards/db"
                   probe)))
      (testing "Request does not have a body"
        (is (req-with-body? probe))))))

(deftest find-dashboards-by-query-test
  (with-stub!
    [[http/get http-stub]]
    (let [query "query"
          probe (sut/find-dashboards-by-query "http://localhost:3000" "irrelevant-token" query)]
      (testing "http/get is called once."
        (is (called-once? http/get)))
      (testing "It uses the correct url."
        (is (url=? (str "http://localhost:3000/api/search?query=" query "&type=dash-db")
                   probe)))
      (testing "Request does not have a body"
        (is (req-without-body? probe))))))

(deftest get-folders-test
  (with-stub!
    [[http/get http-stub]]
    (let [probe (sut/get-folders "http://localhost:3000" "irrelevant-token")]
      (testing "http/get is called once."
        (is (called-once? http/get)))
      (testing "It uses the correct url."
        (is (url=? "http://localhost:3000/api/folders"
                   probe)))
      (testing "Request does not have a body"
        (is (req-without-body? probe))))))

(deftest get-folder-by-folder-uid-test
  (with-stub!
    [[http/get http-stub]]
    (let [probe (sut/get-folder-by-folder-uid "http://localhost:3000" "irrelevant-token" "irrelevant-uid")]
      (testing "http/get is called once."
        (is (called-once? http/get)))
      (testing "It uses the correct url."
        (is (url=? "http://localhost:3000/api/folders/irrelevant-uid"
                   probe)))
      (testing "Request does not have a body"
        (is (req-without-body? probe))))))

(deftest find-folders-by-query-test
  (with-stub!
    [[http/get http-stub]]
    (let [query "query"
          probe (sut/find-folders-by-query "http://localhost:3000" "irrelevant-token" query)]
      (testing "http/get is called once."
        (is (called-once? http/get)))
      (testing "It uses the correct url."
        (is (url=? (str "http://localhost:3000/api/search?query=" query "&type=dash-folder")
                   probe)))
      (testing "Request does not have a body"
        (is (req-without-body? probe))))))

(deftest create-folder-test
  (with-stub!
    [[http/post http-stub]]
    (let [probe (sut/create-folder "http://localhost:3000" "irrelevant-token" "title")]
      (testing "http/post is called once."
        (is (called-once? http/post)))
      (testing "It uses the correct url."
        (is (url=? "http://localhost:3000/api/folders"
                   probe)))
      (testing "Request does not have a body"
        (is (req-with-body? probe))))))

(deftest get-library-panels-test
  (with-stub!
    [[http/get http-stub]]
    (let [probe (sut/get-library-panels "http://localhost:3000" "irrelevant-token")]
      (testing "http/get is called once."
        (is (called-once? http/get)))
      (testing "It uses the correct url."
        (is (url=? "http://localhost:3000/api/library-elements?kind=1"
                   probe)))
      (testing "Request does not have a body"
        (is (req-without-body? probe))))))

(deftest get-library-element-by-uid-test
  (with-stub!
    [[http/get http-stub]]
    (let [probe (sut/get-library-element-by-uid "http://localhost:3000" "irrelevant-token" "irrelevant-uid")]
      (testing "http/get is called once."
        (is (called-once? http/get)))
      (testing "It uses the correct url."
        (is (url=? "http://localhost:3000/api/library-elements/irrelevant-uid"
                   probe)))
      (testing "Request does not have a body"
        (is (req-without-body? probe))))))

(deftest create-library-element-test
  (with-stub!
    [[http/post http-stub]]
    (let [probe (sut/create-library-element "http://localhost:3000" "irrelevant-token" "{}")]
      (testing "http/post is called once."
        (is (called-once? http/post)))
      (testing "It uses the correct url."
        (is (url=? "http://localhost:3000/api/library-elements"
                   probe)))
      (testing "Request does not have a body"
        (is (req-with-body? probe))))))

(deftest update-library-element-test
  (with-stub!
    [[http/patch http-stub]]
    (let [probe (sut/update-library-element "http://localhost:3000" "irrelevant-token" "irrelevant-uid" "{}")]
      (testing "http/post is called once."
        (is (called-once? http/patch)))
      (testing "It uses the correct url."
        (is (url=? "http://localhost:3000/api/library-elements/irrelevant-uid"
                   probe)))
      (testing "Request does not have a body"
        (is (req-with-body? probe))))))
