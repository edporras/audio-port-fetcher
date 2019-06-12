(ns audio-port-fetcher.core
  (:require [clojure.java.io         :as io]
            [clojure.string          :as string]
            [clojure.edn             :as edn]
            [clj-http.client         :as client]
            [clj-http.cookies        :as cookies]
            [digest                  :refer [sha-256]]
            [ring.util.codec         :as ring]
            [sparkledriver.element   :as elem]
            [sparkledriver.cookies   :refer [browser-cookies->map]]
            [sparkledriver.browser   :refer [with-browser make-browser fetch! execute-script]]
            [taoensso.timbre         :as timbre :refer [trace info warn error fatal]]
            [audio-port-fetcher.init :refer [validate-args exit]])
  (:gen-class))

(def audio-port-url "https://www.audioport.org/")
(def default-config-file (io/file (str (System/getProperty "user.home") "/.audioportfetcher")))

(defn read-config
  "Opens the edn configuration."
  [file]
  (with-open [r (io/reader file)]
    (edn/read (java.io.PushbackReader. r))))

(defn- screenshot
  "Take a screenshot and save it to the CWD."
  [browser]
  (-> (elem/screenshot browser)
      io/file
      (io/copy (io/file "screenshot.png"))))

(defn logged-in?
  "If there's a 'Logout' link, we're logged in."
  [browser]
  (some? (first (elem/find-by-xpath* browser "//a[text()='Logout']"))))

(defn login
  "Performs the login process and re-fetches the main page."
  [browser credentials]
  ;; go to the login page
  (-> (fetch! browser audio-port-url)
      (elem/find-by-xpath* "//a[text()='Login']")
      first
      elem/click!)
  (Thread/sleep 1000)
  ;; fill the credentials and go back to the main page
  (elem/send-text! (elem/find-by-id browser "email") (credentials :username))
  (elem/send-text! (elem/find-by-id browser "password") (credentials :password))
  (elem/click! (elem/find-by-xpath browser "//input[@name='Login']"))
  (info "logged in. Redirecting to Home.")
  (elem/click! (first (elem/find-by-xpath* browser "//a[text()='Home Page']")))
  browser)

(defn logout
  [browser]
  (when-let [logout (first (elem/find-by-xpath* browser "//a[text()='Logout']"))]
    (elem/click! logout)))

(defn program-url
  "Assembles the URL program's main page. If a custom path is configured
  with the `:ap_uri` value, use that. Otherwise, base it on the
  `:pub_title`."
  [prog]
  (->> (if-let [prog-url-substr (prog :ap_uri)]
         prog-url-substr
         (prog :pub_title))
       ring/form-encode
       (str audio-port-url "?op=series&series=")))

(defn download-file
  "Attemps to download the binary. If status code is 200, it is saved to disk."
  [url cookies]
  (info (str "Downloading audio file from '" url "'"))
  (let [req (client/get url {:as :byte-array
                             :throw-exceptions false
                             :redirect-strategy :lax
                             :cookies cookies})]
    (if (= 200 (:status req))
      (let [body     (:body req)
            filename (str (sha-256 body) ".mp3")] ;; TODO: mime-type for extension
        (info (str "Saving file to '" filename "'"))
        (io/copy body (io/file filename)))
      (fatal (str "Unable to download file from URL '" url "'")))))

(defn row->audio-file-url
  "Extract the URL from the row pair containing an episode's information."
  [[sub-row-1 _]]
  (-> (elem/find-by-xpath* sub-row-1 "td[contains(@class, 'result_info')]//a")
      first
      (elem/attr "href")))

(defn row->audio-file-info
  "Extract the producer, date, and length strings from the row pair
  containing an episode's information. Returns a vector in the
  format '[producer date length]'."
  [[_ sub-row-2]]
  (->> (elem/find-by-xpath* sub-row-2 "td[contains(@class, 'boxSeparate')]")
       (drop 1) ;; got four cells, first is empty for spacing
       (mapv #(-> (elem/text %)
                  (string/replace " " "")
                  string/trim))))

(defn fetch-program-episode
  "Fetches the episode in the given data."
  [browser episode-data]
  (fetch! browser (:url episode-data))
  ;; scroll the window to the bottom so the MP3 link is visible
  (execute-script browser "window.scrollTo(0,document.body.scrollHeight);")
  (-> browser
      (elem/find-by-xpath* "//td[@class='boxContentInfo']//img[@src='/resources/images/icon-download-on.gif']/parent::a")
      first
      (elem/attr "href")
      (download-file (browser-cookies->map browser))))

(defn read-program-title
  "Reads the program's title from the page's div with class `content_title`."
  [browser]
  (-> (elem/find-by-xpath* browser "//div[@id='content_title']")
      first
      elem/text
      (string/replace "Results from Series:" "")
      (string/replace " " "")
      (string/trim)))

(defn fetch-program-data
  "Navigates to the program's page and extracts the episode
  listing (first page) + title and returns an array in the format
  `[title [ep0_map ep1_map ... epn_map]]`."
  [browser program-url]
  (info (str "navigating to URL '" program-url "'"))
  (let [rows  (-> (fetch! browser program-url)
                  (elem/find-by-xpath* "//tr[contains(@class, 'boxSeparate')]"))
        title (read-program-title browser)]
    (info (str "Extracted program name as '" title "'"))
    [title (->> rows
                (partition 2)
                (mapv (fn [row]
                        (let [[producer date length] (row->audio-file-info row)]
                          {:url (row->audio-file-url row)
                           :producer producer
                           :date date
                           :length length}))))]))

(defn fetch-program-files
  "Downloads the audio files from the requested programs."
  [browser config-data-map req-programs opts]
  (let [prog (config-data-map (first req-programs)) ;; TODO: process more than one request
        url  (program-url prog)]
    (info (str "Requested program '" (:pub_title prog) "'"))
    (let [[title episodes] (fetch-program-data browser url)]
      (if-not (empty? episodes)
        (do
          (info (str "Found " (count episodes)) " episodes available.")
          (cond
            (opts :date) nil ;; soon
            :else
            (do
              (info (str "Fetching latest program"))
              (fetch-program-episode browser (first episodes)))))
        (fatal (str "No episodes found!"))))))

(defn fetch-programs
  "Main driving function."
  [req-programs opts]
  (let [config-file  (or (:config opts) default-config-file)
        config       (read-config config-file)
        program-list (config :programs)]
    (info (str "Fetching " req-programs " from " audio-port-url " using configuration from " config-file))
    (with-browser [browser (make-browser)]
      (-> (login browser (config :credentials))
          (fetch-program-files program-list req-programs opts)))))

(defn -main
  [& args]
  (let [{:keys [action options programs exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (case action
        "fetch" (fetch-programs programs options)))))

(comment
  ;; repl testing
  (def config (read-config default-config-file))
  (def browser (make-browser :headless false))

  (-> browser
      (login (config :credentials)))

  (def rows (->> ((config :programs) :rnrh)
                 program-url
                 (fetch-program-data browser)))
  (count rows)
  (def episodes (last rows))

  (fetch-program-episode browser (first episodes))

  (-> browser
      (elem/find-by-xpath* "//tr[@class='boxSeparateA']//a[@class='result_infoA']")
      first
      elem/text)

  (fetch-program-files browser (config :programs) [:rnrh] {})

  (-main "fetch rnrh")

  )
