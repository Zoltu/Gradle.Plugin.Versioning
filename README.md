[![Build status](https://ci.appveyor.com/api/projects/status/x1xa4ilqfllewosp/branch/master?svg=true)](https://ci.appveyor.com/project/Zoltu/gradle-plugin-versioning/branch/master)
![License](https://img.shields.io/github/license/Zoltu/Gradle.Plugin.Versioning.svg)

# Gradle.Plugin.Versioning
A Gradle plugin for automatically generating versions from git tags and commits.

## Usage
1. Add the plugin to your build.gradle as shown [here](https://plugins.gradle.org/plugin/com.zoltu.gradle.plugin.git-versioning).
2. Tag your repository (anywhere in history) with a tag named like `v1.2`.
3. Remove the `version` property from your `build.gradle` file.

## How it Works
When you run gradle, it uses jGit to execute `git describe --long` (technically: `git.describe().setLong(true).call)`) to create a string containing the nearest tag looking like `v1.2`, the number of commits since that tag, and the short sha.  The plugin then splits that apart and sets the project version to `{major}.{minor}.{count}`.  It also sets the version in the jar manifest (if you are building a JAR):
* `Implementation-Version`: `{major}.{minor}.{count}`
* `Implementation-Sha`: `{sha}`
* `Specification-Version`: `{major}.{minor}`
