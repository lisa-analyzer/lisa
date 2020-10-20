# LiSA 

[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
![buildbadge](https://github.com/UniVE-SSV/lisa/workflows/Gradle%20Build/badge.svg) 

LiSA (Library for Static Analysis) aims to ease the creation and implementation of static analyzers based on the Abstract Interpretation theory.
LiSA provides an analysis engine that works on a generic and extensible control flow graph representation of the program to analyze. Abstract interpreters in LiSA are built 
for analyzing such representation, providing a unique analysis infrastructure for all the analyzers that will rely on it.

Building an analyzer upon LiSA boils down to writing a parser for the language that one aims to analyze, translating the source code or the compiled code towards 
the control flow graph representation of LiSA. Then, simple checks iterating over the results provided by the semantic analyses of LiSA can be easily defined to translate 
semantic information into warnings that can be of value for the final user. 

For more information, documentation and useful guides, refer to the [project website](https://unive-ssv.github.io/lisa/)!

## Contributing 

LiSA is developed and maintained by the [Software and System Verification (SSV)](https://ssv.dais.unive.it/) group @ Università Ca' Foscari in Venice, Italy. 
External contributions are always welcome! Check out our [contributing guidelines](./CONTRIBUTING.md) for information on how to contribute to LiSA.

## Release plan 

We foresee at least five alpha releases that are meant as intermediate building blocks for starting to experiment with analyzers on several languages. 

| Version | Release date | Contents |
| --- | --- | --- |
| [0.1a1](https://github.com/UniVE-SSV/lisa/releases/tag/v0.1a1) | Oct. 19, 2020 | First draft of the prototype of the structure of the library, with syntactic checkers (no lattices, semantic domain, call graphs, …) |
| 0.1a2 | Mid Nov. 2020 | Complete prototype of the structure of the library, including the definition of call graphs and heap abstractions, definition of first numerical value analyses (interface with apron) |
| 0.1a3 | Mid Dec. 2020 | Implementations of heap abstractions |
| 0.1a4 | Mid Jan. 2021 | Call graph abstractions |
| 0.1a5+ | Feb. 2021 onwards | More value analyses (strings, …) and heap analyses (aliasing, sharing, …) |
| 1.0 | TBD | First stable version |

The 1.0 release will be delivered as we reach a stable version of all the main ingredients for the analysis (call graphs and abstractions) as well as a flexible enough control flow graph representation that enables the modeling of most languages.
