(ns audio-port-fetcher.core-test
  (:require [clojure.test :refer :all]
            [audio-port-fetcher.core :refer :all]))

(deftest program-url-test
  (let [prog {:pub_title "A test"}]
    (is (= (program-url prog) "https://www.audioport.org/?op=series&series=A+test"))))

(deftest trim-text-test
  (is (= (trim-text "  This is a title   ") "This is a title")))
