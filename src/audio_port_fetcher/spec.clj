(ns audio-port-fetcher.spec
  (:require [clojure.spec.alpha :as s]))

(s/def :apf-config/username string?)
(s/def :apf-config/password string?)
(s/def :apf-config/credentials (s/keys :req-un [:apf-config/username
                                                :apf-config/password]))

(s/def :apf-config/pub_title string?)
(s/def :apf-config/ap_uri string?)
(s/def :apf-config/program-data (s/keys :req-un [:apf-config/pub_title]
                                        :op-un [:apf-config/ap_uri]))
(s/def :apf-config/program (s/tuple keyword? :apf-config/program-data))
(s/def :apf-config/programs (s/coll-of :apf-config/program :kind map?))

(s/def ::config (s/keys :req-un [:apf-config/credentials
                                 :apf-config/programs]))
