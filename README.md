[![Release](https://jitpack.io/v/scm4j/scm4j-wf.svg)](https://jitpack.io/#scm4j/scm4j-wf)
[![Build Status](https://travis-ci.org/scm4j/scm4j-wf.svg?branch=master)](https://travis-ci.org/scm4j/scm4j-wf)
[![Coverage Status](https://coveralls.io/repos/github/scm4j/scm4j-wf/badge.svg?branch=master)](https://coveralls.io/github/scm4j/scm4j-wf?branch=master)

# Status

IN DEVELOPMENT

# Overview

Tool to manage multi-repositiry configuration. It  detectes changes in repositories, build new components versions and actualize dependency lists.

E.g. we have a `product3` which depends on `component39`:0.9.5 and `component50`:0.5.0, all components and `product3` sit in their own repositories. Now we add some commits to the `develop` branch of `component50` and run the tool using `status` command. Tool analyzes repositories and suggests that new versions of `component50` and `product3` should be built. Then we can run tool using  `fork` and `build` commands. `fork` command creates new `release branches` and increase minor versions in `develop branches`, `build` does whatever configured plus increases patch version in `release branch`.

For version definitions ref. [semantic Versioning 2.0.0](http://semver.org/).

# See also

[Historical version](https://github.com/scm4j/scm4j-wf/blob/d540cb00674d485846117dbd68df19bdad306e56/README.md)
