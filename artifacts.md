# Configuration files
- `ver.conf`
  - Keeps development and release version numbers
- `mdeps.conf`
- `mdeps-changed.conf`
  - Actual for `release` branch only. List of managed dependenciens which has been changed since last minor version.

# Tags
- `#scm-mdeps`
  - Commit in `dev` branch which actualizes mdeps.conf dependencies
- `#scm-ver 5.0`
  - Commit in `dev` branch which increments dev version
- `#scm-ignore`

# Environment variables:
- SCM4j_CREDENTIALS
- SCM4j_REPOSITORIES
  
# ver.conf

Development branch:
```ini
ver=1.5.0
release=1.4.0
branchType=develop
```
  - `release` may exists in dev branch only and may be ommited there if no release branch has been created yet
  - `develop` is a default value for `branchType`
  
Release  branch:
```ini
ver=1.4.0
branchType=release
```  

# mdeps.conf
```
org.simplejavamail:simple-java-mail:4.2.3
org.apache.poi:poi:3.10.1
```
