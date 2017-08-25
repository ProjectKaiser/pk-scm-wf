[![Release](https://jitpack.io/v/scm4j/scm4j-wf.svg)](https://jitpack.io/#scm4j/scm4j-wf)
[![Build Status](https://travis-ci.org/scm4j/scm4j-wf.svg?branch=master)](https://travis-ci.org/scm4j/scm4j-wf)
[![Coverage Status](https://coveralls.io/repos/github/scm4j/scm4j-wf/badge.svg?branch=master)](https://coveralls.io/github/scm4j/scm4j-wf?branch=master)

[Old version](https://github.com/scm4j/scm4j-wf/blob/d540cb00674d485846117dbd68df19bdad306e56/README.md)

# Overview

Tool to build multi-repositiry projects. Tool detectes changes in repositories, build new versions and actualize dependency lists.

E.g. we have a `project1` which depends on `component1.1` and `component1.2`, all components and `project1` sit in their own repositories. `project1` depends
