# hydra tutor

`hydra tutor` is a java web application for experimenting with
 components of the hydra in isolation.


## Building

Assuming you have [Apache Maven](http://maven.apache.org/) installed
and configured:

    mvn package

Should compile and build jars.  Hydra and all its dependencies should be
available on maven central.

[Berkeley DB Java Edition](http://www.oracle.com/technetwork/database/berkeleydb/overview/index-093405.html)
is used for several core features.  The sleepycat license has strong
copyleft properties that do not match the rest of the project.  It is
set as a non-transitive dependency to avoid inadvertently pulling it
into downstream projects.  In the future hydra should have pluggable
storage with multiple implementations. To include BDB JE when building with
`mvn package` use `-P bdbje`.

## Running

`runserver.sh`

### Filter Tutor

Available at `http://localhost:8100/filter.html`.
Click on examples to populate the session with several examples.
Click one of the examples to populate the filter and input boxes.
Select Run to run the filter. The reset button will reset the internal
state of any filters that maintain persistent state.

### Tree Tutor

Available at `http://localhost:8100/tree.html`.
Enter your data in the input box in a CSV format. The first row
consists of the bundle field names and the subsequent rows each
represent one bundle. Select Build to build the output tree. The
Tree Tutor was developed by 2013 summer intern Evan Spillane.

## License

hydra tutor is released under the Apache License Version 2.0.  See
[Apache](http://www.apache.org/licenses/LICENSE-2.0) or the LICENSE
for details.
