- `CR`: current release
- `CRB`: current release branch



# NEED FORK?

1) `CRB` does not exist => YES
2) `CRB`.version.patch == 0 => NO
3) `develop` branch has valuable commits => YES
4) Any mdep needs FORK => YES
5) `mdeps` are  frozen and versions in `mdeps` equal to components CR versions => NO
6) YES

# MINOR BUILD STATUS

1) FORK needed? => FORK
2) CR.version.patch == 0 => NONE
3) mdeps are not frozen => FREEZE
4) Any component has patch which more than one mentioned in `mdeps` => ACTUALIZE_PATCHES
5) CRB.version.patch == 0 => BUILD
6) CRB has valuable commits after `CRB.version.patch-1` tag => BUILD
7) NONE

