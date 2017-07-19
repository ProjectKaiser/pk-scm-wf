All statuses are calculated top-down using `BranchesStatuses` class

# `develop` branch
- `IGNORED`
  - Last commit has `#scm-ignore` or no commits at all
- `MODIFIED`
  - Last commit has no `#scm-ver`
- `BRANCHED`
  - Last commit has `#scm-ver`
  
# `release` branch
- `MISSING`
  - Branch does not exist
- `TAGGED`
  - Tag exist
- `BUILT`
  - Last commit has `#scm-built`
- `MDEPS_TAGGED`   
  - Every mdep is built and tagged
- `MDEPS_FROZEN` 
  - All mdeps (if any) have fixed version
- `BRANCHED`
  - Not all mdeps have fixed version
