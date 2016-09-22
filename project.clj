(defproject minesweeper "0.1.0"
  :description "Minesweeper clone with a solver"
  :url "https://github.com/RokLenarcic/Minesweeper"
  :license {:name "GNU General Public License v3.0"
            :url "http://www.gnu.org/licenses/gpl-3.0.txt"
            :year 2016
            :key "gpl-3.0"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [seesaw "1.4.5"]]
  :plugins [[lein-license "0.1.6"]]
  :main ^:skip-aot minesweeper.ui
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
