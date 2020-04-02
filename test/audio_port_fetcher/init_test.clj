(ns audio-port-fetcher.init-test
  (:require
   [clojure.test            :refer [deftest is]]
   [audio-port-fetcher.init :as sut]))

(deftest option-parse-unknown-action
  (let [arg-list ["test"]
        rslt     (sut/validate-args arg-list)]
    (is (nil? (:action rslt)))))

(deftest option-parse-action-fetch
  (let [arg-list ["fetch" "wv"]
        rslt     (sut/validate-args arg-list)]
    (is (= "fetch"
           (:action rslt)))))

(deftest option-parse-program-list
  (let [arg-list ["fetch" "-d" "2018-01-15" "rnrh" ":wv"]
        rslt     (sut/validate-args arg-list)]
    (is (= (:programs rslt)
           #{:rnrh :wv}))))

(deftest option-parse-date
  (let [arg-list ["fetch" "-d" "2018-01-15" "wv"]
        rslt     (sut/validate-args arg-list)]
    (is (= {:date "2018-01-15"}
           (:options rslt)))))

(deftest option-parse-date-error
  (let [arg-list ["fetch" "-d" "2018-01-44" "wv"]
        rslt     (sut/validate-args arg-list)]
    (is (nil? (:ok? rslt)))))

(deftest option-parse-config
  (let [arg-list ["fetch" "-c" "test/audio_port_fetcher/config-sample.edn" "wv"]
        rslt     (sut/validate-args arg-list)]
    (is (= {:config "test/audio_port_fetcher/config-sample.edn"}
           (:options rslt)))))

(deftest option-parse-config-not-found
  (let [arg-list ["fetch" "-c" "test/audio_port_fetcher/not-found" "wv"]
        rslt     (sut/validate-args arg-list)]
    (is (nil? (:ok? rslt)))))
