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
  
# Environment Vars

- SCM4J_VCS_REPOS: list of yaml files which map artifact coordinates to repositories URLs
- SCM4J_CREDENTIALS: list of yaml files which keeps credentials

## SCM4J_VCS_REPOS

Example of variable: `SCM4J_VCS_REPOS=file:///c:/workspace/my-repos.yaml;http://mycompany.com/repos/project1-repos.yaml`

Example of yaml file:

```yaml

#Two artifacts in the same repository
artA1|artA2:
  url: http://url.com/svn/prjA
  
#Artifacts which are prefixed with my.*, repository name is constructed with no prefix using regexps
my(.*):
  url: http://localhost/git/myProj$1
  # git and svn types are supported. If ommited then:
  #   if url ends with ".git" then type is git
  #   otherwise - svn
  type: git
  
  # default "release/"
  releaseBanchPrefix: B
  # Branch name which is considered as development branch, i.e. to create release branches from. Null means "master" branch for Git, "trunk/" branch for SVN. Default is null.
  devBranch: null
.*:
  url: https://github.com/qwerty/$0
  type: svn
  devBranch: branches/
```

## SCM4J_CREDENTIALS

aaa 
