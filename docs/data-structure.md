# TOC

- [component configuration files](#component-configuration-files)
  - [`version` file](#version-file)
  - [`mdeps` file](#mdeps-file)
- [comment tags](#comment-tags)
- [environment vars](#environment-vars)
- [working files](#working-files)

# component configuration files

Component configuration files should be  located in the root of repository

- `version`
  - Keeps development and release version numbers
- `mdeps`
  - Managed dependencies list
- `mdeps-changed`
  - Actual for `release` branch only. List of managed dependenciens which has been changed since last minor version
  
## `version` file

Contains a single line.

`develop` branch:
```ini
1.5.0-SNAPSHOT
```
`release`  branch:
```ini
1.4.0
```  

## `mdeps` file

`develop` branch:

```
com.mycompany:component-one:-SNAPSHOT
com.mycompany:component-two:-SNAPSHOT
com.mycompany:component-three:-SNAPSHOT
```

`release` branch:

```
com.mycompany:component-one:4.2.0
com.mycompany:component-two:1.56.0
com.mycompany:component-three:2.1.0
```

versions in `release` branch are relaced automatically during `fork` operation.

# comment tags

Comment tags are placed inside commit comments

- `#scm-mdeps`
  - Commit in `release` branch which actualizes mdeps.conf dependencies
- `#scm-ver 1.5.0`
  - Commit in `develop` branch which increments version
- `#scm-ver release`
  - Commit in `release` branch which truncates `-SNAPSHOT`
- `#scm-ignore`
  - Commit in `develop` branch which shows that all previous feature commits are ignored
  
# Environment Vars

- [SCM4J_VCS_REPOS](data-structure-SCM4J_VCS_REPOS.md): list of yaml files which map artifact coordinates to repositories URLs
- [SCM4J_CREDENTIALS](data-structure-SCM4J_CREDENTIALS.md): list of yaml files which defines credentials

# Working Files

Working files are located at ${user.home}/.scm4j

- `vcs-repositories`: repositories working copies
