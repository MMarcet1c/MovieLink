(ns movielink.genre
  (:require [clojure.string :as str]))

;; Distance lookup table
(def genre-distances
  {"drama"      {"drama" 0, "sci-fi" 2, "crime" 1, "romance" 1, "action" 2, "adventure" 2, "comedy" 1, "horror" 2}
   "sci-fi"     {"drama" 2, "sci-fi" 0, "crime" 2, "romance" 3, "action" 1, "adventure" 1, "comedy" 3, "horror" 2}
   "crime"      {"drama" 1, "sci-fi" 2, "crime" 0, "romance" 2, "action" 1, "adventure" 2, "comedy" 2, "horror" 1}
   "romance"    {"drama" 1, "sci-fi" 3, "crime" 2, "romance" 0, "action" 2, "adventure" 2, "comedy" 1, "horror" 3}
   "action"     {"drama" 2, "sci-fi" 1, "crime" 1, "romance" 2, "action" 0, "adventure" 1, "comedy" 2, "horror" 2}
   "adventure"  {"drama" 2, "sci-fi" 1, "crime" 2, "romance" 2, "action" 1, "adventure" 0, "comedy" 2, "horror" 2}
   "comedy"     {"drama" 1, "sci-fi" 3, "crime" 2, "romance" 1, "action" 2, "adventure" 2, "comedy" 0, "horror" 3}
   "horror"     {"drama" 2, "sci-fi" 2, "crime" 1, "romance" 3, "action" 2, "adventure" 2, "comedy" 3, "horror" 0}})

(defn genre-diff [g1 g2]
  (get-in genre-distances [(str/lower-case g1) (str/lower-case g2)] 3)) ;; default penalty if missing
