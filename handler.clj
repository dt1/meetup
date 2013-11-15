(ns irc.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [clojure.string :as string]
            [clojure.java.io :as io])
  (:import java.net.Socket
           javax.io.IOException))


(declare register-connection)
(declare create-connection)

(defn write-irc-line [irc & s]
  (let [s (clojure.string/join " " s)]
    (binding [*out* (get-in @irc [:connection :out])]
      (println s))))

(defn register-connection
  [irc]
  (let [{:keys [username]} @irc]
    (write-irc-line irc "USERNAME" username)))

(defn parse-line [line]
  (let [[command args] (clojure.string/split line #" " 2)
        args (rest (clojure.string/split line #" "))]
    {:command command 
     :raw line
     :args args}))

(defn write-line [conn line]
  (let [writer (:writer conn)]
    (doto writer
      (.write line)
      (.newLine)
      (.flush))))
 
(defn create-connection [host port]
  (let [socket (Socket. host port)]
    {:socket socket
     :in (io/reader socket)
     :out (io/writer socket)}))

(defn connect
  "Connect to IRC. Connects in another thread and returns a big fat ref of
data about the connection, you, and IRC in general."
  [host port username]
  (let [{:keys [in out]}
        (create-connection host port)
        irc
        (ref {:connection out
              :username username})]
    (.start
     (Thread.
      (fn []
          (register-connection irc)
          (loop [line (read-line)]
            (let [parse (parse-line line)]
              (cond
               (= (:command parse) "JOIN")
                 (write-line line out)
               (= (:command parse) "MESSAGE")
                 (write-line line out)
               (= (:command parse) "WHISPER")
                 (write-line line out)))
            (recur (read-line))))))))

(def port 1802)
(def ip-address "192.168.200.46")



(defroutes app-routes
  (GET "/" [] "Hello World")
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))
