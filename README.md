# Overview

Library povides API and CLI to perform such tasks as
- Create major software version
- Create minor software version

For low vcs operations scm4j-vcs- libraries are used

# Terms

- Managed dependency (mdep) / Контролируемая зависимость
  - Component which is under control, i.e. can be built, branched and uploaded to a maven repository
- Development branch
  - Branch developers commit changes to. `master` for git and `trunk` for svn
  
# Artifacts  

- ver.conf
- mdeps.conf
- mdeps-changed.conf
- `#scm-mdeps`
- `#scm-ver 4.0`
  - Commit in a `dev` branch
- `#scm-ignore`
- SCM4j_CREDENTIALS
- SCM4j_REPOSITORIES
  
# ver.conf

Development branch:
```json
  {
    "ver": "4"
    ,"childVer": 3.0
  }
```
`childVer` appears only if release branch exists
  
Release  branch:
```json
  {
    "ver": "4.1"
    ,"verCommit": "???"
  }
```  

# mdeps.json
```json
[
   "org.simplejavamail:simple-java-mail:4.2.3"
  ,"org.apache.poi:poi:3.10.1"
]
```

# ISCMWorkflow.calculateProductionReleaseActions

Calculate actions to do the following things

- Run recursively calculateProductionReleaseActions for all  mdeps
  - pass mdeps, since even if dependency is in actual state it may be needed to fetch its latest version (FetchVersion action)
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
  


  
