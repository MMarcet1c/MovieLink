(defproject movielink "0.1.0-SNAPSHOT"
    :dependencies [[org.clojure/clojure "1.11.1"]
                   [com.github.seancorfield/next.jdbc "1.3.1070"]
                   [org.xerial/sqlite-jdbc "3.42.0.0"]
                   [buddy/buddy-hashers "2.0.167"]
                   [org.clojure/tools.cli "1.0.214"]]
    :main movielink.core)
