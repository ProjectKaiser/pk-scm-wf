Variable lists files which maps artifact coordinates to repositories parameters.
Deprecated name: `SCM4J_VCS_REPOS`

Example of variable (";" - separated): 

```
SCM4J_CC=c:/workspace/my-repos.yml;http://mycompany.com/repos/project1-repos.yml`
```
Default protocol is file:///

Yaml file consists of number of rules which are applied in order of appearance, first one which matches is used.

```yaml

# Just a component. Note that `releaseCommand` (deprecated: `builder`) is a must and ident of attribute specification must be at least three spaces

mycompany:component1: 
 url: http://mycompany.com/repos/component1
 releaseCommand: cmd /c gradlew.bat upload

# Two components in the same repository

component1|component2:
 url: http://mycompany.com/repos/components

# Coordinates which matches `my.*`. Repository name is constructed from coorinates name using regular expression

my(.*):
 url: http://mycompany.com/git/myProj$1
  
# `svn` type repository (`git` is default)

mycompany:component3:
 url: http://mycompany.com/repos/component3
 type: svn
  
# Repository where 
# a) `release` branches are prefixed with `B` (default is `release/`) 
# b) `develop` branch is named `branches/develop` (by default it is `trunk` or `master` according to the repository type).

mycompany:component4:
 url: http://mycompany.com/repos/component4
 type: svn
 releaseBranchPrefix: B
 developBranch: branches/develop

# All repos will have `rel` as a `release` branch prefix by default, if not specified above
~:
 releaseBranchPrefix: rel/

# Component with `afterTag` hook (ref. issue #8)

mycompany:component5:
 afterTag: cmd /c gradlew.bat afterTag

```
