Maps coordinates to reposirtories

Example of variable: `SCM4J_VCS_REPOS=file:///c:/workspace/my-repos.yaml;http://mycompany.com/repos/project1-repos.yaml`

Example of yaml file:

```yaml

#Just conponent
mycompany:component1:
  url: http://url.com/svn/myProject

#Two components in the same repository
component1|component2:
  url: http://url.com/svn/myProject
  
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
