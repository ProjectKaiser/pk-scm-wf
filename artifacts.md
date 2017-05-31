# Configuration files
- `version`
  - Keeps development and release version numbers
- `mdeps`
  - Managed dependencies list
- `mdeps-changed`
  - Actual for `release` branch only. List of managed dependenciens which has been changed since last minor version

# Tags
- `#scm-mdeps`
  - Commit in `develop` branch which actualizes mdeps.conf dependencies
- `#scm-ver 1.5.0`
  - Commit in `develop` branch which increments version
- `#scm-ver release`
  - Commit in `release` branch which truncates `-SNAPSHOT`
- `#scm-ignore`

# Environment variables:
- SCM4j_CREDENTIALS  
- SCM4j_REPOSITORIES
  - `developBranchName`, `releaseBranchPrefix`
  
# `version` file

Contains a single line.

`develop` branch:
```ini
1.5.0-SNAPSHOT
```
`release`  branch:
```ini
1.4.0
```  

# `mdeps` file

Contains few lines.

```
com.mycompany:component-one:4.2.0
com.mycompany:component-two:1.56.0
com.mycompany:component-three:2.1.0
```
