# las-cl
Lexical Analysis Command-Line Tool for lemmatizing, lexical analysis and language identification of multiple languages.

Program help:
```
las 1.4.5
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
