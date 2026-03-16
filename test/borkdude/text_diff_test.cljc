(ns borkdude.text-diff-test
  (:require [borkdude.text-diff :as td]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]))

(deftest diff-test
  (testing "identical inputs"
    (is (= [[:= "a"] [:= "b"] [:= "c"]]
           (td/diff "a\nb\nc" "a\nb\nc"))))
  (testing "single deletion"
    (is (= [[:= "a"] [:- "b"] [:= "c"]]
           (td/diff "a\nb\nc" "a\nc"))))
  (testing "single addition"
    (is (= [[:= "a"] [:+ "b"] [:= "c"]]
           (td/diff "a\nc" "a\nb\nc"))))
  (testing "replacement"
    (is (= [[:= "a"] [:- "b"] [:+ "B"] [:= "c"]]
           (td/diff "a\nb\nc" "a\nB\nc"))))
  (testing "empty inputs"
    (is (= [[:= ""]] (td/diff "" ""))))
  (testing "windows line endings"
    (is (= [[:= "a"] [:- "b"] [:+ "B"] [:= "c"]]
           (td/diff "a\r\nb\r\nc" "a\r\nB\r\nc")))))

(deftest identical-inputs-test
  (is (= "" (td/unified-diff "a\nb\nc" "a\nb\nc" {:filename "test.clj"}))))

(deftest single-deletion-test
  (let [result (td/unified-diff "a\nb\nc\nd" "a\nc\nd" {:filename "test.clj"})]
    (testing "contains header"
      (is (str/starts-with? result "--- a/test.clj\n+++ b/test.clj")))
    (testing "hunk counts"
      (is (str/includes? result "@@ -1,4 +1,3 @@")))
    (testing "shows deleted line"
      (is (str/includes? result "-b")))
    (testing "does not show added line"
      (is (not (re-find #"(?m)^\+[^+]" result))))))

(deftest single-addition-test
  (let [result (td/unified-diff "a\nc\nd" "a\nb\nc\nd" {:filename "test.clj"})]
    (is (str/includes? result "@@ -1,3 +1,4 @@"))
    (is (str/includes? result "+b"))))

(deftest replacement-test
  (let [result (td/unified-diff "a\nb\nc" "a\nB\nc" {:filename "test.clj"})]
    (is (str/includes? result "-b"))
    (is (str/includes? result "+B"))))

(deftest multiple-hunks-test
  (let [a (str/join "\n" (concat ["a" "b"] (repeat 10 "x") ["c" "d"]))
        b (str/join "\n" (concat ["a" "B"] (repeat 10 "x") ["C" "d"]))
        result (td/unified-diff a b {:filename "test.clj"})]
    (testing "produces two hunks"
      (is (= 2 (count (re-seq #"(?m)^@@" result)))))))

(deftest merged-hunks-test
  (let [a (str/join "\n" (concat ["a" "b"] (repeat 4 "x") ["c" "d"]))
        b (str/join "\n" (concat ["a" "B"] (repeat 4 "x") ["C" "d"]))
        result (td/unified-diff a b {:filename "test.clj"})]
    (testing "merges into one hunk"
      (is (= 1 (count (re-seq #"(?m)^@@" result)))))))

(deftest context-size-test
  (let [a (str/join "\n" (concat (repeat 20 "x") ["old"] (repeat 20 "x")))
        b (str/join "\n" (concat (repeat 20 "x") ["new"] (repeat 20 "x")))]
    (testing "default context of 3"
      (let [result (td/unified-diff a b {:filename "test.clj"})]
        (is (str/includes? result "@@ -18,7 +18,7 @@"))))
    (testing "custom context of 1"
      (let [result (td/unified-diff a b {:filename "test.clj" :context 1})]
        (is (str/includes? result "@@ -20,3 +20,3 @@"))))))

(deftest empty-inputs-test
  (is (= "" (td/unified-diff "" "" {:filename "test.clj"})))
  (testing "empty to something"
    (let [result (td/unified-diff "" "a\nb" {:filename "test.clj"})]
      (is (str/includes? result "+a"))
      (is (str/includes? result "+b"))))
  (testing "something to empty"
    (let [result (td/unified-diff "a\nb" "" {:filename "test.clj"})]
      (is (str/includes? result "-a"))
      (is (str/includes? result "-b")))))

(deftest windows-line-endings-test
  (let [result (td/unified-diff "a\r\nb\r\nc" "a\r\nB\r\nc" {:filename "test.clj"})]
    (is (str/includes? result "-b"))
    (is (str/includes? result "+B"))))

(deftest colorize-unified-diff-test
  (let [input  "@@ -1,3 +1,3 @@\n a\n-b\n+B\n c"
        result (td/colorize-unified-diff input)]
    (is (str/includes? result "\u001b[036m"))
    (is (str/includes? result "\u001b[031m"))
    (is (str/includes? result "\u001b[032m"))))
