(ns minesweeper.ui
  (:require [seesaw.core :refer :all]
            [seesaw.bind :as b]
            [seesaw.mouse :as mouse]
            [seesaw.keymap :as keys]
            [seesaw.graphics :as g]
            [seesaw.font :refer [font]]
            [minesweeper.core :as core])
  (:gen-class)
  (:import (java.awt Font Graphics2D)
           (java.awt.event MouseEvent)))

(defn- disable-canvas [frame] ((user-data (select frame [:#canvas]))))

(defn- get-color [val] (get {:? :grey 0 :black 1 :white 2 :white 3 :white 4 :white 5 :white 6 :white 7 :white 8 :white :x :red :! :orange} val))

(def ^:private choose-font
  (memoize (fn [font-render-ctx block-w block-h]
             (let [desired-height (* 0.8 block-h)
                   to-font-map (fn [^Font fnt]
                                 {:font  fnt
                                  :x-off (unchecked-divide-int (- block-w (.getWidth (.getMaxCharBounds fnt font-render-ctx))) 2)
                                  :y-off (- 0 (+ block-h (.getY (.getMaxCharBounds fnt font-render-ctx))))})]
               (->> (iterate #(+ 0.5 %) 8)
                    (map #(font :size % :name :monospaced))
                    (filter #(> (* -1 (.getY (.getMaxCharBounds ^Font % font-render-ctx))) desired-height))
                    (first)
                    (to-font-map))))))

(defn- block-renderer [c ^Graphics2D g rows cols]
  (let [block-w (/ (width c) cols)
        block-h (/ (height c) rows)
        font-info (choose-font (.getFontRenderContext g) block-w block-h)]
    (fn [row col val]
      (g/draw g
              (g/rect (* col block-w) (* row block-h) block-w block-h)
              (g/style :foreground :black :background (get-color val))
              (g/string-shape (+ (:x-off font-info) (* col block-w)) (+ (:y-off font-info) (* (inc row) block-h)) (str (last (seq (str val)))))
              (g/style :foreground :black :font (:font font-info))))))

(defn- get-event-coords [field ^MouseEvent e]
  [(int (/ (* (.getY e) (:rows field)) (height e))) (int (/ (* (.getX e) (:cols field)) (width e)))])

(defn create-window [rows cols mines]
  (let [game (atom {:mines 0, :cols 0, :rows 0, :id 0})
        minefield (atom (core/generate-minefield 10 9 8))
        revealed (atom (core/unrevealed-field @minefield))
        settings-changer (fn [key] #(let [val (read-string (value %))] (when (integer? val) (swap! game assoc key val))))
        paint (fn [c g]
                (let [{:keys [rows cols blocks]} @revealed
                      renderer (block-renderer c g rows cols)]
                  (doseq [row (range rows)
                          col (range cols)]
                    (renderer row col (get-in blocks [row col :val])))))
        click-action (fn [e] (swap! revealed
                                    #(if (= (mouse/button e) :left)
                                      (core/reveal %1 (get-event-coords %1 %2) @minefield)
                                      (core/mark %1 (get-event-coords %1 %2))) e))
        menus (menubar :items [(button :action (action :name "New" :handler (fn [_] (swap! game #(update % :id inc)))))
                               "Rows" (text :text rows :listen [:focus (settings-changer :rows) :action (settings-changer :rows)])
                               "Cols" (text :text cols :listen [:focus (settings-changer :cols) :action (settings-changer :cols)])
                               "Mines" (text :text mines :listen [:focus (settings-changer :mines) :action (settings-changer :mines)])])
        frame (frame :title "Minesweeper" :width 0 :height 0
                     :content (border-panel :center (canvas :id :canvas :paint paint)) :menubar menus :on-close :exit)]
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
                    [(max 300 (* (:cols f) 20)) :by (max 300 (* (:rows f) 20))]))
                (b/property frame :size))
              (b/b-swap! minefield (fn [_ g] (core/generate-minefield (g :rows) (g :cols) (min (* (g :rows) (g :cols)) (g :mines)))))
              (b/b-do [x] (config! (select frame [:#canvas]) :user-data (listen (select frame [:#canvas]) :mouse-released click-action)) (repaint! frame))))
    (b/bind minefield (b/b-swap! revealed (fn [_ f] (core/unrevealed-field f))))
    (b/bind revealed
            (b/notify-soon)
            (b/tee
              (b/b-do [x] (repaint! frame))
              (b/bind (b/filter #(core/dead? %)) (b/b-do [_] (alert frame "KABOOM!") (disable-canvas frame)))
              (b/bind (b/filter #(core/won? %)) (b/b-do [_] (alert frame "WINRAR!") (disable-canvas frame)))))
    (reset! game {:cols cols, :rows rows, :mines mines :id 0})
    frame))

(defn -main
  "Run app."
  [& args]
  (show! (create-window 10 10 10)))
