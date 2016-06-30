---
title: 'LAS: an integrated language analysis tool for multiple languages'
tags:
  - NLP
  - language analysis
  - morphological analysis
  - lemmatization
  - language identification
authors:
 - name: Eetu Mäkelä
   orcid: 0000-0002-8366-8414
   affiliation: Aalto University
date: 29 June 2016
bibliography: paper.bib
---

# Summary

LAS is a command-line tool for lemmatizing, morphological analysis, inflected form generation, hyphenation and language identification of multiple languages.

These functionalities are of use as part of many workflows requiring natural language processing. Indeed, LAS has been used for example as part of a pipeline for entity recognition [@makela-sarpa-2014], in creating a contextual reader for texts in English, Finnish and Latin [@makela-et-al-core-dh2016], and for processing a Finnish historical newspaper collection in preparation for data publication [@dhn2016].

The functionalities of LAS are mostly based on integrating existing tools into a common package. Particularly, the tool bases on:
 * Finite state transducers provided by the [HFST](http://hfst.sourceforge.net/) [@hfst], [Omorfi](https://github.com/flammie/omorfi/) [@pirinen2015omorfi] and [Giellatekno](http://giellatekno.uit.no/) [@moshagenopen] projects
 * [Snowball](http://snowballstem.org/) stemmers
 * the [language-detector](https://github.com/optimaize/language-detector) library
 * Statistical language models from [Turku NLP](http://turkunlp.github.io/Finnish-dep-parser/) [@haverinen2013tdt]

While LAS supports many languages, the most complete support it has is for Finnish, where considerable work has gone into improving the results.

Aside from a being available as a command-line tool, the functionalities in LAS are also available as a web service, at http://demo.seco.tkk.fi/las/.

# References
