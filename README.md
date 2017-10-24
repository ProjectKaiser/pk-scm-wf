[![Release](https://jitpack.io/v/scm4j/scm4j-releaser.svg)](https://jitpack.io/#scm4j/scm4j-releaser)
[![Build Status](https://travis-ci.org/scm4j/scm4j-releaser.svg?branch=master)](https://travis-ci.org/scm4j/scm4j-releaser)
[![Coverage Status](https://coveralls.io/repos/github/scm4j/scm4j-releaser/badge.svg?branch=master)](https://coveralls.io/github/scm4j/scm4j-releaser?branch=master)


# Terms

- `component`: component has its own repository and can be built and uploaded to some repository
- `project`: has a component and  can be installed (together with all dependencies) as "a whole", from end-user POV
- `managable dependency`: dependency of any component which is "under control", i.e. can be branched, built and uploaded to a maven repository
- `mdeps`: file which lists managable dependencies, is located at the root of every repository

# Scenarious

- view project `status`: if something has been changed in `develop` and `release` branches of any component?
- `fork`: create `release` branches out of changed `develop` branches
- `build`: apply `build` command to `release` branches

E.g. we have a `product3` which depends on `component39`:0.9.5 and `component50`:0.5.0, all components and `product3` sit in their own repositories. Now we add some commits to the `develop` branch of `component50` and run the tool using `status` command. Tool analyzes repositories and suggests that new versions of `component50` and `product3` should be built. Then we can run tool using  `fork` and `build` commands. `fork` command creates new `release branches` and increase minor versions in `develop branches`, `build` does whatever configured plus increases patch version in `release branch`.

For version definitions ref. [semantic Versioning 2.0.0](http://semver.org/).

# Installation

1. Install groovy, download [run.grovy](https://raw.githubusercontent.com/scm4j/scm4j-releaser/release/10/run.groovy) and execute `groovy run.groovy`, it will list available commands

2. Add `version` file to your repository, optinally `mdeps`, configure `SCM4J_VCS_REPOS` and `SCM4J_CREDENTIALS` environment variables. Ref. [data-structure](docs/data-structure.md) for more details

# Data Structure

- [data-structure](docs/data-structure.md)
- [release statuses](/../../issues/10)

# Features

- [delayed tagging](/../../issues/2)
- [exact versions in `develop`/`mdeps`](/../../issues/4)
- [postTag hooks]()


# See also

[Historical version](https://github.com/scm4j/scm4j-releaser/blob/d540cb00674d485846117dbd68df19bdad306e56/README.md)
