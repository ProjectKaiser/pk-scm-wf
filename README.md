# Overview

Library povides API and CLI to perform such tasks as
- Create major software version
- Create minor software version

For low vcs operations scm4j-vcs- libraries are used

# Terms

- Managed dependency (mdep) / Контролируемая зависимость
  - Component which is under control, i.e. can be built, branched and uploaded to a maven repository
  
# Artifacts  

- ver.conf
- mdeps.conf
- `#scm-mdeps`
- `#scm-ver`
- `#scm-ignore`
  
# SCM Actions

Actions are orginiazed into trees and can be executed

ISCMAction
  - `execute() throws Exception`
  - `ISCMAction getParent() //May be null`
  - `LinkedHashMap<String, ISCMAction> getActions() //not null`
  - `Object getResult() //may be null`
  - `Object getChildResult(String childName) throws EChildNotFound`

# ver.json

Development branch:
```json
  {
    "ver": "4"
    ,"childVer": 3.1
    ,"lastVerCommit": "???"
  }
```  
  
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
  


  
