All statuses are calculated top-down

# `develop` branch
- `IGNORED`
  - Lst commit has `#scm-ignore`
- `MODIFIED`
  - Last commit has no `#scm-ver`
- `BRANCHED`
  - Last commit has `#scm-ver`
  
# `release` branch
- MISSED
  - Branch does not exist
- TAGGED
  - Tag exist
- BUILT
  - Last commit has `#scm-built`
- MDEPS_TAGGED
  - All mdeps (if any) have version and their release branches tagged 
- MDEPS_FROZEN 
  - All mdeps (if any) has fixed version
