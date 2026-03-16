(ns borkdude.text-diff.impl
  "Implementation details. Do not use directly."
  (:require [clojure.string :as str]))

;; Wu et al. 1990 "An O(NP) Sequence Comparison Algorithm"

(defn- wu-diff
  "Returns a sequence of edit ops: integers (keep N lines), :+ (add), :- (delete)."
  [a b]
  (let [n     (count a)
        m     (count b)
        delta (- n m)
        snake (fn [k x]
                (loop [x (int x) y (int (- x k))]
                  (if (and (< x n) (< y m) (= (nth a x) (nth b y)))
                    (recur (inc x) (inc y))
                    x)))
        fp-fn (fn [fp k]
                (let [[dk-1 vk-1] (get fp (dec k) [-1 []])
                      dk-1        (inc (int dk-1))
                      [dk+1 vk+1] (get fp (inc k) [-1 []])
                      x           (max dk-1 (int dk+1))
                      sk          (snake k x)
                      ops         (let [es (if (> dk-1 (int dk+1))
                                               (conj vk-1 :-)
                                               (conj vk+1 :+))]
                                    (if (> sk x)
                                      (conj es (- sk x))
                                      es))]
                  (assoc! fp k [sk ops])))]
    (if (and (zero? n) (zero? m))
      []
      (loop [p 0 fp (transient {})]
        (let [fp (loop [k (- p) fp fp]
                   (if (< k delta)
                     (recur (inc k) (fp-fn fp k))
                     fp))
              fp (loop [k (+ delta p) fp fp]
                   (if (< delta k)
                     (recur (dec k) (fp-fn fp k))
                     fp))
              fp (fp-fn fp delta)]
          (if (= n (first (get fp delta)))
            (rest (nth (get fp delta) 1))
            (recur (inc p) fp)))))))

(defn- swap-ops [edits]
  (mapv (fn [op] (case op :+ :- :- :+ op)) edits))

(defn diff-ops [a b]
  (if (< (count a) (count b))
    (swap-ops (wu-diff b a))
    (wu-diff a b)))

(defn ops->tagged-lines [ops a b]
  (loop [ops ops, ai 0, bi 0, out (transient [])]
    (if-let [op (first ops)]
      (if (integer? op)
        (let [end (+ ai op)]
          (recur (next ops) end (+ bi op)
                 (loop [i ai, out out]
                   (if (< i end)
                     (recur (inc i) (conj! out [:= (nth a i)]))
                     out))))
        (case op
          :- (recur (next ops) (inc ai) bi (conj! out [:- (nth a ai)]))
          :+ (recur (next ops) ai (inc bi) (conj! out [:+ (nth b bi)]))))
      (persistent! out))))

(defn count-tags [tagged from to]
  (loop [i from, adds 0, dels 0]
    (if (< i to)
      (case (first (nth tagged i))
        :+ (recur (inc i) (inc adds) dels)
        :- (recur (inc i) adds (inc dels))
        := (recur (inc i) adds dels))
      [adds dels])))

(defn tagged->ranges [tagged context]
  (let [n (count tagged)]
    (reduce (fn [ranges i]
              (if (= := (first (nth tagged i)))
                ranges
                (let [cs (max 0 (- i context))
                      ce (min n (+ i context 1))
                      [start end] (peek ranges)]
                  (if (and start (<= cs end))
                    (conj (pop ranges) [start (max end ce)])
                    (conj ranges [cs ce])))))
            []
            (range n))))

(defn format-hunk [tagged [start end]]
  (let [[adds-before dels-before] (count-tags tagged 0 start)
        [adds-in dels-in] (count-tags tagged start end)
        total-in  (- end start)
        old-start (inc (- start adds-before))
        new-start (inc (- start dels-before))
        old-count (- total-in adds-in)
        new-count (- total-in dels-in)
        lines     (map (fn [[tag text]]
                         (case tag
                           := (str " " text)
                           :- (str "-" text)
                           :+ (str "+" text)))
                       (subvec tagged start end))]
    (str "@@ -" old-start "," old-count
         " +" new-start "," new-count " @@\n"
         (str/join "\n" lines))))
