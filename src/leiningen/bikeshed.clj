(ns leiningen.bikeshed
  (:require [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [leiningen.core.eval :as lein]
            [leiningen.core.project :as project]))

(defn help
  "Help text displayed from the command line"
  []
  "Bikesheds your project with totally arbitrary criteria.")

(defn- plugin-dep-vector
  [{:keys [plugins]}]
  (some
   (fn [[plugin-symb :as dep-vector]]
     (when (= plugin-symb 'ertu/lein-bikeshed)
       dep-vector))
   plugins))


(defn- check-namespace-decls-profile
  [project]
  {:dependencies [(plugin-dep-vector project)]})


(defn bikeshed
  "Main function called from Leiningen"
  [project & args]
  (let [[opts args banner]
        (cli/cli
         args
         ["-H" "--help-me" "Show help"
          :flag true :default false]
         ["-v" "--verbose" "Display missing doc strings"
          :flag true :default false]
         ["-m" "--max-line-length" "Max line length"
          :parse-fn #(Integer/parseInt %)]
         ["-fl" "--max-fn-lines" "Max function lines"
          :parse-fn #(Integer/parseInt %)]
         ["-l" "--long-lines"
          "If true, check for trailing blank lines"
          :parse-fn #(Boolean/valueOf %)
          :default true]
         ["-f" "--long-fns"
          "If true, check for long functions"
          :parse-fn #(Boolean/valueOf %)
          :default true]
         ["-w" "--trailing-whitespace"
          "If true, check for trailing whitespace"
          :parse-fn #(Boolean/valueOf %)
          :default false]
         ["-b" "--trailing-blank-lines"
          "If true, check for trailing blank lines"
          :parse-fn #(Boolean/valueOf %)
          :default false]
         ["-r" "--var-redefs"
          "If true, check for redefined var roots in source directories"
          :parse-fn #(Boolean/valueOf %)
          :default false]
         ["-d" "--docstrings"
          "If true, generate a report of docstring coverage"
          :parse-fn #(Boolean/valueOf %)
          :default false]
         ["-n" "--name-collisions"
          "If true, check for function arg names that collide with clojure.core"
          :parse-fn #(Boolean/valueOf %)
          :default false]
         ["-x" "--exclude-profiles" "Comma-separated profile exclusions"
          :default nil
          :parse-fn #(mapv keyword (str/split % #","))])
        lein-opts (:bikeshed project)
        project   (project/merge-profiles project [(check-namespace-decls-profile project)])]
    (if (:help-me opts)
      (println banner)
      (lein/eval-in-project
       project
       `(if (bikeshed.core/bikeshed
             '~project
             {:max-line-length (or (:max-line-length ~opts)
                                   (:max-line-length ~lein-opts))
              :max-fn-lines    (or (:max-fn-lines ~opts)
                                   (:max-fn-lines ~lein-opts))
              :verbose         (:verbose ~opts)
              :check?          #(get (merge ~lein-opts ~opts) % true)})
          (System/exit -1)
          (System/exit 0))
       '(require 'bikeshed.core)))))
