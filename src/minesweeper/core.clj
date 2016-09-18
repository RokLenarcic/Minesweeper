(ns minesweeper.core
  (:require [clojure.set :refer [intersection difference]]
            [clojure.walk :refer :all]))

(defn adjacent-points [row col]
  "Generate eight points around given point"
  (filter #(not= [row col] %) (for [r (range (dec row) (+ 2 row)) c (range (dec col) (+ 2 col))] [r c])))

(defn vectorize
  "Turns nested sequences into nested vectors"
  [data] (if (seq? data) (into [] (map vectorize data)) data))

(defrecord Block [row col val])

(defprotocol IField
  (find-around [this pred point])
  (mark [this point])
  (group [this point])
  (reveal [this point from-field])
  (won? [this])
  (dead? [this]))

(defn block-type-pred [val]
  (condp #(%1 %2) val
    keyword? #(= val (:val %))
    number? #(= val (:val %))
    ifn? #(val (:val %))))

(defn unrevealed-field [field]
  "Creates a copy of a field with all the values replaced with :?"
  (update field :blocks (partial postwalk #(if (instance? Block %) (assoc % :val :?) %))))

(defn- compare-groups [g1 g2]
  (let [m1 (:mines g1)
        m2 (:mines g2)
        common (intersection (:members g1) (:members g2))]
    (if (zero? (count common))
      []
      (let [rest1 (difference (:members g1) common) rest2 (difference (:members g2) common)
            max-common-mines (min (count common) m1 m2)
            min-common-mines (max (- m1 (count rest1)) (- m2 (count rest2)))]
        [{:members rest1 :min-mines (- m1 max-common-mines) :max-mines (- m1 min-common-mines)}
         {:members rest2 :min-mines (- m2 max-common-mines) :max-mines (- m2 min-common-mines)}
         {:members common :min-mines min-common-mines :max-mines max-common-mines}]
        ))))

(defn ask-complex [groups]
  (let [seq-of-tails (fn [coll] (take-while not-empty (iterate rest coll)))
        map-pairs (fn [f coll] (for [i (seq-of-tails coll) j (seq-of-tails (rest i))] (f (first i) (first j))))]
    (let [group-info (filter #(not-empty (:members %)) (apply concat (map-pairs compare-groups groups)))]
      {:reveal (mapcat :members (filter #(<= (:max-mines %) 0) group-info))
       :mark (mapcat :members (filter #(>= (:min-mines %) (count (:members %))) group-info))})))

(defn ask [revealed]
  (let [groups (filter #(not-empty (:members %)) (map #(group revealed %) (flatten (:blocks revealed))))
        easy-picks {:reveal (map :origin (filter #(zero? (:mines %)) groups))
                    :mark (mapcat :members (filter #(<= (count (:members %)) (:mines %)) groups))}]
    (if (or (not-empty (:reveal easy-picks)) (not-empty (:mark easy-picks)))
      easy-picks (ask-complex groups))
    ))

(defrecord Field [rows cols mines blocks]
  IField
  (mark [this [row col]] (update-in this [:blocks row col :val] #(get {:? :!, :! :?} % %)))
  (find-around [_ pred {:keys [row col]}] (filter pred (map #(get-in blocks %) (adjacent-points row col))))
  (group [this {:keys [row col] :as p}]
    (if-let [num-mines (when (number? (get-in blocks [row col :val])) (get-in blocks [row col :val]))]
      {:mines   (max 0 (- num-mines (count (find-around this (block-type-pred :!) p))))
       :members (into #{} (find-around this (block-type-pred :?) p)) :origin p}
      {:mines 9 :members (list) :origin p}))
  (dead? [_] (some #(some (block-type-pred :x) %) blocks))
  (won? [_] (= mines (reduce + (map #(count (filter (block-type-pred #{:? :!}) %)) blocks))))
  (reveal [this point from-field]
    (loop [[point & tail] (list (get-in blocks point))
           revealed this]
      (if (and point (not= (:val point) :!))
        (let [copy (fn [f {:keys [row col]}] (assoc-in f [:blocks row col :val] (get-in from-field [:blocks row col :val])))
              r (copy revealed point)
              {:keys [mines members]} (group r point)]
          (if (zero? mines)
            (recur (concat tail members) (reduce copy r members))
            (recur tail r)))
        revealed))))

(defn generate-minefield [rows cols num-mines]
  (let [minefields (reductions conj #{} (repeatedly #(vector (rand-int rows) (rand-int cols))))
        minefield (first (filter #(= (count %) num-mines) minefields))
        adjacency (for [row (range rows) col (range cols)]
                    (Block. row col (if (minefield [row col]) :x (count (keep minefield (adjacent-points row col))))))]
    (->Field rows cols num-mines (vectorize (partition cols adjacency)))))