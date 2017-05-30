# Overview

Library povides API and CLI to perform such tasks as
- Create major software version
- Create minor software version

For low vcs operations scm4j-vcs- libraries are used

# Terms

- Managed dependency (mdep) / Контролируемая зависимость
  - Component which is under control, i.e. can be built, branched and uploaded to a maven repository
- Development branch (`devBranch`)
  - Branch developers commit changes to. By default `master` for git and `trunk` for svn
  
# Artifacts  

- ver.conf
- mdeps.conf
- mdeps-changed.conf
- `#scm-mdeps`
  - Commit in `dev` branch which actualizes mdeps.conf dependencies
- `#scm-ver 5.0`
  - Commit in `dev` branch which increments dev version
- `#scm-ignore`
- SCM4j_CREDENTIALS
- SCM4j_REPOSITORIES
  
# ver.conf

Development branch:
```ini
ver=5

#`childVer` appears only if release branch exists
childVer=4.0
```
  
Release  branch:
```ini
#".0" is added automatically
ver=4.0
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
    - save release/ver.json
      - ver.json.ver = ver + ".1"
      - ver.json.verCommit = lastVerCommit
      - ver.json.lastVerCommit = null
- Change dev/ver.json
  - lastVerCommit
  - childVer = ver + ".1"
  - ver = ver + 1
  
  
  
