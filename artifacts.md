# Configuration files
- `version`
  - Keeps development and release version numbers
- `mdeps`
- `mdeps-changed`
  - Actual for `release` branch only. List of managed dependenciens which has been changed since last minor version

# Tags
- `#scm-mdeps`
  - Commit in `dev` branch which actualizes mdeps.conf dependencies
- `#scm-ver 5.0`
  - Commit in `dev` branch which increments dev version
- `#scm-ignore`

# Environment variables:
- SCM4j_CREDENTIALS  
- SCM4j_REPOSITORIES
  - `developBranchName`, `releaseBranchPrefix`
  
# version

Development branch:
```ini
1.5.0-SNAPSHOT
```
Release  branch:
```ini
1.4.0
```  

# mdeps
```
org.simplejavamail:simple-java-mail:4.2.3
org.apache.poi:poi:3.10.1
```
