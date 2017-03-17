# Preface #

This document describes the functionality provided by the XL Release Flowdock plugin.

See the [XL Release Reference Manual](https://docs.xebialabs.com/xl-release/4.5.x/reference_manual.html) for background information on XL Release and release orchestration concepts.

# CI status #

[![Build Status][xlr-flowdock-plugin-travis-image] ][xlr-flowdock-plugin-travis-url]
[![Codacy][xlr-flowdock-plugin-codacy-image] ][xlr-flowdock-plugin-codacy-url]
[![Code Climate][xlr-flowdock-plugin-code-climate-image] ][xlr-flowdock-plugin-code-climate-url]
[![License: MIT][xlr-flowdock-plugin-license-image] ][xlr-flowdock-plugin-license-url]


[xlr-flowdock-plugin-travis-image]: https://travis-ci.org/xebialabs-community/xlr-flowdock-plugin.svg?branch=master
[xlr-flowdock-plugin-travis-url]: https://travis-ci.org/xebialabs-community/xlr-flowdock-plugin
[xlr-flowdock-plugin-codacy-image]: https://api.codacy.com/project/badge/Grade/0fc277a894eb4cf1b286fcdd5e770768
[xlr-flowdock-plugin-codacy-url]: https://www.codacy.com/app/joris-dewinne/xlr-flowdock-plugin
[xlr-flowdock-plugin-code-climate-image]: https://codeclimate.com/github/xebialabs-community/xlr-flowdock-plugin/badges/gpa.svg
[xlr-flowdock-plugin-code-climate-url]: https://codeclimate.com/github/xebialabs-community/xlr-flowdock-plugin
[xlr-flowdock-plugin-license-image]: https://img.shields.io/badge/License-MIT-yellow.svg
[xlr-flowdock-plugin-license-url]: https://opensource.org/licenses/MIT



# Overview #

The XL Release Flowdock plugin is a XL Release plugin that adds capability for sending message to a Flowdock inbox.

# Requirements #

* **Requirements**
	* **XL Release** 4.5.0

# Installation #

Place the plugin JAR file into your `SERVER_HOME/plugins` directory.

# Usage #

1. Go to `Settings - Configuration - Flowdock: Configuration`
   ![Flowdock configuration part 1](images/Flowdock_configuration_1.png?raw=true "Flowdock configuration part 1")
2. Add a new configuration
   ![Flowdock configuration part 2](images/Flowdock_configuration_2.png?raw=true "Flowdock configuration part 2")
3. Provide Title (Can be anything), API url (eg: https://api.flowdock.com), Flow token (See your Flow configuration in Flowdock) and enable or disable the Flowdock configuration.
   ![Flowdock configuration part 3](images/Flowdock_configuration_3.png?raw=true "Flowdock configuration part 3")
