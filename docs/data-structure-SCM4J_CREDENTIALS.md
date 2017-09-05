Example of variable (";" - separated): 

```
SCM4J_CREDENTIALS=file:///c:/workspace/credentials.yaml;http://company.com/repos/credentials.yaml
```

Yaml file consists of number of rules which are applied in order of appearance, first one which matches is used.

```yaml

# omap is a must since it defines order

!!omap

# Note that attribute ident must be at least three spaces

- https?://mycompany.com.*:
    name: user
    password: password
- https?://github.com.*:
    name: user2
    password: password2
- https?://strangesite.org.*:
    name: null
    password: null
- ~:
    name: guest
    password: guest
```
