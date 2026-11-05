(ns active-grafana.grafana-api-test
  (:require [active-grafana.grafana-api :as sut]
            [active-grafana.helper :as helper]
            [clojure.test :as t :refer [deftest testing is]]))

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

(deftest make-request-url-test
  (testing "Valid https base-url, endpoint and one more fragment"
    (is (= "http://localhost:3000/api/dashboards/uid/PARAM-DASHBOARD-UID"
           (sut/make-request-url "http://localhost:3000" "/api/dashboards/uid" "PARAM-DASHBOARD-UID"))))
  (testing "Valid http base-url, endpoint and one more fragment"
    (is (= "https://localhost:3000/api/dashboards/uid/PARAM-DASHBOARD-UID"
           (sut/make-request-url "https://localhost:3000" "/api/dashboards/uid" "PARAM-DASHBOARD-UID"))))
  (testing "Numbers are valid fragments" (is (= "http://localhost:3000/api/dashboards/uid/1/2/3.2" (sut/make-request-url "http://localhost:3000" "/api/dashboards/uid" 1 2 3.2))))
  (testing "No endpoint is not allowed" (is (thrown? java.lang.AssertionError (sut/make-request-url "http://localhost:3000" ""))))
  (testing "Don't forget the double slash" (is (thrown? java.lang.AssertionError (sut/make-request-url "http:localhost:3000" "/api/dashboards/uid"))))
  (testing "Don't forget the schema!" (is (thrown? java.lang.AssertionError (sut/make-request-url "localhost:3000" "/api/dashboards/uid"))))
  (testing "Collections are not valid fragments"
    (is (thrown? java.lang.AssertionError (sut/make-request-url "http://localhost:3000" "/api/dashboards/uid" {})))
    (is (thrown? java.lang.AssertionError (sut/make-request-url "http://localhost:3000" "/api/dashboards/uid" #{})))
    (is (thrown? java.lang.AssertionError (sut/make-request-url "http://localhost:3000" "/api/dashboards/uid" [])))
    (is (thrown? java.lang.AssertionError (sut/make-request-url "http://localhost:3000" "/api/dashboards/uid" '())))))
