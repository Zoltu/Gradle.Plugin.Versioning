[![Build status](https://ci.appveyor.com/api/projects/status/x1xa4ilqfllewosp/branch/master?svg=true)](https://ci.appveyor.com/project/Zoltu/gradle-plugin-versioning/branch/master)
![License](https://img.shields.io/github/license/Zoltu/Gradle.Plugin.Versioning.svg)

# Gradle.Plugin.Versioning

[![Join the chat at https://gitter.im/Zoltu/Gradle.Plugin.Versioning](https://badges.gitter.im/Zoltu/Gradle.Plugin.Versioning.svg)](https://gitter.im/Zoltu/Gradle.Plugin.Versioning?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
A Gradle plugin for automatically generating versions from git tags and commits.

## Usage
1. Add the plugin to your build.gradle as shown [here](https://plugins.gradle.org/plugin/com.zoltu.git-versioning).
2. Tag your repository (anywhere in history) with a tag named like `v1.2` or a semantic version tag like `v1.2.3-tag1.tag2` or something in between like `v1.2-tag1.tag2`
3. Remove the `version` property from your `build.gradle` file.

## How it Works
When you run gradle, it uses jGit to execute `git describe --long` (technically: `git.describe().setLong(true).call)`) to create a string containing the nearest tag looking like `v1.2`, the number of commits since that tag, and the short sha.  The plugin then splits that apart and sets the project version to `{major}.{minor}.{count}` or `{major}.{minor}.{patch}-{tags}.{count}` or `{major}.{minor}.{count}-{tags}` (depending on what your git tag looks like).  It also sets the version in the jar manifest (if you are building a JAR):
* `Implementation-Version`: `{major}.{minor}.{count}` or `{major}.{minor}.{patch}-{tags}.{count}` or `{major}.{minor}.{count}-{tags}`
* `Implementation-Sha`: `{sha}`
* `Specification-Version`: `{major}.{minor}`
