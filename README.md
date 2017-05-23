# Overview

Library povides API and CLI to perform such tasks as
- Create major software version
- Create minor software version

For low vcs operations scm4j-vcs- libraries are used

# Terms

- Managed dependency (mdep) / Контролируемая зависимость
  - Component which is under control, i.e. can be built, branched and uploaded to a maven repository
  

# Scenarious

## ISCMWorkflow.calculateProductionReleaseActions

Calculate actions to do the following things

- Run analyzeVersion for all  mdeps
- Actualize list of mdeps versions in dev branch
  - save: dev/mdeps.json
  - calc: lastVerCommit
- Create a release branch
    - save release/ver.json
      - ver.json.ver = ver + ".1"
      - ver.json.verCommit = lastVerCommit
      - ver.json.lastVerCommit = null
- Change dev/ver.json
  - lastVerCommit
  - childVer = ver + ".1"
  - ver = ver + 1



  
 
  


  
