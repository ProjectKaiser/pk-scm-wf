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
  - Tag exist, no commits after tag but `#scm-ver` or `#scm-ignore`
  - is tag with current version exists? -> is the tag points to prev-head commit? => is the last commit is `#scm-ver`? => TAGGED
- `MDEPS_PATCHES_ACTUAL`   
  - Tag which corresponds to `mdeps` exists and points to head-1 (NOTE that newer minor versions of components are ignored)
- `MDEPS_FROZEN` 
  - All mdeps (if any) have fixed version
- `BRANCHED`
  - Not all mdeps have fixed version
