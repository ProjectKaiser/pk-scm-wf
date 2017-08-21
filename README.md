[![Release](https://jitpack.io/v/scm4j/scm4j-wf.svg)](https://jitpack.io/#scm4j/scm4j-wf)
[![Build Status](https://travis-ci.org/scm4j/scm4j-wf.svg?branch=master)](https://travis-ci.org/scm4j/scm4j-wf)
[![Coverage Status](https://coveralls.io/repos/github/scm4j/scm4j-wf/badge.svg?branch=master)](https://coveralls.io/github/scm4j/scm4j-wf?branch=master)

# Overview

Library povides API and CLI to automate such tasks as
- Create a minor version
- Create a patch version

 Ref. [Semantic Versioning 2.0.0](http://semver.org/) for definitions.
For low vcs operations scm4j-vcs-* libraries are used

- Last Unbuilt Release Branch
  - Oldest Release Branch of last 2 versions which not TAGGED or BUILT or MISSING
  - Otherwise - current Release Branch
- Last Built Release Branch
  - Latest BUILT or TAGGED Release Branch of last 2 versions
  - Otherwise null
- Fork (verb), To Fork Release
  - To create a Release Branch based on Develop Branch head
  

# Terms
- `Managable dependency` (`mdeps`)
  - Component which has its own repository and is under control, i.e. can be built, branched and uploaded to a maven repository
- Component
  - Artifact alias 
  - Can be Root or Nested
- Component Repository
  - Component's VCS Repository
  - SVN and Git are supported
- Develop Branch
  - Branch new features are commited to
  - Has version with -SNAPSHOT
  - Has full mDeps list without versions (-SNAPSHOTs only are provided)
- Release Branch
  - Branch the new Release is building on
  - named Release/<version without patch and -SNAPSHOT>
  - Has Status. See [branches-statuses.md](branches-statuses.md)
  - Can exist or be MISSING
  - Can be Completed, i.e. have BUILT or TAGGED status
  - Can be Uncompleted, i.e. exists but have neither BUILT nor TAGGED status 
- Current Release Branch, CRB
  - The Release Branch with last unbuilt version or Release Branch of current Develop version (if no unbuilt versions)
  - Examples:
    - Commit a feature, fork new version. 
      - Trunk has version 2.0-SNAPSHOT, Current Release Branch is Release/1.0
      - Release/1.0 is still used although new features are commited to Dev Branch 
    - Build (and optinaly tag) Release/1.0
      - CRB is Release/2.0 (MISSING for now)
- Version file
  - file `version` which contains current product version. Located in the Product Repository root
    - has -SNAPSHOT in Develop Branch
    - has no -SNAPSHOT in Release Branch
    - See [data-structure.md](data-structure.md)
- MDeps file
  - Managed Dependencies definition file. Located in the Component Repository root
  - Must not contain exact versions in Develop Branch i.e. com.my:component:-SNAPSHOT
  - Exact versions are written automatically in Release Branch on Fork stage. This process calls "MDeps freezing"
  
# Usage
- Install [groovy](http://groovy-lang.org/install.html)
- Download and save [run.groovy](run.groovy) file
- Add Version file to all repositories
- Add MDeps file to all repositories if necessary
- Configure repositories
  - Create repository definition yaml file
    - Can be file or url
  - Create Credentials definition yaml 
    - Can be file or url
    - Optional
  - Set SCM4J_VCS_REPOS environment var value as url to Repository definition yaml file
    - file:///c:/repos.yaml
    - http://my.server/repos.yaml
  - Set SCM4J_CREDENTIALS environment var value as url to Credentials definition yaml file
- Execute `groovy run.groovy -show <product coords>` to show what actions are needed to execute as the next step to build a release
- Execute `groovy run.groovy -fork <product coords>` to fork all necessary release branches. Note: if all branches are created alredy then nothing will be made
- Execute `groovy run.groovy -build show <product coords>` to build all components. Note: if not all release branches are created or eveerything is built already then nothing will be done

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
  - Component's Develop Branch has new commits:
    - Compoent's CRB is determined
    - CRB is Uncompleted 
      - Component has to be built with NEW_FEATURES reason
    - CRB is MISSING or Completed 
      - Component has to be forked with NEW_FEATURES reason
  - Develop Branch has no new commits and not IGNORED, i.e. is BRANCHED
    - CRB is MISSING 
      - then nothing should be done
    - CRB is Completed then Dependencies are checked for new versions:
      - No mDeps - nothing should be done
      - If at least one Dependency (last forked version is taken) is not used in CRB then the Component has to be forked with NEW_DEPENDENCIES reason
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
# To do
  - throw exception if a version is missed
    - Have Dev Branch version 1.0 
    - manualy changed version to 3.0
    - Exception must be thrown because we can not determine if we used 1.0 in parent Component
  
