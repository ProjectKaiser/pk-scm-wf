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
  - Tag which corresponds to version.minor.patch-1 exists and points to head-1 (no commits after last patch but `#scm-ver`)
- `MDEPS_PATCHES_ACTUAL`
   - For every mdep release branch which corresponds to `mdeps` is `TAGGED`
- `MDEPS_FROZEN`
  - All mdeps (if any) have fixed version
- `BRANCHED`
  - Not all mdeps have fixed version
