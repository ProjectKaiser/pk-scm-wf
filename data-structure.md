# Environment vars

- Repositories list urls
	- Defined as ;-separated value of SCM4J_VCS_REPOS environment var. Each url must lead to json-file with repositories descriptions
	- `SCM4J_VCS_REPOS=file:///c:/workspace/vcs-repos.json;http://host/git/untillProtocols.git`
- Credentials list urls
	- Defined as ;-separated value of SCM4J_CREDENTIALS environment var. Each url must lead to json-file with credentials descriptions
	- `SCM4J_CREDENTIALS=file:///c:/workspace/credentials.json;http://host/artiactory/repo/.../credentials.json`
	
# Repositories list
- must be referenced by SCM4J_VCS_REPOS environment var
- configuration by convention
	- note: if "credentials" is omitted then default credentials are used
	- note: if "type" filed is omitted and if a repo url ends with ".git" then the repository is considered as Git, otherwise - SVN
```
[
	{
		"name": "eu.untill:untill",
		"url": "http://host"
	},
]
```
- explicit repositories config
```
[
	{
		"name": "eu.untill:untill",
		"url": "http://host",
		"credentials": {
			"name": "username"
		},
		"type": "GIT"
	},
]
```

# Credentials list
- must be referenced by SCM4J_CREDENTIALS environment var
	- note: "isDefault" field is false by default. Can be omited.
```
[
	{
		"name": "username",
		"password": "password",
		"isDefault": true
	}
]
```

# VCS repositories workspaces
- located at user home folder by default or defined by SCMWorkfow(<product name>, <workspace home dir>) constructor

# ver file
- Single line, no comments. Exmaple:
	- `1.0.0-SNAPSHOT`

# mdeps file
- multiline, #-commented. Example:
-	```
	eu.untill:unTillDb:1.11.2#sddfgdfgdfg
	eu.untill:UBL:1.99.2
	```
