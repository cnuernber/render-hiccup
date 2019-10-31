(ns render-hiccup.core
  (:require [clojure.data.json :as json]
            [org.httpkit.server :as http-kit]
            [org.httpkit.client :as http-client]
            [ring.util.response :as response]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [bidi.ring :as bidi-ring]
            [muuntaja.middleware :refer [wrap-format]]
            [cognitect.transit :as transit]
            [clojure.tools.logging :as log]
            [clojure.pprint :as pp]
            [oz.core :as oz])
  (:gen-class))


(defn- params-helper
  [{:keys [body-params]}])

(defn wrap-catch-errors
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Throwable e
        (log/error e
                   (json/write-str
                    {:uri (:uri request)
                     :params   (with-out-str (pp/pprint
                                              (:body-params request)))
                     :referer (get-in request [:headers "referer"])
                     :description (.getMessage e)
                     :origin (get (:headers request) "origin")
                     :trace (with-out-str (pp/pprint (.getStackTrace e)))}))
        {:status 500
         :headers {}
         :body {:message (.getMessage e)}}))))


(defn wrap-request-logging
  [next-handler]
  (fn [req]
    (let [uri (:uri req)
          start (System/nanoTime)
          resp (next-handler req)]
      (let [response-time-sec (/ (- (System/nanoTime) start) 1e9)]
        (log/info (pr-str {:response-time-sec response-time-sec
                           :uri uri
                           :remote-addr (:remote-addr req)
                           :referer (get-in req [:headers "referer"])
                           :params (params-helper req)
                           :response-code (:status resp)})))
      resp)))


(defn handler
  [routes]
  (-> (bidi-ring/make-handler routes)
      (wrap-request-logging)
      (wrap-catch-errors)
      (wrap-format)
      (wrap-cookies)
      (wrap-params)
      (wrap-multipart-params)
      (wrap-resource "public")
      (wrap-content-type)))


(comment
    (defn play-data [& names]
  (for [n names
        i (range 20)]
    {:time i :item n :quantity (+ (Math/pow (* i (count n)) 0.8) (rand-int (count n)))}))

  (def line-plot
  {:data {:values (play-data "monkey" "slipper" "broom")}
   :encoding {:x {:field "time"}
              :y {:field "quantity"}
              :color {:field "item" :type "nominal"}}
   :mark "line"})

  (def json-plot (-> (json/write-str line-plot)
                     (json/read-str :key-fn keyword)))

  (oz/view! [:vega-lite json-plot])
  )


(def params (atom nil))

(defn read-transit-string
  [^String transit-str]
  (let [in-stream (java.io.ByteArrayInputStream. (.getBytes transit-str))
        reader (transit/reader in-stream :json)]
    (transit/read reader)))
(defn upload
  [request]
  (let [view-params (get-in request [:query-params "view"])]
    (reset! params view-params)
    (oz/view! (read-transit-string view-params))
    (response/status {} 200)))


(def routes ["/upload" #'upload])


(defonce server-stop-fn* (atom nil))


(defn run-server
  [& [port]]
  (swap! server-stop-fn*
         (fn [existing]
           (when existing
             (existing))
           (let [port (or port 7337)]
             (log/info (format "Started server on port %s" port))
             (http-kit/run-server (handler routes)
                                  {:port port}))))
  :ok)


(defn -main [& args]
  (run-server (when-let [first-arg (first args)]
                (try
                  (Long/parseLong first-arg)
                  (catch Throwable e nil)))))
