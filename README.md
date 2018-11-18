# WildFyre Java API

[![Javadocs](https://bit.ly/2vxyIa5)](https://bit.ly/2OBImBj)
[![Maven Central](https://bit.ly/2AiQT95)](https://bit.ly/2K1PTG4)

## Making a project using this library

This library is available on [MavenCentral](https://search.maven.org/search?q=g:net.wildfyre),
you can find the different ways to import it there.

## Collaborating

Bugs, tasks and issues are discussed [here](https://phabricator.wildfyre.net/tag/libwf-java/),
the source code is available [here](https://phabricator.wildfyre.net/source/libwf-java/).

Here is the list of commands you can use to check the code (you can replace `./gradlew` by `gradlew.bat` on Windows):

 - `./gradlew clean` -- cleans the build/ directory
 - `./gradlew test` -- runs the unit tests (in the future this will require that the server API is set up!) (the full report can be seen at build/reports/tests/index.html)
 - `./gradlew check` -- runs static code analysis (FindBugs, reports can be found at build/reports/findbugs/)
 - `./gradlew build` -- compiles & runs the tests
 - `./gradlew javadoc` -- exports the javadoc of the project (in build/docs/javadoc/index.html)
 - `./gradlew jar` -- exports the JAR file that can be included in other projects as a dependency (in build/libs)
