[![Release](https://jitpack.io/v/scm4j/scm4j-wf.svg)](https://jitpack.io/#scm4j/scm4j-wf)
[![Build Status](https://travis-ci.org/scm4j/scm4j-wf.svg?branch=master)](https://travis-ci.org/scm4j/scm4j-wf)
[![Coverage Status](https://coveralls.io/repos/github/scm4j/scm4j-wf/badge.svg?branch=master)](https://coveralls.io/github/scm4j/scm4j-wf?branch=master) 

# Overview

Library povides API and CLI to automate such tasks as
- Create a minor version
- Create a patch version

 Ref. [Semantic Versioning 2.0.0](http://semver.org/) for definitions.
For low vcs operations scm4j-vcs-* libraries are used

# Terms
- `Managable dependency` (`mdeps`)
  - Component which has its own repository and is under control, i.e. can be built, branched and uploaded to a maven repository
  
# Data Structure
Ref. [data-structure.md](data-structure.md)
  
# Artifacts  
Ref. [artifacts.md](artifacts.md)

# Runner
- Install Groovy
- Download https://github.com/scm4j/scm4j-wf/blob/master/run.groovy file
- Execute runner
  ```
  groovy run.groovy -show|-build|-tag <artifact coords>
  ```

# ISCMWorkflow.getProductionReleaseAction()
Calculate actions to do the following things
- Run recursively getProductionReleaseAction for all mdeps
  - pass mdeps, since even if dependency is in actual state it may be needed to fetch its latest version (FetchVersion action)
- Actualize list of mdeps versions in dev branch
  - save: dev/mdeps.json
  - calc: lastVerCommit
- Create a release branch
    - `release` removed from `ver.conf`, if any
- Change dev/ver.conf
    - minor version increased 
# ISCMWorkflow.getTagReleaseAction()
Calculate actions do to the folowing things
- Run recursively getTagReleaseAction() for all mdeps
- Create VCS Tag on Head of each Release branch
# Code snippet
```java
ISCMWorkflow wf = new SCMWorkflow("eu.untill:untill");
IAction action = wf.getProductionReleaseAction();

PrintAction pa = new PrintAction();
pa.print(System.out, action);

try (IProgress progress = new ProgressConsole(action.getName(), ">>> ", "<<< ")) {
	action.execute(progress);
}
```
# Workflow
- call
- get last unbuilt version
- Release Branch of last unbuilt version missing? - fork
- Release Branch of last unbuilt version exists? - build

# Last unbuilt version determination
- take current trunk version
- Release Branch status is BUILT? 
- 