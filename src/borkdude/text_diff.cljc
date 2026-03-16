(ns borkdude.text-diff
  "Line-level text diffing with unified diff output.
  Based on Wu et al. 1990 \"An O(NP) Sequence Comparison Algorithm\"."
  (:require [borkdude.text-diff.impl :as impl]
            [clojure.string :as str]))

(defn diff
  "Diffs two strings line-by-line. Returns a vector of tagged lines:
  [[:= \"unchanged\"] [:- \"deleted\"] [:+ \"added\"] ...].
  This is the core data model — use it to build any output format."
  [old-text new-text]
  (let [a (str/split old-text #"\r?\n" -1)
        b (str/split new-text #"\r?\n" -1)]
    (impl/ops->tagged-lines (impl/diff-ops a b) a b)))

(defn unified-diff
  "Produces a unified diff string from two texts.
  Options:
    :context  - number of context lines (default 3)
    :filename - filename for the header (default \"a\")"
  ([old-text new-text]
   (unified-diff old-text new-text nil))
  ([old-text new-text {:keys [context filename] :or {context 3 filename "a"}}]
   (let [tagged (diff old-text new-text)
         hunks  (impl/tagged->ranges tagged context)]
     (if (seq hunks)
       (str "--- a/" filename "\n"
            "+++ b/" filename "\n"
            (str/join "\n" (map (partial impl/format-hunk tagged) hunks)))
       ""))))

(defn colorize-unified-diff
  "Adds ANSI color codes to unified diff text.
  Hunk headers in cyan, deletions in red, additions in green."
  [diff-text]
  (-> diff-text
      (str/replace #"(?m)^(@@.*@@)$"       "\u001b[036m$1\u001b[0m")
      (str/replace #"(?m)^(\+(?!\+\+).*)$" "\u001b[032m$1\u001b[0m")
      (str/replace #"(?m)^(-(?!--).*)$"     "\u001b[031m$1\u001b[0m")))
