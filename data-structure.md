# Environment vars

- `SCM4J_VCS_REPOS`: list of coord=>URL maps
	- Example: `SCM4J_VCS_REPOS=file:///c:/workspace/vcs-repos.yaml;http://host/git/untillProtocols.git`
- `SCM4J_CREDENTIALS`: list of url=>credentials maps
	- `SCM4J_CREDENTIALS=file:///c:/workspace/credentials.yml;http://company.com/repos/credentials.yml` 
	
# `coord => URL` map
- Need to match a dependency by its coord to its Repository
- Must be referenced by `SCM4J_VCS_REPOS environment var
- Represented as YAML which describes repository parameters for dependencies by its coords. Coords could contain regular expressions. Described Repository will be assigned to all matching dependencies.
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

# `url => credentials` map
- Need to match a Repository by its url to its Credentials
- Must be referenced by SCM4J_CREDENTIALS environment var
- Represented as YAML which defines credentials for repository urls. Urls could contains regular expressions. Described Credentials will be assigned to all matching Repositories 
```yaml
https?://url\.com.*:
  name: user
  password: password
http://localhost.*:
  name: null
  password: null
.*:
  name: guest
  password: guest
```

# Working Files

Working files are located at ${user.home}/.scm4j

- vcs-repositories: repositories working copies

# ver file
- Single line, no comments. Exmaple:
```
1.0.0-SNAPSHOT
```

# mdeps file
- multiline, #-commented. Example:
```
eu.untill:unTillDb:1.11.2#sddfgdfgdfg
eu.untill:UBL:1.99.2```
