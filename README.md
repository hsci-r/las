# Lexical Analysis Tool
Lexical Analysis Command-Line Tool for lemmatizing, lexical analysis, inflected form generation, hyphenation and language identification of multiple languages.

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

## Running

The LAS binary at https://github.com/jiemakel/las-cl/releases is actually a Java JAR file, to which a tiny shell script has been prepended, running the JAR with an allocation of 4G of memory. You can however run the JAR also directly with other parameters yourself, e.g. `java -Xmx2G -jar las --help`.

### Optimal mode of running

The memory allocation is necessary, as some of the transducers used by LAS are really quite huge (the biggest two some ~760 megabytes). This is also why the executable package is a whopping 400-900 megabytes (depending on release). This size also means that when running the program, initial loading will take a significant time (which you can test by running `las --help`). However, after that, processing will be fluent. This means that to optimally use the tool, you should pass LAS as much data in a single run as possible. LAS should be able to efficiently process both large files, as well as a large number of them. Another option is also to not give LAS a filename, whereby the tool will enter a a streaming mode, processing input line by line.

When running on files, one should also select the appropriate `--process-by` mode. The default is to process by `file`, which is suitable for small files. However, if you have larger files, you should process either by `paragraph` (if you have such paragraphs, separated by two newlines) or by `line`, if you know sentences won't cross lines.

## Functionalities

The library is also exposed as a web service at http://demo.seco.tkk.fi/las/ . The documentation that follows is mostly equivalent to the one there, with the exception that http://demo.seco.tkk.fi/las/ has live examples where you can experiment with the different functionalities and inputs.

### Language detection

Tries to recognize the language of an input. In total, the language detection supports 78 locales, combining results from three sources:

 * The [language-detector](https://github.com/optimaize/language-detector) library (locales `af, an, ar, ast, be, bg, bn, br, ca, cs, cy, da, de, el, en, es, et, eu, fa, fi, fr, ga, gl, gu, he, hi, hr, ht, hu, id, is, it, ja, km, kn, ko, lt, lv, mk, ml, mr, ms, mt, ne, nl, no, oc, pa, pl, pt, ro, ru, sk, sl, so, sq, sr, sv, sw, ta, te, th, tl, tr, uk, ur, vi, yi, zh-CN, zh-TW`),
 * custom code based on the list of cues at the [Wikipedia language recognition chart](http://en.wikipedia.org/wiki/Wikipedia:Language_recognition_chart) (locales `cs, de, en, es, et, fi, fr, hu, it, pl, pt, ro, ru, sk, sv`), and
 * finite state transducers provided by the [HFST](http://hfst.sourceforge.net/), [Omorfi](https://github.com/jiemakel/omorfi/) and [Giellatekno](http://giellatekno.uit.no/) projects (locales `de, en, fi, fr, it, la, liv, mdf, mhr, mrj, myv, sme, sv, tr, udm`)

Example:
```
Input: "The quick brown fox jumps over the lazy dog"
Output: {
  "locale" : "en",
  "certainty" : 0.6803500000000001,
  "details" : {
    "languageRecognizerResults" : { "en" : 0.1973 },
    "languageDetectorResults" : [ { "en" : 1.0 } ],
    "hfstAcceptorResults" : [
      { "en" : 0.84375 },
      { "fi" : 0.09375 },
      { "la" : 0.010416666666666666 },
      { "tr" : 0.010416666666666666 },
      { "sv" : 0.010416666666666666 },
      { "sme" : 0.010416666666666666 },
      { "it" : 0.010416666666666666 },
      { "de" : 0.010416666666666666 }
    ]
  }
}
```

### Lemmatization

Lemmatizes the input into its base form. Uses finite state transducers provided by the [HFST](http://hfst.sourceforge.net/), [Omorfi](https://github.com/jiemakel/omorfi/) and [Giellatekno](http://giellatekno.uit.no/) projects where available (locales `de, en, fi, fr, it, la, liv, mdf, mhr, mrj, myv, sme, sv, tr, udm`).
Snowball stemmers are used for locales `dk, es, nl, no, pt, ru` (not used: `de, en, fi, fr, it, sv`)

Note that the quality and scope of the lemmatization varies wildly between languages.

```
Input: "Albert osti fagotin ja töräytti puhkuvan melodian maakunnanvoudinvirastossa."
Output: "Albert ostaa fagotti ja töräyttää puhkua melodia maakuntavoutivirasto ."
```

### Morphological analysis

Gives a morphological analysis of the text. Uses finite state transducers provided by the provided by the [HFST](http://hfst.sourceforge.net/), [Omorfi](https://github.com/jiemakel/omorfi/) and [Giellatekno](http://giellatekno.uit.no/) projects.
Supported locales: `de, en, fi, fr, it, la, liv, mdf, mhr, mrj, myv, sme, sv, tr, udm`

Note that the quality and scope of analysis as well as tags returned vary wildly between languages (and see below for Finnish specifically, which has the most support).

Example:
```
Input: "Albert osti"
Output:
[ {
  "word" : "Albert",
  "analysis" : [ {
    "weight" : 0.099609375,
    "wordParts" : [ {
      "lemma" : "Albert",
      "tags" : {
        "SEGMENT" : [ "Albert" ],
        "KTN" : [ "5" ],
        "UPOS" : [ "PROPN" ],
        "NUM" : [ "SG" ],
        "PROPER" : [ "LAST" ],
        "CASE" : [ "NOM" ]
      }
    } ],
    "globalTags" : {
      "HEAD" : [ "2" ],
      "DEPREL" : [ "punct" ],
      "POS_MATCH" : [ "TRUE" ],
      "BEST_MATCH" : [ "TRUE" ]
    }
  }, {
    "weight" : 0.099609375,
    "wordParts" : [ {
      "lemma" : "Albert",
      "tags" : {
        "SEGMENT" : [ "Albert" ],
        "KTN" : [ "5" ],
        "UPOS" : [ "PROPN" ],
        "NUM" : [ "SG" ],
        "SEM" : [ "MALE" ],
        "PROPER" : [ "FIRST" ],
        "CASE" : [ "NOM" ]
      }
    } ],
    "globalTags" : {
      "HEAD" : [ "2" ],
      "DEPREL" : [ "punct" ],
      "POS_MATCH" : [ "TRUE" ],
      "BEST_MATCH" : [ "TRUE" ]
    }
  } ]
}, {
  "word" : "osti",
  "analysis" : [ {
    "weight" : 0.099609375,
    "wordParts" : [ {
      "lemma" : "ostaa",
      "tags" : {
        "TENSE" : [ "PAST" ],
        "SEGMENT" : [ "ost", "{MB}i" ],
        "KTN" : [ "53" ],
        "UPOS" : [ "VERB" ],
        "MOOD" : [ "INDV" ],
        "PERS" : [ "SG3" ],
        "INFLECTED_FORM" : [ "V N Nom Sg" ],
        "VOICE" : [ "ACT" ],
        "INFLECTED" : [ "ostaminen" ]
      }
    } ],
    "globalTags" : {
      "HEAD" : [ "0" ],
      "DEPREL" : [ "punct" ],
      "POS_MATCH" : [ "TRUE" ],
      "BEST_MATCH" : [ "TRUE" ]
    }
  } ]
} ]
```
### Inflected form generation

Transforms the text given a set of inflection forms (e.g. `V N Nom Sg, N Nom Pl, A Pos Nom Pl`), by default also converting words not matching the inflection forms to their base form.

Uses finite state transducers provided by the provided by the [HFST](http://hfst.sourceforge.net/), [Omorfi](https://github.com/jiemakel/omorfi/) and [Giellatekno](http://giellatekno.uit.no/) projects. Note that the inflection form syntaxes differ wildly between languages.

Supported locales: `de, en, fi, fr, it, liv, mdf, mhr, mrj, myv, sme, sv, tr, udm`

Example:
```
Input: "Albert osti fagotin ja töräytti puhkuvan melodian.", "V N Nom Sg, N Nom Pl, A Pos Nom Pl"
Output: "Albert ostaminen fagotit ja töräyttäminen puhkuminen melodiat ."
```

### Hyphenation

Hyphenates the given text. Uses finite state transducers provided by the provided by the [HFST](http://hfst.sourceforge.net/), [Omorfi](https://github.com/jiemakel/omorfi/) and [Giellatekno](http://giellatekno.uit.no/) projects. Those provided by HFST have been automatically translated from the TeX CTAN distribution's hyphenation rulesets.

Supported locales: `bg, ca, cop, cs, cy, da, el, es, et, eu, fi, fr, ga, gl, hr, hsb, hu, ia, in, is, it, la, liv, mdf, mhr, mn, mrj, myv, nb, nl, nn, pl, pt, ro, ru, sa, sh, sk, sl, sme, sr, sv, tr, udm, uk, zh`

Example:
```
Input: "Albert osti fagotin ja töräytti puhkuvan melodian."
Output: "al-bert os-ti fa-go-tin ja tö-räyt-ti puh-ku-van me-lo-dian"
```

## Things to know when using LAS for analyzing Finnish

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
