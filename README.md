# Overview

Library povides API and CLI to perform such tasks as (Ref [Semantic Versioning 2.0.0](http://semver.org/))
- Create a minor version
- Create a patch version

For low vcs operations scm4j-vcs- libraries are used

# Terms

- Managed dependency (mdep) / Контролируемая зависимость
  - Component which is under control, i.e. can be built, branched and uploaded to a maven repository
  
# Artifacts  

Configuration files:
- ver.conf
- mdeps.conf
- mdeps-changed.conf

Tags:
- `#scm-mdeps`
  - Commit in `dev` branch which actualizes mdeps.conf dependencies
- `#scm-ver 5.0`
  - Commit in `dev` branch which increments dev version
- `#scm-ignore`

Environment variables:
- SCM4j_CREDENTIALS
- SCM4j_REPOSITORIES
  
# ver.conf

Development branch:
```ini
ver=1.5.0
release=1.4.0
branchType=develop
```
  - `release` may exists in dev branch only and may be ommited there if no release branch has been created yet
  - `develop` is a default value for `branchType`
  
Release  branch:
```ini
ver=1.4.0
branchType=release
```  

# mdeps.conf
```
org.simplejavamail:simple-java-mail:4.2.3
org.apache.poi:poi:3.10.1
```

# ISCMWorkflow.ultimateProduction

Calculate actions to do the following things

- Run recursively calculateProductionReleaseActions for all  mdeps
  - pass mdeps, since even if dependency is in actual state it may be needed to fetch its latest version (FetchVersion action)
- Actualize list of mdeps versions in dev branch
  - save: dev/mdeps.json
  - calc: lastVerCommit
- Create a release branch
    - `release` removed from `ver.conf`, if any
- Change dev/ver.conf
    - minor version increased 
  
  
