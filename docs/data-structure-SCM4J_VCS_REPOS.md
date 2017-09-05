Example of variable (";" - separated): 

```
SCM4J_VCS_REPOS=file:///c:/workspace/my-repos.yaml;http://mycompany.com/repos/project1-repos.yaml`
```

Example of yaml file:

```yaml

# omap is a must since it defines order

!!omap

# Just a component

- mycompany:component1: 
   url: http://mycompany.com/repos/component1

# Two components in the same repository

- component1|component2:
   url: http://mycompany.com/repos/components
  
# Coordinates which matches `my.*`, repository name is constructed from repository name using regular expression

- my(.*):
   url: http://mycompany.com/git/myProj$1
  
# Subversion type repository

- mycompany:component2:
   url: http://mycompany.com/repos/component1
   type: svn
  
# Repository where 
# a) `release` branches are prefixed with `B` (default is `release/`) 
# b) `develop` branch is named `branches/develop` (by default it is `trunk` or `master` according to the repository type).

- mycompany:component3:
   url: http://mycompany.com/repos/component3
   type: svn
   releaseBanchPrefix: B
   devBranch: branches/develop

# All repos will have "rel/" as a release branch prefix by default, if not specified above
- ~:
   releaseBanchPrefix: rel/

```
