(defproject goal-tracker "0.1.0-SNAPSHOT"
  :description "Personal goal tracking program"
  :url "https://github.com/lsund/goal-tracker"
  :min-lein-version "2.7.0"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.946"]
                 [org.clojure/core.async  "0.4.474"]
                 [org.clojure/tools.namespace "0.2.11"]
                 [org.clojure/java.jdbc "0.7.6"]
                 [org.postgresql/postgresql "42.2.2"]
                 [http-kit "2.2.0"]
                 [ring/ring-defaults "0.3.0"]
                 [compojure "1.6.1"]
                 [reagent "0.8.0"]
                 [hiccup "1.0.5"]
                 [com.taoensso/timbre "4.10.0"]
                 [io.aviso/pretty "0.1.34"]
                 [clj-time/clj-time "0.15.0"]
                 [medley "1.0.0"]
                 [slingshot "0.12.2"]]
  :plugins [[lein-figwheel "0.5.15"]
            [lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]
            [io.aviso/pretty "0.1.34"]]
  :source-paths ["src/clj" "src/cljs"]
  :ring {:handler goal-tracker.core/new-handler}
  :main goal-tracker.main
  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src/cljs"]
                :figwheel {:on-jsload "goal-tracker.core/on-js-reload"
                           :open-urls ["http://localhost:3449/index.html"]}
                :compiler {:main goal-tracker.core
                           :asset-path "js/compiled/out"
                           :output-to "resources/public/js/compiled/goal-tracker.js"
                           :output-dir "resources/public/js/compiled/out"
                           :source-map-timestamp true
                           :preloads [devtools.preload]}}
               {:id           "min"
                :source-paths ["src/cljs"]
                :compiler     {:output-to "resources/public/js/compiled/goal-tracker.js"
                               :main goal-tracker.core
                               :optimizations :advanced
                               :pretty-print  false}}]}
  :figwheel {:css-dirs ["resources/public/css"]}
  :repl-options {:init-ns user}
  :profiles {:dev {:dependencies  [[binaryage/devtools "0.9.9"]
                                   [figwheel-sidecar "0.5.15"]
                                   [com.cemerick/piggieback "0.2.2"]]
                   :source-paths  ["src/clj" "src/cljs" "dev"]
                   :repl-options  {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                   :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                                     :target-path]}})
