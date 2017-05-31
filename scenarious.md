# Migration to scm4j

Let your product consists of three components:

- com.mycompany:product
- com.mycompany:server
- com.mycompany:client

To start with scm4j

**Step1**. Create `mdeps` file for `product` component, version part may be avoided at this step

```ini
com.mycompany:server
com.mycompany:client
```

**Step2**. Create `version` file for all components. If `develop` branch of any component does not have `version` file scm4j fails.

**`#scm-ignore` usage**




