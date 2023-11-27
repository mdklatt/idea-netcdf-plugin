# NetCDF Plugin for IDEA

[![IDEA version][1]][7]
[![Latest release][2]][3]
[![Test status][4]][5]

[1]: https://img.shields.io/static/v1?label=IDEA&message=2022.1%2B&color=informational
[2]: https://img.shields.io/github/v/release/mdklatt/idea-netcdf-plugin?sort=semver
[3]: https://github.com/mdklatt/idea-netcdf-plugin/releases
[4]: https://github.com/mdklatt/idea-netcdf-plugin/actions/workflows/test.yml/badge.svg
[5]: https://github.com/mdklatt/idea-netcdf-plugin/actions/workflows/test.yml


## Description

<!-- This content is used by the Gradle IntelliJ Plugin. --> 
<!-- Plugin description -->

Tools for inspecting [Network Common Data Form][6] (netCDF) files in any
[JetBrains][7] IDE. Right-click on a file in the *Project* tool window to
access the `NetCDF` context menu, which offers these actions:

### Open in Viewer

Open the file in the *NetCDF* tool window. The `Schema` tab shows the file
structure. Variables selected in this tab will be displayed in the `Data` tab. 
Multiple variables can be selected as long as they have the same dimensions.

The plugin does not yet support pagination, so **beware of attempting to view
large variables**.

### Create CDL File

Write the file header to a [Common Data Language][8] (CDL) file. This is
equivalent to executing `ndcump -h`.

### Create NcML File

Write the file header to a [NetCDF Markup Language][9] (NcML) file.


[6]: https://www.unidata.ucar.edu/software/netcdf
[7]: https://www.jetbrains.com
[8]: https://docs.unidata.ucar.edu/nug/current/netcdf_utilities_guide.html
[9]: https://docs.unidata.ucar.edu/netcdf-java/current/userguide/ncml_overview.html

<!-- Plugin description end -->

## Installation

The latest version is available via a [custom plugin repository][10]. [Releases][3]
include a binary distribution named `idea-ansible-plugin-<version>.zip` that
can be [installed from disk][11].


[10]: https://mdklatt.github.io/idea-plugin-repo
[11]: https://www.jetbrains.com/help/idea/managing-plugins.html#install_plugin_from_disk
