# Overview

Library povides API and CLI to automate such tasks as
- Create a minor version
- Create a patch version

 Ref. [Semantic Versioning 2.0.0](http://semver.org/) for definitions.
For low vcs operations scm4j-vcs-* libraries are used

# Terms
- `Managable dependency` (`mdeps`)
 - Component which has its own repository and is under control, i.e. can be built, branched and uploaded to a maven repository
- Component
  - Artifact alias 
  - Can be Root or nested
- Develop Branch
  - Branch new features are commited to
  - Has version with -SNAPSHOT
  - Has full mDeps list without versions (-SNAPSHOTs only are provided)
- Release Branch
  - Branch the new Release is building on
  - named Release/<version>
  - Has Status. See [branches-statuses.md](branches-statuses.md)
  - Can exist or be MISSING
  - Can be Completed, i.e. have BUILT or TAGGED status
- Last Unbuilt Release Branch
  - Oldest Release Branch of last 2 versions which not TAGGED or BUILT or MISSING
  - Otherwise - current Release Branch
- Last Built Release Branch
  - Latest BUILT or TAGGED Release Branch of last 2 versions
  - Otherwise null
- Fork (verb), To Fork Release
  - To create a Release Branch based on Develop Branch head
  
  

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
# How it works
- First pass for root component
  - Action tree is built
    - Child Actions are built for all nested Components (if any)
    - Action for Root compoent is created, all child Actions are attached to it.
- Action determination workflow
  - If a Component's Develop Branch has new commits:
    - If Component's Release Branch is uncompleted then Component has to be built with NEW_FEATURES reason
    - If Component's Release Branch is MISSING or Completed then Component has to be forked with NEW_FEATURES reason
  - If a Component's Develop Branch has no new commits and not IGNORED, i.e. is BRRANCHED
    - If Component's Uncompleted Release Branch is MISSING then nothing should be done
    - If Compoents's Uncompleted Release Branch is Completed then Dependencies are checked for new versions:
      - No mDeps - nothing should be done
      - If at least one Dependency (last forked version is taken) is not used in Uncompleted Release Branch then the Component has to be forked or built with NEW_DEPENDENCIES reason
        - If Release Branch is MISSING then fork
	  - Any child Build Action (just first level, recusrsion is not necessary) is replaced with Action None. 
	  - E.g. on childs processing we took a decision to build a child. But now we sees that Root has to be forked. That means that all builds must be skipped until the entire tree fork is completed
	- otherwise build existing Uncompleted Release Branch
    - otherwise Compoents's build is unfinished and it has to be built with NEW_FEATURES reason (if has no mDeps) or with NEW_DEPENDENCIES reason (has mDeps)
- Action Fork workflow
  - All child actions are executed
  - Current Release Branch is determined
  - If CRB exists then we are already forked. According message is reported and fork process is considered as successful
  - Release Branch is created
    - mDeps versions are frozen and written to Release Branch
      - #scm-mdeps log tag is attached
    - Minor is increased in trunk
      - #scm-ver log tag is attached
    - version with no -SNAPSHOT is writen to Release Branch
      - #csm-ver log tag is attached
- Action Build workflow
  - All child actions are executed
  - Current Release Branch is determined
  - if CRB is already built then according message is reported and build process is considered as successful
  - Release Branch is checked out to a separate Locked Working Copy
  - Builder is executed withing the LWC. Standard output is reported.
  - If no errors then "build" file is created with built version content, #csm-built log tag is attached
  
  
