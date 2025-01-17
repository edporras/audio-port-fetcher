(ns audio-port-fetcher.core
  (:require [clojure.java.io         :as io]
            [clojure.string          :as string]
            [clj-http.client         :as client]
            [clj-http.cookies        :as cookies]
            [digest                  :refer [sha-256]]
            [ring.util.codec         :as ring]
            [sparkledriver.element   :as elem]
            [sparkledriver.cookies   :refer [browser-cookies->map]]
            [sparkledriver.browser   :refer [with-browser make-browser fetch! execute-script]]
            [taoensso.timbre         :as timbre :refer [trace info warn error fatal]]
            [audio-port-fetcher.init :refer [validate-args read-config]])
  (:gen-class))

(def audio-port-url "https://www.audioport.org/")
(def default-config-file (io/file (str (System/getProperty "user.home") "/.audioportfetcher")))

(defn- screenshot
  "Take a screenshot and save it to the CWD."
  [browser]
  (-> (elem/screenshot browser)
      io/file
      (io/copy (io/file "screenshot.png"))))

(defn- logged-in?
  "If there's a 'Logout' link, we're logged in."
  [browser]
  (some? (first (elem/find-by-xpath* browser "//a[text()='Logout']"))))

(defn- login
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

(defn- logout
  [browser]
  (when-let [logout (first (elem/find-by-xpath* browser "//a[text()='Logout']"))]
    (elem/click! logout)))

(defn program-url
  "Assembles the program's main page URL based on `:pub_title`."
  [prog]
  (->> (prog :pub_title)
       ring/form-encode
       (str audio-port-url "?op=series&series=")))

(defn trim-text
  "Removes surrounding whitespace found in site text."
  [text]
  (string/trim (string/replace text " " "")))

(def content-disposition-file-pattern (re-pattern "filename=([\\w]+\\.[\\p{Alnum}]+)"))
(defn resp->filename
  "Extract the filename from the response headers. Generates a sha-256 name if filename is not found."
  [headers body]
  (if-let [content-disposition (get headers "Content-Disposition")]
    (last (re-find content-disposition-file-pattern content-disposition))
    ;; name is not in content-disposition. Try to guess extension from
    ;; content-type. I think it's all MP3s but throw otherwise and
    ;; I'll add as I find them.
    (let [content-type (get headers "Content-Type")
          ext          (case content-type
                         "audio/mpeg" ".mp3"
                         (throw (Exception. (str "Unhandled content type. Please report to developer. Headers: " headers))))]
      (warn "No content-disposition found in headers. Using sha-256-based name.")
      (str (sha-256 body) ext))))

(defn- download-file
  "Attemps to download the binary. If status code is 200, it is saved to disk."
  [url cookies]
  (info (str "Downloading audio file from '" url "'"))
  (let [resp (client/get url {:as :byte-array
                              :throw-exceptions false
                              :redirect-strategy :lax
                              :cookies cookies})]
    (if (= 200 (:status resp))
      (let [body     (:body resp)
            filename (resp->filename (:headers resp) body)]
        (info (str "Saving file to '" filename))
        (io/copy body (io/file filename)))
      (fatal (str "Unable to download file from URL '" url "'")))))

(defn- row->audio-file-url
  "Extract the URL from the row pair containing an episode's information."
  [[sub-row-1 _]]
  (-> (elem/find-by-xpath* sub-row-1 "td[contains(@class, 'result_info')]//a")
      first
      (elem/attr "href")))

(defn- row->audio-file-info
  "Extract the producer, date, and length strings from the row pair
  containing an episode's information. Returns a vector in the
  format '[producer date length]'."
  [[_ sub-row-2]]
  (->> (elem/find-by-xpath* sub-row-2 "td[contains(@class, 'boxSeparate')]")
       (drop 1) ;; got four cells, first is empty for spacing
       (mapv #(trim-text (elem/text %)))))

(defn- fetch-program-episode
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

(defn- read-program-title
  "Reads the program's title from the page's div with class `content_title`."
  [browser]
  (-> (elem/find-by-xpath* browser "//div[@id='content_title']")
      first
      elem/text
      (string/replace "Results from Series:" "")
      trim-text))

(defn- rows->episode-data
  "Groups all episode data from a program's page into a map."
  [rows]
  (->> rows
       (partition 2)
       (mapv (fn [row]
               (let [[producer date length] (row->audio-file-info row)]
                 {:url (row->audio-file-url row)
                  :producer producer
                  :date date
                  :length length})))))

(defn- fetch-program-data-using-search
  "Uses `:pub_title` text to do a search."
  [browser pub_title]
  (info (str "Searching for program using text '" pub_title "'"))
  (elem/send-text! (elem/find-by-xpath browser "//input[@name='searchtext']") pub_title)
  (elem/click! (elem/find-by-xpath browser "//input[@name='submit']"))
  (let [rows (elem/find-by-xpath* browser "//tr[contains(@class, 'boxSeparate')]")]
    (if (empty? rows)
      []
      [(rows->episode-data rows) (read-program-title browser)])))

(defn- fetch-program-data
  "Navigates to the program's page and extracts the episode
  listing (first page) + title and returns an array in the format
  `[[ep0_map ep1_map ... epn_map] title]`."
  [browser program]
  (info (str "navigating to URL '" (program-url program) "'"))
  (let [rows (-> (fetch! browser (program-url program))
                 (elem/find-by-xpath* "//tr[contains(@class, 'boxSeparate')]"))]
    (if (empty? rows)
      (do
        (warn "Program page has no results (non-conventional URL?)")
        (fetch-program-data-using-search browser (:pub_title program)))
      (let [title (read-program-title browser)]
        (info (str "Extracted program name as '" title "'"))
        [(rows->episode-data rows) title]))))

(defn- fetch-program-by-date
  "Filters the episode list using the date option and fetches the resulting list."
  [browser episodes fetch-date]
  (info (str "Fetching programs dated " fetch-date))
  (let [matched-eps (filter #(when (= (:date %) fetch-date) %) episodes)]
    (if (empty? matched-eps)
      (error "No episodes found matching date.")
      (doseq [ep matched-eps]
        (fetch-program-episode browser ep)))))

(defn- fetch-program-files
  "Downloads the audio files from the requested programs."
  [browser config-data-map req-programs opts]
  (doseq [program-code req-programs]
    (info "Processing program code" program-code)
    (if-let [prog (config-data-map program-code)]
      (if-let [episodes (-> (fetch-program-data browser prog)
                            first)] ; not using title yet
        (cond
          (opts :date) (fetch-program-by-date browser episodes (:date opts))
          :else
          (let [latest (first episodes)]
            (info (str "Fetching latest program dated " (:date latest)))
            (fetch-program-episode browser latest)))
        (fatal (str "No episodes found!")))
      (warn "Program code '" program-code "' not found in config.")))
  browser)

(defn- fetch-programs
  "Main driving function."
  [req-program-codes opts]
  (let [config-file                    (or (:config opts) default-config-file)
        {:keys [credentials programs]} (read-config config-file)]
    (info (str "Fetching " req-program-codes " from " audio-port-url " using configuration from " config-file))
    (with-browser [browser (make-browser)]
      (-> (login browser credentials)
          (fetch-program-files programs req-program-codes opts)
          logout))))

(defn -main
  [& args]
  (let [{:keys [action options programs exit-message ok?]} (validate-args args)]
    (if-not exit-message
      (case action
        "fetch" (fetch-programs programs options))
      (do ;; error validating args
        (fatal exit-message)
        (System/exit (if ok? 0 1))))))

(comment
  ;; repl testing
  (def config (read-config default-config-file))
  (def browser (make-browser :headless false))

  (-> browser
      (login (config :credentials)))

  (def rows (->> ((config :programs) :rnrh)
                 ;;program-url
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
