# hydra tutor

`hydra tutor` is a java web application for experimenting with
 components of the hydra in isolation.


## Building

Assuming you have [Apache Maven](http://maven.apache.org/) installed
and configured:

    mvn package

Should compile and build jars.  Hydra and all its dependencies should be
available on maven central.

## Running

`runserver.sh`

### Filter Tutor

Available at `http://localhost:8100/filter.html`.
Click on examples to populate the session with several examples.
Click one of the examples to populate the filter and input boxes.
Select Run to run the filter. The reset button will reset the internal
state of any filters that maintain persistent state.

## License

hydra tutor is released under the Apache License Version 2.0.  See
[Apache](http://www.apache.org/licenses/LICENSE-2.0) or the LICENSE
for details.
