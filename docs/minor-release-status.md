- `CR`: current minor release,  `develop`.version - minor(1)
- `CRB`: current release branch
- `RB`: release branch

# MINOR BUILD STATUS

Status denotes next action which should be undertaken to finish minor build: {FORK, FREEZE, BUILD_MDEPS, ACTUALIZE_PATCHES, BUILD, DONE}

Aux calculations

- `WB`: branch where to get mdeps from
  - if version not locked 
    - if CRB exists - CRB
    - else `develop`
  - else RB for given version
- `mdeps`: are taken from `WB`
- `subComponents` extended status are calculated using `mdeps`

Status calculation

- If version is not locked: FORK needed? => FORK
- WB.version.patch >0 => DONE
- mdeps are not frozen => FREEZE
- Any component is not in DONE status => BUILD_MDEPS
- Any component has patch which is greater than one mentioned in `mdeps` => ACTUALIZE_PATCHES
- If none of above : BUILD

# FORK NEEDED?

{YES, NO}

- WB.name = `develop` => YES
- WB.version.patch == 0 => NO
- `develop` branch has valuable commits => YES
- Any mdep needs FORK => YES
- Versions in `mdeps` does NOT equal to components CR versions => YES (means that all is built but some sub-component has newer minor or patch)
- NO

# PATCH BUILD STATUS

Status denotes next action which should be undertaken to finish patch build: {ACTUALIZE_PATCHES, BUILD, DONE}

- RB does not exist or RB.patch < 1 => ERROR, show error on status command
- mdeps are not frozen => FREEZE
- Any component is not in DONE status => BUILD_MDEPS
- Any component has patch which is greater than one mentioned in `mdeps` => ACTUALIZE_PATCHES
- No valuable commits after last tag => DONE
- If none of above : BUILD

# EXTENDED STATUS 

Extended status calculation is introduced as a way to avoid multiple visits of subcomponents repositories. Every subcomponent repository is visitied only once, each visit fetches all info needed for all calculations.

  - Coords
  - Status
  - wbVersion. Version from `WB`, no decrements, -SNAPSHOT is truncated
  - Map<Coords, ExtendedStatusTreeNode> subComponents
