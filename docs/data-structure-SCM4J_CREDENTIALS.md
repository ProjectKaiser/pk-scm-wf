Example of variable (";" - separated): 

```
SCM4J_CREDENTIALS=c:/workspace/credentials.yaml;http://company.com/repos/credentials.yaml
```
Default protocol is file:///

Yaml file consists of number of rules which are applied in order of appearance, first one which matches is used.

```yaml

# Note that attribute ident must be at least three spaces

https?://mycompany\.com/.*:
  name: user
  password: password1
https?://github\.com/.*:
  name: user2
  password: password2
https?://strangesite\.org/.*:
  name: null
  password: null
~:
  name: guest
  password: guest
```

Note: for `github` you better use tokens rather than account password: https://github.com/settings/tokens
