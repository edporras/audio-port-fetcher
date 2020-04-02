(ns audio-port-fetcher.core-test
  (:require
   [clojure.test            :refer [deftest is]]
   [audio-port-fetcher.core :as sut]))

(deftest program-url-test
  (let [prog {:pub_title "A test"}]
    (is (= "https://www.audioport.org/?op=series&series=A+test"
           (sut/program-url prog)))))

(deftest trim-text-test
  (is (= "This is a title"
         (sut/trim-text "  This is a title   "))))
