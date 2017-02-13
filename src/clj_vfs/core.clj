(ns clj-vfs.core
  (:import
    (org.apache.commons.vfs2 FileSystemOptions FileObject VFS Selectors)
    (org.apache.commons.vfs2.auth StaticUserAuthenticator)
    (org.apache.commons.vfs2.impl DefaultFileSystemConfigBuilder)
    (java.io File InputStream)
    (org.apache.commons.vfs2.provider.ftp FtpFileSystemConfigBuilder)))

(defn connect [^String host ^String uid ^String pwd]
  "save this connection to a global value.
   returns a map with :fs file-manager :host host-string :auth StatusUserAuthenticator :opts FileSystemOptions"
  (let [fs-manager (VFS/getManager)
        auth (StaticUserAuthenticator. nil uid pwd)
        opts (FileSystemOptions.)]
    (doto (DefaultFileSystemConfigBuilder/getInstance)
      (.setUserAuthenticator opts auth))
    (doto (FtpFileSystemConfigBuilder/getInstance)
      (.setUserDirIsRoot opts true))
    {:fs fs-manager :host host :auth auth :opts opts}))

(defn put [{:keys [fs host opts]} ^String file ^String dest]
  "Performs a FTP/SFTP put"
  (let [url (clojure.string/join "/" [host dest])]
    (with-open [remote-fs-obj (-> fs (.resolveFile url opts)) ;remote file object
                local-fs-obj  (-> fs (.resolveFile (-> (File. file) .getAbsolutePath)))] ;local file object
      (-> remote-fs-obj (.copyFrom local-fs-obj (Selectors/SELECT_SELF))))))

(defn get [{:keys [fs host opts]} ^String remote ^String local]
  "Performs a FTP/SFTP get"
  (let [url (clojure.string/join "/" [host remote]) local-file (File. local)]
    (with-open [remote-fs-obj (-> fs (.resolveFile url opts)) ;remote file object
                local-fs-obj  (-> fs (.resolveFile (-> local-file .getAbsolutePath ) ))] ;local file object
      (.mkdirs (.getParentFile local-file)) ;ensure that the parent directories exist
      (-> local-fs-obj (.copyFrom remote-fs-obj (Selectors/SELECT_SELF))))))

(defn mv [{:keys [fs host opts]} ^String f1 ^String f2]
  "Performs a FTP/SFTP move/rename"
  (let [url1 (clojure.string/join "/" [host f1])
        url2 (clojure.string/join "/" [host f2])]
    (with-open [f1-obj (-> fs (.resolveFile url1 opts))  ;remote file object
                f2-obj (-> fs (.resolveFile url2 opts))] ;remote file object
      (-> f1-obj (.moveTo f2-obj)))))

(defn details [ {:keys [fs host opts]} ^String remote]
  "Returns a map with :size on ftp server, :last-modified-time :attributes"
  (let [url (clojure.string/join "/" [host remote])]
    (with-open [f-obj (-> fs (.resolveFile url opts))]
      (let [cnt (.getContent f-obj)]
        {:size (.getSize cnt) :last-modified-time (.getLastModifiedTime cnt) :attributes (.getAttributes cnt)}))))

(defn inputstream [ {:keys [fs host opts]} ^String remote]
  "Returns an inputstream for the file"
  (let [url (clojure.string/join "/" [host remote])
        f-obj (-> fs (.resolveFile url opts))
        in (-> f-obj .getContent .getInputStream)]
    (proxy [InputStream] []
      (available [] (.available in))
      (mark [limit] (.mark in limit))
      (markSupported [] (.markSupported in))
      (read
        ([] (.read in))
        ([bts] (.read in bts))
        ([bts off len] (.read in bts off len)))
      (reset [] (.reset in))
      (skip [n] (.skip in n))
      (close [] ;we need to close obht the f-obj and the inputstream
        (.close in)
        (.close f-obj)))))

(defn mkdirs [{:keys [fs host opts]} ^String remote]
  "Performs a FTP/SFTP mkdir on all parent directories of remote before calling mkdir on the remote directories"
  ;we apply the mkdir function to each subdirectory
  (reduce (fn [parent dir]
            (let [abs_dir (clojure.string/join "/" [parent dir])
                  url (clojure.string/join "/" [host abs_dir])]
              (with-open [remote-fs-obj (-> fs (.resolveFile url opts))]
                (if (not (.exists remote-fs-obj))
                  (.createFolder remote-fs-obj)))
              (clojure.string/join "/" [parent dir])))
          (clojure.string/split remote #"/") ))

(defn exists? [{:keys [fs host opts]} ^String remote]
  "Performs a FTP/SFTP exists"
  (let [url (clojure.string/join "/" [host remote])]
    ;remote file object
    (with-open [remote-fs-obj (-> fs (.resolveFile url opts))]
      (.exists remote-fs-obj))))

(defn rm [{:keys [fs host opts]} ^String remote]
  "Performs a FTP/SFTP rmdir"
  (let [url (clojure.string/join "/" [host remote])]
    ;remote file object
    (with-open [remote-fs-obj (-> fs (.resolveFile url opts))]
      (if (.exists remote-fs-obj) (.delete remote-fs-obj)))))

(defn ls [{:keys [fs host opts]} ^String remote]
  "Performs a FTP/SFTP list on the remote dir"
  (let [url (clojure.string/join "/" [host remote])]
    (with-open [remote-fs-obj (-> fs (.resolveFile url opts))]
      (map (fn [^FileObject x]
             (let [name (-> x (.getName) (.getPathDecoded))]
               (.close x)
               name))
           (.getChildren remote-fs-obj)))))
