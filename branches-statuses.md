All statuses are calculated top-down using `BranchesStatuses` class

# `develop` branch
- `IGNORED`
  - Last commit has `#scm-ignore`
- `MODIFIED`
  - Last commit has no `#scm-ver`
- `BRANCHED`
  - Last commit has `#scm-ver`
  
# `release` branch
- `MISSED`
  - Branch does not exist
- `TAGGED`
  - Tag exist
- `BUILT`
  - Last commit has `#scm-built`
- `MDEPS_BUILT_AND_TAGGED` 
  - All mdeps (if any) have fixed version. Means that every dep is built and tagged
- `BRANCHED`
  - Not all mdeps have fixed version
