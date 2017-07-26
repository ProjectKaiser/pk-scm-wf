# Overview

Library povides API and CLI to automate such tasks as
- Create a minor version
- Create a patch version

 Ref. [Semantic Versioning 2.0.0](http://semver.org/) for definitions.
For low vcs operations scm4j-vcs-* libraries are used

# Terms
- `Managable dependency` (`mdeps`)
 - Component which has its own repository and is under control, i.e. can be built, branched and uploaded to a maven repository
- Release Branch
  - Branch the new Release is building on
  - named Release/<version>
  - Has Status. See [branches-statuses.md](branches-statuses.md)
  - Can exist or be missing
- Last Unbuilt Release Branch
  - Oldest Release Branch of last 2 versions which not TAGGED or BUILT or MISSING
  - Otherwise - current Release Branch
- Last Built Release Branch
  - Latest BUILT or TAGGED Release Branch of last 2 versions
  - Otherwise null

# Data Structure
- [branches-statuses.md](branches-statuses.md)
- [data-structure.md](data-structure.md)
  
- [branches-statuses.md](branches-statuses.md)
- [data-structure.md](data-structure.md)
  
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
