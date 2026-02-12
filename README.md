<p align="center">
    <a href="https://www.lfdecentralizedtrust.org/projects/identus">
        <img src="https://raw.githubusercontent.com/hyperledger-identus/docs/refs/heads/main/static/img/graphics/identus-hero.svg" alt="identus-logo" height="99px" />
    </a>
</p>

# Edge Agent SDK - Kotlin Multiplatform (Android/JVM)

[![Coverage Status](https://coveralls.io/repos/github/hyperledger-identus/sdk-kmp/badge.svg?branch=main)](https://coveralls.io/github/hyperledger-identus/sdk-kmp?branch=main)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=hyperledger-identus_sdk-kmp&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=hyperledger-identus_sdk-kmp)
[![OpenSSF Best Practices](https://www.bestpractices.dev/projects/11735/badge)](https://www.bestpractices.dev/projects/11735)
[![OpenSSF Scorecard](https://api.scorecard.dev/projects/github.com/hyperledger-identus/sdk-kmp/badge)](https://scorecard.dev/viewer/?uri=github.com/hyperledger-identus/sdk-kmp)

[![GitHub release](https://img.shields.io/github/v/release/hyperledger-identus/sdk-kmp)](https://github.com/hyperledger-identus/sdk-kmp/releases)
[![Discord](https://img.shields.io/discord/905194001349627914?label=discord)](https://discord.com/channels/905194001349627914/1230596020790886490)

# Welcome to Edge Agent SDK KMP (Kotlin Multiplatform)

The following will explain how to use the SDK in your project, how to prepare your development environment if you wish to contribute and some basic considerations around the project.

This SDK provides a library and documentation for developers to build Android and JVM-connected SSI applications with Identus.

## Basic considerations

### Edge Agent

Edge Agent is a self-sovereign identity (SSI) platform and service suite for verifiable data and digital identity. Built on Cardano, it offers core infrastructure for issuing DIDs (Decentralized identifiers) and verifiable credentials, alongside tools and frameworks to help expand your ecosystem.
The complete platform is separated into multiple repositories:

* [sdk-swift](https://github.com/hyperledger-identus/sdk-swift/) - Repo that implements Edge Agent for Apple platforms in Swift.
* [sdk-ts](https://github.com/hyperledger-identus/sdk-ts/) - Repo that implements Edge Agent for Browser and Node.js platforms in Typescript.
* [identus-cloud-agent](https://github.com/hyperledger-identus/cloud-agent/) - Repo that contains the platform Building Blocks.
* [mediator](https://github.com/hyperledger-identus/mediator/) - Repo for DIDComm V2 Mediator.

### Modules / APIs

Edge Agent SDK KMP provides the following building blocks to create, manage and resolve decentralized identifiers, issue, manage and verify verifiable credentials, establish and manage trusted, peer-to-peer connections and interactions between DIDs, and store, manage, and recover verifiable data linked to DIDs.

* __Apollo__: Building block that provides a suite of cryptographic operations.
* __Castor__: Building block that provides a suite of DID operations in a user-controlled manner.
* __Pollux__: Building block that provides a suite of credential operations in a privacy-preserving manner.
* __Mercury__: Building block that provides a set of secure, standards-based communications protocols in a transport-agnostic and interoperable manner.
* __Pluto__: Building block that provides an interface for storage operations in a portable, storage-agnostic manner.
* __EdgeAgent__: EdgeAgent, using all the building blocks, provides an agent that can provide a set of high-level DID functionalities.

## Getting started

### Setup

To get started with the Edge Agent SDK KMP, you can set up the SDK and start a new project or integrate it into an existing project. Before you start, make sure you have the following installed on your development machine:

- Android: API level 21 and above.
- Kotlin 1.9.24 or later.
- JVM: 17 or later.

### Integrating the SDK in an existing project

To integrate the SDK into an existing project, you have to import the SDK into your project:

```kotlin
implementation("org.hyperledger.identus.sdk:<latest version>")
```

<!-- TAG_PLATFORMS -->
[badge-platform-android]: http://img.shields.io/badge/-android-6EDB8D.svg?style=flat
[badge-platform-jvm]: http://img.shields.io/badge/-jvm-DB413D.svg?style=flat
