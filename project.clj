(defproject audio-port-fetcher "0.1.1"
  :description "Audio Port Program Fetcher"
  :url "https://github.com/edporras/audio-port-fetcher"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/mit-license.php"}
  :dependencies [
                 [org.clojure/clojure "1.10.0"]
                 [org.clojure/tools.cli "0.4.2"]
                 [clj-http "3.10.0"]
                 [clj-time "0.15.0"]
                 [digest "1.4.9"]

                 [com.taoensso/timbre "4.10.0"]
                 [ring/ring-codec "1.1.2"]
                 [sparkledriver "0.2.4"]
                 ]
  :main ^:skip-aot audio-port-fetcher.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
