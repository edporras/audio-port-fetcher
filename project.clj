(defproject audio-port-fetcher "0.1.2"
  :description "Audio Port Program Fetcher"
  :url "https://github.com/edporras/audio-port-fetcher"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/mit-license.php"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [org.clojure/tools.cli "1.0.206"]

                 [clj-http "3.12.1"]
                 [clojure.java-time "0.3.2"]
                 [digest "1.4.10"]

                 [com.taoensso/timbre "5.1.2"]
                 [ring/ring-codec "1.1.3"]
                 [sparkledriver "0.2.4"]]
  :main ^:skip-aot audio-port-fetcher.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
