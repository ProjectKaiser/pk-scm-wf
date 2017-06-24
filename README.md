# Overview

Library povides API and CLI to automate such tasks as
- Create a minor version
- Create a patch version

 Ref. [Semantic Versioning 2.0.0](http://semver.org/) for definitions.

For low vcs operations scm4j-vcs- libraries are used

# Terms

- `Managable dependency` (`mdeps`)
 - Component which has its own repository and is under control, i.e. can be built, branched and uploaded to a maven repository
  
# Data Structure

Ref. [data-structure.md](data-structure.md)
  
# Artifacts  

Ref. [artifacts.md](artifacts.md)

# ISCMWorkflow.getProductionReleaseAction

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
    
# Code snippet
```java
ISCMWorkflow wf = new SCMWorkflow("eu.untill:untill");
IAction action = wf.getProductionReleaseAction();
System.out.println(action.toString());
try (IProgress progress = new ProgressConsole(action.getName(), ">>> ", "<<< ")) {
	action.execute(progress);
}
```

  
  
