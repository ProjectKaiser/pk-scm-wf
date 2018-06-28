[![Release](https://jitpack.io/v/scm4j/scm4j-releaser.svg)](https://jitpack.io/#scm4j/scm4j-releaser)
[![Build Status](https://travis-ci.org/scm4j/scm4j-releaser.svg?branch=master)](https://travis-ci.org/scm4j/scm4j-releaser)
[![Coverage Status](https://coveralls.io/repos/github/scm4j/scm4j-releaser/badge.svg?branch=master)](https://coveralls.io/github/scm4j/scm4j-releaser?branch=master)


# Terms

- `component`: component has its own repository and can be built and uploaded to some repository
- `project`: root component
- `managable dependency`: dependency of any component which is "under control", i.e. can be branched, built and uploaded to a maven repository
- `mdeps`: file which lists managable dependencies, is located at the root of every repository
- `develop branch`: branch which is used as a source for release branches (in other words - all development happens on). By default it is `master` or `trunk`, depending on VCS

# Usage

1. Add [`version`](docs/data-structure.md#version-file) file to your repository, optionally [`mdeps`](docs/data-structure.md#mdeps-file)
1. Clone https://github.com/scm4j/scm4j-releaser-shell
1. Execute `releaser`, it will create `cc`, `cc.yml`, `credentials.yml` files in $user.home/.scm4j folder. Edit these files (ref. comments there)
1. Run `releaser (status|fork|build) com.mycompany:my-root-component`

E.g. we have a `product3` which depends on `component39`:0.9.5 and `component50`:0.5.0, all components and `product3` sit in their own repositories. Now we add some commits to the `develop` branch of `component50` and run the tool using `status` command. Tool analyzes repositories and suggests that new versions of `component50` and `product3` should be built. Then we can run tool using  `fork` and `build` commands. `fork` command creates new `release branches` and increase minor versions in `develop branches`, `build` does whatever configured plus increases patch version in `release branch`.

For version definitions ref. [semantic Versioning 2.0.0](http://semver.org/).

**Run from gradle**

- gradlew run -Pa=status,com.mycompany:my-root-component

# Advanced `mdeps` Management

- It is safe to add a new entry without version to any SNAPSHOT mdeps
- If you add or modify some release mdeps you must also copy these changes to all your project mdeps
  - Otherwise it is possible to get "Inconsistent dependencies"
- It is safe to remove a record from mdeps and add it to `build.gralde` - this way you can change dependency version at your own risk, do not cry aftewards

# Under the Hood

- `CLI` gets ExtendedStatusTree using `ExtendedStatusTreeBuilder` class (ref. [release statuses](docs/minor-release-status.md))
- `ActionTreeBuilder` converts ExtendedStatusTree to ActionTree (IAction)
- IAction is executed

# Data Structure

- [data-structure](docs/data-structure.md)
- [release statuses](docs/minor-release-status.md)

# Features

- [delayed tagging](/../../issues/2)
- [exact versions in `develop`/`mdeps`](/../../issues/4)
- [postTag hooks](/../../issues/8)

# Related repositories
  
  - [scm4j-vcs-api](../../../scm4j-vcs-api)
  - [scm4j-vcs-git](../../../scm4j-vcs-git)
  - [scm4j-vcs-svn](../../../scm4j-vcs-svn)

# See also

[Historical version](https://github.com/scm4j/scm4j-releaser/blob/d540cb00674d485846117dbd68df19bdad306e56/README.md)

# Problems
- Subversion is very slow on VMs if network type is `NAT`. `Bridge` type gives  much better  perfomance. See similar problem  [here](https://blog.inventic.eu/2012/08/very-slow-svn-updates-from-virtual-machines-vmware/)
