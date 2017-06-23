# Environment vars

- Repositories list urls
	- Defined as ;-separated value of SCM4J_VCS_REPOS environment var. Each url must lead to json-file with repositories descriptions
	- SCM4J_VCS_REPOS=file:///c:/workspace/vcs-repos.json;http://dev.untill.com/git/untillProtocols.git
- Credentials list urls
	- Defined as ;-separated value of SCM4J_CREDENTIALS environment var. Each url must lead to json-file with credentials descriptions
	- SCM4J_CREDENTIALS=file:///c:/workspace/credentials.json;http://dev.untill.com/ertiactory/repo/.../credentials.json
	
# Repositories list
- must be referenced by SCM4J_VCS_REPOS environment var
- configuration by convention
	- note: if "credentials" is omitted then default credentials are used
	- note: if "type" filed is omitted and if a repo url ends with ".git" then the repository is considered as Git, otherwise - SVN
```
[
    {
		"name": "eu.untill:untill",
		"url": "http://dev.untill.com/svn/untill"
	},
]
```
- explicit repositories config
	- note: credentials will be repleced with credentilas from crdentials file by name. So just credentials name is required here.
```
[
    {
		"name": "eu.untill:untill",
		"url": "http://dev.untill.com/svn/untill",
		"credentials": {
			"name": "gdy",
			"password": ""
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
		"name": "gdy",
		"password": "9usNgQxANUZC",
		"isDefault": true
	}
]
```

# VCS repositories workspaces
- located at user home folder by default or defined by SCMWorkfow(<product name>, <workspace home dir>) constructor



