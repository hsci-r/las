# las-cl
Lexical Analysis Command-Line Tool for lemmatizing, lexical analysis and language identification of multiple languages.

Program help:
```
las 1.3.0
Usage: las [lemmatize|analyze|inflect|recognize|identify] [options] [<file>...]

Command: lemmatize
(locales: pt, mhr, fr, ru, myv, dk, it, mrj, liv, de, fi, es, tr, la, en, sv, udm, nl, mdf, sme, no)
Command: analyze
(locales: de, en, fi, fr, it, liv, mdf, mhr, mrj, myv, sme, sv, tr, udm, la)
Command: inflect
(locales: de, en, fi, fr, it, liv, mdf, mhr, mrj, myv, sme, sv, tr, udm)
Command: recognize
report recognition rate (locales: de, en, fi, fr, it, liv, mdf, mhr, mrj, myv, sme, sv, tr, udm, la
Command: identify
identify language (locales: hy, fi, no, lb, hr, ta, ka, ar, fr, is, ug, lv, eu, am, mt, bn, uz, dk, uk, si, ky, pa, ga, tt, so, pt, cs, fr, gn, sr, mrj, el, it, ca, os, vi, yo, dv, tl, nl, bg, ko, liv, tk, it, mk, et, af, de, ru, yi, cy, en, udm, ur, ln, mdf, jv, myv, sme, ru, ml, th, id, pnb, sq, sv, de, sv, tr, da, my, zh-tw, en, gu, he, es, kn, sk, az, lij, es, fo, hi, te, mr, sw, be, qu, pt, nl, mi, ja, zh-cn, fi, bo, ro, mhr, ne, lt, no, km, kk, fa, mn, hu, pl, la, tr)
  --locale <value>
        possible locales
  --forms <value>
        inclection forms for inflect/analyze
  --segment
        segment compound words?
  --no-guess
        Don't guess baseforms for unknown words?
  --no-segment-guessed
        Don't guess segmentation information for guessed words (speeds up processing significantly)?
  --no-pretty
        Don't pretty print analysis json in file output?
  <file>...
        files to process (stdin if not given)
  --help
        prints this usage text

```
