(ns audio-port-fetcher.init
  (:require [clojure.java.io         :as io]
            [clojure.string          :as string]
            [clojure.tools.cli       :refer [parse-opts]]
            [taoensso.timbre         :as timbre :refer [trace info warn error fatal]])
  (:gen-class))

(def cli-options
  [["-c" "--config PATH-TO-CONFIG" "Specify the configuration file to use instead of the default."
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
       (string/join \newline)))

(defn- error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

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
       :programs (set (map #(keyword (string/replace % #"(^:)" "")) (rest arguments)))}
      :else ; failed custom validation => exit with usage summary
      {:exit-message (usage summary)})))

(defn exit [status msg]
  (fatal msg)
  (System/exit status)) ;; TODO: disable on repl

(comment

  (let [arg-list ["fetch" "-c" "/tmp/test.edn" "rnrh" ":wv"]]
    (let [{:keys [action options programs exit-message ok?]} (validate-args arg-list)]
      options))

  )
