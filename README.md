# las-cl
Lexical Analysis Command-Line Tool for lemmatizing, lexical analysis, inflected form generation and language identification of multiple languages.

Program help:
```
las 1.4.8
Usage: las [lemmatize|analyze|inflect|recognize|identify] [options] [<file>...]

Command: lemmatize
(locales: pt, mhr, fr, ru, myv, dk, it, mrj, liv, de, fi, es, tr, la, en, sv, udm, nl, mdf, sme, no)
Command: analyze
(locales: de, en, fi, fr, it, liv, mdf, mhr, mrj, myv, sme, sv, tr, udm, la)
Command: inflect
(locales: de, en, fi, fr, it, liv, mdf, mhr, mrj, myv, sme, sv, tr, udm)
Command: recognize
report word recognition rate (locales: de, en, fi, fr, it, liv, mdf, mhr, mrj, myv, sme, sv, tr, udm, la
Command: identify
identify language (locales: zh-TW, fi, no, hr, ta, ar, fr, is, lv, eu, mt, bn, dk, uk, pa, ga, br, so, pt, cs, fr, gl, sr, zh-CN, mrj, el, it, ca, vi, tl, nl, bg, ko, liv, it, mk, oc, et, af, de, ru, yi, cy, en, udm, ur, mdf, myv, sme, ru, ht, ml, th, id, sq, sv, de, sv, tr, da, en, gu, he, es, kn, sk, es, hi, te, mr, an, sw, be, pt, nl, ja, ast, fi, ro, mhr, ne, lt, no, km, sl, fa, ms, hu, pl, la, tr)
  --locale <value>
        possible locales
  --forms <value>
        inclection forms for inflect/analyze
  --segment
        segment baseforms?
  --no-guess
        Don't guess baseforms for unknown words?
  --no-segment-guessed
        Don't guess segmentation information for guessed words (speeds up processing significantly)?
  --process-by <value>
        Analysis unit when processing files (file, paragraph, line)?
  --max-edit-distance <value>
        Maximum edit distance for error-correcting unidentified words (default 0)?
  --no-pretty
        Don't pretty print json?
  <file>...
        files to process (stdin if not given)
  --help
        prints this usage text
```

## Further information

The LAS binary at https://github.com/jiemakel/las-cl/releases is actually a Java JAR file, to which a tiny shell script has been prepended, running the JAR with an allocation of 4G of memory. You can however run the JAR also directly with other parameters yourself, e.g. `java -Xmx2G -jar las --help`.

### Optimal mode of running

The memory allocation is necessary, as some of the transducers used by LAS are really quite huge (the biggest two some ~760 megabytes). This is also why the executable package is a whopping 400-900 megabytes (depending on release). This size also means that when running the program, initial loading will take a significant time (which you can test by running `las --help`). However, after that, processing will be fluent. This means that to optimally use the tool, you should pass LAS as much data in a single run as possible. LAS should be able to efficiently process both large files, as well as a large number of them. Another option is also to not give LAS a filename, whereby the tool will enter a a streaming mode, processing input line by line.

### Things to know when using LAS for analyzing Finnish

While LAS supports many languages, the most complete support it has is for Finnish. However, this also makes the functionality complex. Thus, it is useful to delve deeper into what is actually happening.

First, the Finnish analysis is based on a [fork](https://github.com/jiemakel/omorfi/) of the [Omorfi](https://github.com/flammie/omorfi/) morphological analyzer for Finnish. What the user needs to know about this is that Omorfi normally provides 1) all possible morphological analyses of a word and 2) only works for words that are included in its lexicon and rules.

To this baseline, the functionality in LAS (or the modified Omorfi) adds:
 1. support for better sentence splitting and tokenization from [Turku NLP](https://github.com/TurkuNLP/Finnish-dep-parser).
 1. support for guessing the most probable of multiple analyses
    1. by using case matching of the initial letter (if not the first word in a sentence)
    1. by using machine learned disambiguation from [Turku NLP](https://github.com/TurkuNLP/Finnish-dep-parser)
    1. by using word class and inflection -based rules
    1. by using word frequency information from the Finnish Wikipedia
 1. lemma guessing for words outside the lexicon
 1. support for Early Modern Finnish inflection
 1. support for edit-distance error correction (by up to 2 steps) in a guessed analysis
 1. automatic dehyphenation

 Final note: In analysis, Omorfi supports initial capitalization of words, necessitated by needing to analyze first words in a sentence without fuzz. However, nothing else is done. So, `pariisi` will return only `pari` as the lemma, and not `Pariisi`. (As a sidenote, if you actually *do* want case insensitive matching, you can thus convert every word into initial uppercase, but that will mess with the disambiguation)

 Examples of the various rules in action in lemmatization:
  * `Pariisi` -> `pari` (initial case is ignored for first word in a sentence)
  * `Pariisissa` -> `Pariisi` (cannot be an inflected form of pari)
  * `Pariisi on` -> `Pariisi olla` (machine learned disambiguation guesses correctly)
  * `pariisi on` -> `pari olla` (uppercasing not allowed)
  * `oli Pariisi` -> `olla Pariisi` (case change not allowed after first word in a sentence)
  * `oli pariisi` -> `olla pari` (case change not allowed after first word in a sentence)
  * `kuin` -> `kuin` (instead of `kuu`, based on word class and inflection rules)
  * `twiittasin` -> `tviitata`, (guessed, `twiittasin` for `--no-guess`)
  * `Leh>tim»ehen` -> `Lehtimies` for `--max-edit-distance 2`
  * `Helsingin` -> `Helsinki` (instead of the last name `Helsing`, based on Wikipedia frequency)
