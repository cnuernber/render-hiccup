(defproject cnuernber/render-hiccup "0.1-SNAPSHOT"
  :description "Render some hiccup.  Includes support for vega."
  :url "http://github.com/cnuernber/render-hiccup"
  :license {:name "EPL-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [http-kit "2.3.0"]
                 ;; HTTP middleware stack
                 [ring "1.7.1"]
                 ;; URL<->data
                 [bidi "2.1.4"]
                 ;; Better http parameter handling
                 [metosin/muuntaja "0.6.1"]
                 ;; html generation
                 [hiccup "1.0.5"]
                 [techascent/tech.io "3.8"
                  :exclusions [org.slf4j/slf4j-api]]
                 [metasoarous/oz "1.6.0-alpha5"]
                 ]
  :main render-hiccup.core
  :repl-options {:init-ns render-hiccup.core}
  :profiles {:uberjar {:aot [render-hiccup.core]
                       :uberjar-name "render-hiccup.jar"}})
