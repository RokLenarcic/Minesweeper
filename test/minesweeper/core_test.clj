(ns minesweeper.core-test
  (:require [clojure.test :refer :all]
            [minesweeper.core :refer :all]))

(deftest core-utilities
  (testing "vectorize"
    (is (vector? (vectorize '())))
    (is (= 4 (vectorize 4)))
    (is (vector? (vectorize '(5))))
    (is (vector? (get (vectorize '((1 2) 3)) 0))))
  (testing "adjacent-points"
    (is (= [[-1 -1] [-1 0] [-1 1] [0 -1] [0 1] [1 -1] [1 0] [1 1]] (adjacent-points 0 0)))
    )
  (testing "adjacent-points"
    (is (= [[-1 -1] [-1 0] [-1 1] [0 -1] [0 1] [1 -1] [1 0] [1 1]] (adjacent-points 0 0)))
    ))
