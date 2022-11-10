<img src="logo.png" alt="logo" width="300"/>

---

![GitHub](https://img.shields.io/github/license/lisa-analyzer/lisa?color=brightgreen)
![GitHub Workflow Status (branch)](https://img.shields.io/github/workflow/status/lisa-analyzer/lisa/Gradle%20Build/master)
![GitHub release (latest)](https://img.shields.io/github/v/release/lisa-analyzer/lisa?display_name=release&color=brightgreen)
![GitHub last commit](https://img.shields.io/github/last-commit/lisa-analyzer/lisa)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.lisa-analyzer/lisa-sdk?color=brightgreen)](https://search.maven.org/artifact/io.github.lisa-analyzer/lisa-sdk)
[![Javadoc.io](https://javadoc.io/badge2/io.github.lisa-analyzer/lisa-sdk/javadoc.svg)](https://javadoc.io/doc/io.github.lisa-analyzer/lisa-sdk)

LiSA (Library for Static Analysis) aims to ease the creation and implementation of static analyzers based on the Abstract Interpretation theory.
LiSA provides an analysis engine that works on a generic and extensible control flow graph representation of the program to analyze. Abstract interpreters in LiSA are built 
for analyzing such representation, providing a unique analysis infrastructure for all the analyzers that will rely on it.

Building an analyzer upon LiSA boils down to writing a parser for the language that one aims to analyze, translating the source code or the compiled code towards 
the control flow graph representation of LiSA. Then, simple checks iterating over the results provided by the semantic analyses of LiSA can be easily defined to translate 
semantic information into warnings that can be of value for the final user. 

For more information, documentation and useful guides, refer to the [project website](https://lisa-analyzer.github.io/)!

## Contributing 

LiSA is developed and maintained by the [Software and System Verification (SSV)](https://ssv.dais.unive.it/) group @ Università Ca' Foscari in Venice, Italy. 
External contributions are always welcome! Check out our [contributing guidelines](./CONTRIBUTING.md) for information on how to contribute to LiSA.

## Releases

Head to the [Releases](https://github.com/lisa-analyzer/lisa/releases) page for the full list of releases. 

### Snapshots

The snapshot of the last commit of the `master` branch is always available on [GitHub Packages](https://github.com/orgs/lisa-analyzer/packages?repo_name=lisa). Be aware that you will need a personal access token to use packages as server for resolving dependencies. You can refer to the official GitHub guide for [Maven](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry#authenticating-to-github-packages) or [Gradle](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#authenticating-to-github-packages) for more information.
