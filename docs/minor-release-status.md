- `CR`: current minor release,  `develop`.version - minor(1)
- `CRB`: current release branch
- `RB`: release branch

# EXTENDED STATUS 

Extended status calculation is introduced as a way to avoid multiple visits of subcomponents repositories. Every subcomponent repository is visitied only once, each visit fetches all info needed for all calculations.

  - `Coords`
  - `Status`: denotes next action which should be undertaken to finish build: {FORK, LOCK (dependencies), BUILD_MDEPS, ACTUALIZE_PATCHES, BUILD, DONE}
  - `nextVersion`. FORK: from `develop` (without -SNAPSHOT, 0-patched), otherwise from RB
  - Map<Coords, ExtendedStatusTreeNode> `subComponents`


# MINOR BUILD STATUS

Status denotes next action which should be undertaken to finish minor build: {FORK, LOCK, BUILD_MDEPS, ACTUALIZE_PATCHES, BUILD, DONE}

- If version is not locked: FORK needed? => FORK
- If version is locked: mdeps extended status is calculated using mdeps from particular RB
- CRB.version.patch >0 => DONE
- mdeps are not locked => LOCK
- Any component is not in DONE status => BUILD_MDEPS
- Any component has patch which is greater than one mentioned in `mdeps` => ACTUALIZE_PATCHES
- If none of above : BUILD

# FORK NEEDED?

{YES, NO}

- `CRB` does not exist => YES
- `CRB`.version.patch == 0 => NO
- `develop` branch has valuable commits => YES
- Any mdep needs FORK => YES
- Versions in `mdeps` does NOT equal to components CR versions => YES (means that all is built but some sub-component has newer minor or patch)
- NO

This procedure also calculates `nextVersion` and `subComponents`.  If CRB exists and version.patch == 0 mdeps are taken from CRB otherwise from `develop`

# PATCH BUILD STATUS

Status denotes next action which should be undertaken to finish patch build: {ACTUALIZE_PATCHES, BUILD, DONE}

- RB does not exist or RB.patch < 1 => ERROR, show error on status command
- mdeps are not locked => ERROR
- mdeps extended status is calculated using mdeps from particular RB
- Any component is not in DONE status => BUILD_MDEPS
- Any component has patch which is greater than one mentioned in `mdeps` => ACTUALIZE_PATCHES
- No valuable commits after last tag => DONE
- If none of above : BUILD

# See Also
- [Wrongly refactored](https://github.com/scm4j/scm4j-releaser/blob/eafe1330dd7076d7e9c1c41dfdbb7dc9e85a6afb/docs/minor-release-status.md)
