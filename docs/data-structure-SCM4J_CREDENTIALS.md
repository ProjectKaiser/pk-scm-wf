Example of variable (";" - separated): 

```
SCM4J_CREDENTIALS=file:///c:/workspace/credentials.yaml;http://company.com/repos/credentials.yaml
```

Example of yaml file

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
