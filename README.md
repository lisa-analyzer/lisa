# LiSA #

[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
![buildbadge](https://github.com/UniVE-SSV/lisa/workflows/Gradle%20Build/badge.svg) 

LiSA (Library for Static Analysis) aims to ease the creation and implementation of static analyzers based on the Abstract Interpretation theory.
LiSA provides an analysis engine that works on a generic and extensible control flow graph representation of the program to analyze. Abstract interpreters in LiSA are built 
for analyzing such representation, providing a unique analysis infrastructure for all the analyzers that will rely on it.

Building an analyzer upon LiSA boils down to writing a parser for the language that one aims to analyze, translating the source code or the compiled code towards 
the control flow graph representation of LiSA. Then, simple checks iterating over the results provided by the semantic analyses of LiSA can be easily defined to translate 
semantic information into warnings that can be of value for the final user. 

## Release plan ##

We foresee at least five alpha releases that are meant as intermediate building blocks for starting to experiment with analyzers on several languages. 

| Version | Release date | Contents |
| --- | --- | --- |
| 0.1a1 | Mid Oct. 2020 | First draft of the prototype of the structure of the library, with syntactic checkers (no lattices, semantic domain, call graphs, …) |
| 0.1a2 | Mid Nov. 2020 | Complete prototype of the structure of the library, including the definition of call graphs and heap abstractions, definition of first numerical value analyses (interface with apron) |
| 0.1a3 | Mid Dec. 2020 | Implementations of heap abstractions |
| 0.1a4 | Mid Jan. 2021 | Call graph abstractions |
| 0.1a5+ | Feb. 2021 onwards | More value analyses (strings, …) and heap analyses (aliasing, sharing, …) |
| 1.0 | TBD | First stable version |

The 1.0 release will be delivered as we reach a stable version of all the main ingredients for the analysis (call graphs and abstractions) as well as a flexible enough control flow graph representation that enables the modeling of most languages.

## Contributing ##

LiSA is developed and maintained by the [Software and System Verification (SSV)](https://ssv.dais.unive.it/) group @ Università Ca' Foscari in Venice, Italy. 
External contributions are always welcome! Check out our [contributing guidelines](./CONTRIBUTING.md) for information on how to contribute to LiSA.

## Building LiSA ##

LiSA comes as a gradle 6.0 project. Building LiSA boils down to executing three simple commands.

**Mac/Linux**
```
git clone https://github.com/UniVE-SSV/lisa.git
cd lisa/lisa
./gradlew build
```

**Windows**
```
git clone https://github.com/UniVE-SSV/lisa.git
cd lisa\lisa
.\gradlew.bat build
```

The `build` task ensures that everything (from code generation to compilation, packaging and test execution) works fine. If the above commands succeed, then everyhing is set.

### Using Eclipse ###

The [Gradle IDE Pack](https://marketplace.eclipse.org/content/gradle-ide-pack) plugin for Eclipse can manage all the integration between the build system and the IDE.
When installed, create the project for LiSA with `File -> Import... -> Existing Gradle project`, and select the folder where LiSA was cloned into.

The Gradle build (the same executed above with the command lines and that is executed on every commit) can be executed from the Gradle Tasks view (`Window -> Show View -> Other... -> Gradle Tasks`)
by double clicking on the `build -> build` task under the `lisa` project (if it does not appear in the list of projects of the Gradle Tasks view, click on the refresh icon in the top-right of the view itself).

**Caution**: sometimes (e.g., when adding new dependencies to the project) the plugin does not automatically refresh the Gradle configuration, and thus the build might fail
due to missing dependencies or to files not being generated. If this happens, right click on the `lisa` project inside the `Project Explorer` (`Window -> Show View -> Other... -> Project Explorer`) view 
or inside the `Package Explorer` view (`Window -> Show View -> Other... -> Package Explorer`) and select `Gradle -> Refresh Gradle project`.

## Using LiSA ##

LiSA operates on an intermediate representation based on control flow graphs, end exposes a set of interfaces that can be inherited for defining custom checks that generate warnings.
The basic workflow for a program using LiSA `v0.1a1` is the following:

```java
// create a new instance of LiSA
LiSA lisa = new LiSA();

// create a control flow graph to analyze
CFG cfg = new CFG(...);
// initialize the cfg
// add the cfg to the analysis
lisa.addCFG(cfg);

// add a syntactic check to execute during the analysis
lisa.addSyntacticCheck(new SyntacticCheck() {...} );

// start the analysis
lisa.run();	
```
