(ns audio-port-fetcher.init
  (:require [audio-port-fetcher.spec :as spec]
            [clojure.java.io         :as io]
            [clojure.spec.alpha      :as s]
            [clojure.string          :as str]
            [clojure.tools.cli       :refer [parse-opts]]
            [clojure.edn             :as edn]
            [clj-time.format         :as time])
  (:gen-class))

(defn read-config
  "Opens the edn configuration and checks that the read object looks valid."
  [file]
  (let [config-data (with-open [r (io/reader file)]
                      (edn/read (java.io.PushbackReader. r)))]
    (assert (s/valid? ::spec/config config-data)
            (s/explain-str ::spec/config config-data))
    config-data))

(def date-fmt (time/formatter :date))
(def cli-options
  [["-d" "--date YYYY-MM-DD" "Specify the episode date to fetch."
    :validate [#(try (time/parse date-fmt %) (catch Exception _ false)) "Invalid date."]]
   ["-c" "--config PATH-TO-CONFIG" "Specify the configuration file to use instead of the default."
    :validate [#(.exists (io/file %)) "File not found."]]
   ["-h" "--help"]])

(defn- usage [options-summary]
  (->> ["AudioPort.org Program Fetcher"
        ""
        "Usage: audio-port-fetcher action [options] <program codes>"
        ""
        "Actions:"
        "  fetch    Fetch a program's episode audio file."
        ""
        "Options:"
        options-summary]
       (str/join \newline)))

(defn- error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with a error message, and optional ok status), or a map
  indicating the action the program should take and the options provided."
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}
      ;; custom validation on arguments
      (and (> (count arguments) 1)
           (#{"fetch"} (first arguments)))
      {:action (first arguments)
       :options options
       :programs (set (map #(keyword (str/replace % #"(^:)" "")) (rest arguments)))}
      :else ; failed custom validation => exit with usage summary
      {:exit-message (usage summary)})))

(comment

  (let [arg-list ["fetch" "-d" "2018-01-15" "rnrh" ":wv"]]
    (let [{:keys [action options programs exit-message ok?]} (validate-args arg-list)]
      options))

  )
