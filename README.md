# Overview

Library povides API and CLI to perform such tasks as
- Create a minor version
- Create a patch version

 Ref. [Semantic Versioning 2.0.0](http://semver.org/) for naming convention.

For low vcs operations scm4j-vcs- libraries are used

# Terms

- Managed dependency (mdep) / Контролируемая зависимость
  - Component which is under control, i.e. can be built, branched and uploaded to a maven repository
  
# Artifacts  

Ref. [artifacts.md](artifacts.md)

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
  
  
