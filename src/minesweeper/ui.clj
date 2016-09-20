(ns minesweeper.ui
  (:require [seesaw.core :refer :all]
            [seesaw.border :as border]
            [seesaw.bind :as b]
            [seesaw.mouse :as mouse]
            [seesaw.keymap :as keys]
            [minesweeper.core :as core])
  (:gen-class))

(defn- create-grid [{:keys [rows cols]} action]
  (let [buttons (for [row (range rows) col (range cols)]
                  (button :user-data [row col] :text "?" :background :grey :class :block :margin 0 :border (border/line-border)))
        handlers (doall (map #(listen % :mouse-released action) buttons))]
    (grid-panel :id :field :hgap 2 :vgap 2 :rows rows :columns cols :user-data handlers :items buttons)))

(defn- disable-btns [frame] (doseq [disbler (user-data (select frame [:#field]))] (disbler)))

(defn create-window [rows cols mines]
  (let [game (atom {:mines 0, :cols 0, :rows 0, :id 0})
        minefield (atom (core/generate-minefield 10 9 8))
        revealed (atom (core/unrevealed-field @minefield))
        settings-changer (fn [key] #(let [val (read-string (value %))] (when (integer? val) (swap! game assoc key val))))
        click-action (fn [e] (swap! revealed
                                    #(if (= (mouse/button e) :left)
                                      (core/reveal %1 %2 @minefield)
                                      (core/mark %1 %2)) (user-data e)))
        menus (menubar :items [(button :action (action :name "New" :handler (fn [_] (swap! game #(update % :id inc)))))
                               "Rows" (text :text rows :listen [:focus (settings-changer :rows) :action (settings-changer :rows)])
                               "Cols" (text :text cols :listen [:focus (settings-changer :cols) :action (settings-changer :cols)])
                               "Mines" (text :text mines :listen [:focus (settings-changer :mines) :action (settings-changer :mines)])])
        frame (frame :title "Minesweeper" :width 0 :height 0 :content "HA" :menubar menus :on-close :exit)]
    (keys/map-key frame "menu A" (fn [e] (when-let [x (core/ask @revealed)]
                                           (let [r @revealed m @minefield
                                                 marked (reduce #(assoc-in %1 [:blocks (:row %2) (:col %2) :val] :!) r (:mark x))
                                                 expanded (reduce #(core/reveal %1 [(:row %2) (:col %2)] m) marked (:reveal x))]
                                             (reset! revealed expanded)))))
    (b/bind game
            (b/notify-soon)
            (b/tee
              (b/bind
                (b/transform
                  (fn [f]
                    [(max 300 (* (:cols f) 30)) :by (max 300 (* (:rows f) 30))]))
                (b/property frame :size))
              (b/bind (b/transform #(create-grid % click-action)) (b/property frame :content))
              (b/b-swap! minefield (fn [_ g] (core/generate-minefield (g :rows) (g :cols) (min (* (g :rows) (g :cols)) (g :mines)))))))
    (b/bind minefield (b/b-swap! revealed (fn [_ f] (core/unrevealed-field f))))
    (b/bind revealed
            (b/notify-soon)
            (b/tee
              (b/b-do [x] (doseq [btn (select frame [:.block])]
                            (let [new-val (:val (get-in (:blocks x) (config btn :user-data)))]
                              (config! btn :text (str (last (seq (str new-val)))))
                              (config! btn :background (get {:? :grey 0 :black 1 :white 2 :white 3 :white 4 :white 5 :white 6 :white 7 :white 8 :white :x :red :! :orange} new-val))))
                      (repaint! frame))
              (b/bind (b/filter #(core/dead? %)) (b/b-do [_] (alert frame "KABOOM!") (disable-btns frame)))
              (b/bind (b/filter #(core/won? %)) (b/b-do [_] (alert frame "WINRAR!") (disable-btns frame)))))
    (reset! game {:cols cols, :rows rows, :mines mines :id 0})
    frame))

(defn -main
  "Run app."
  [& args]
  (show! (create-window 10 10 10)))
