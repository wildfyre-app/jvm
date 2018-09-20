# WildFyre Java API

[![Javadocs](https://bit.ly/2vxyIa5)](https://bit.ly/2OBImBj)
[![Maven Central](https://bit.ly/2AiQT95)](https://bit.ly/2K1PTG4)

## Installation

To install this library for testing..., clone it and ensure you have Java installed (at least version 1.8).

You can check your Java version by running:

    $ java -version

To use this library as a dependency, you WILL BE able to import it from MavenCentral in the future.

## Testing...

Here is the list of commands you can use to check the code (you can replace `./gradlew` by `gradlew.bat` on Windows):

 - `./gradlew clean` -- cleans the build/ directory
 - `./gradlew test` -- runs the unit tests (in the future this will require that the server API is set up!) (the full report can be seen at build/reports/tests/index.html)
 - `./gradlew check` -- runs static code analysis (FindBugs, reports can be found at build/reports/findbugs/)
 - `./gradlew build` -- compiles & runs the tests
 - `./gradlew javadoc` -- exports the javadoc of the project (in build/docs/javadoc/index.html)
 - `./gradlew jar` -- exports the JAR file that can be included in other projects as a dependency (in build/libs)
