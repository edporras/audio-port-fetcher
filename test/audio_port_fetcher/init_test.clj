(ns audio-port-fetcher.init-test
  (:require [clojure.test :refer :all]
            [audio-port-fetcher.init :refer :all]))

(deftest option-parse-unknown-action
  (let [arg-list ["test"]
        {:keys [action]} (validate-args arg-list)]
    (is (nil? action))))

(deftest option-parse-action-fetch
  (let [arg-list ["fetch" "wv"]
        {:keys [action]} (validate-args arg-list)]
    (is (= action "fetch"))))

(deftest option-parse-program-list
  (let [arg-list ["fetch" "-d" "2018-01-15" "rnrh" ":wv"]
        {:keys [programs]} (validate-args arg-list)]
    (is (= programs #{:rnrh :wv}))))

(deftest option-parse-date
  (let [arg-list ["fetch" "-d" "2018-01-15" "wv"]
        {:keys [options]} (validate-args arg-list)]
    (is (= options {:date "2018-01-15"}))))

(deftest option-parse-date-error
  (let [arg-list ["fetch" "-d" "2018-01-44" "wv"]
        {:keys [ok?]} (validate-args arg-list)]
    (is (nil? ok?))))

(deftest option-parse-config
  (let [arg-list ["fetch" "-c" "test/audio_port_fetcher/config-sample.edn" "wv"]
        {:keys [options]} (validate-args arg-list)]
    (is (= options {:config "test/audio_port_fetcher/config-sample.edn"}))))

(deftest option-parse-config-not-found
  (let [arg-list ["fetch" "-c" "test/audio_port_fetcher/not-found" "wv"]
        {:keys [ok?]} (validate-args arg-list)]
    (is (nil? ok?))))
