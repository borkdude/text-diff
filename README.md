# text-diff

[![Clojars Project](https://img.shields.io/clojars/v/io.github.borkdude/text-diff.svg)](https://clojars.org/io.github.borkdude/text-diff)

Line-level text diffing for Clojure, ClojureScript and babashka. Produces the same unified diff format as `git diff` and `diff -u`.

## Installation

```clojure
io.github.borkdude/text-diff {:mvn/version "0.1.0"}
```

Or as a git dependency:

```clojure
io.github.borkdude/text-diff {:git/sha "..."}
```

## Usage

```clojure
(require '[borkdude.text-diff :as td])
```

### diff

Returns a vector of tagged lines:

```clojure
(td/diff "a\nb\nc" "a\nB\nc")
;; => [[:= "a"] [:- "b"] [:+ "B"] [:= "c"]]
```

### unified-diff

Produces unified diff output (`diff -u` format):

```clojure
(println (td/unified-diff "a\nb\nc\nd" "a\nc\nd" {:filename "test.clj"}))
```

```
--- a/test.clj
+++ b/test.clj
@@ -1,4 +1,3 @@
 a
-b
 c
 d
```

Options:
- `:filename` - filename for the header (default `"a"`)
- `:context` - number of context lines (default `3`)

### colorize-unified-diff

Adds ANSI color codes to unified diff text (cyan hunk headers, red deletions, green additions):

```clojure
(println (td/colorize-unified-diff (td/unified-diff "a\nb\nc" "a\nB\nc" {:filename "test.clj"})))
```

## License

MIT, see [LICENSE](LICENSE).
