# Artifacts

- [Component configuration files](#component-configuration-files)
  - [`version` file](#version-file)
  - [`mdeps` file](#mdeps-file)
- [Comment Tags](#comment-tags)

# Component Configuration Files

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

Contains few lines, :

```
com.mycompany:component-one:4.2.0
com.mycompany:component-two:1.56.0
com.mycompany:component-three:2.1.0
```

# Comment Tags

Comment tags are placed inside commit comments

- `#scm-mdeps`
  - Commit in `develop` branch which actualizes mdeps.conf dependencies
- `#scm-ver 1.5.0`
  - Commit in `develop` branch which increments version
- `#scm-ver release`
  - Commit in `release` branch which truncates `-SNAPSHOT`
- `#scm-ignore`
  - Commit in `develop` branch which shows that all previous feature commits are ignored

