[![Build status](https://ci.appveyor.com/api/projects/status/x1xa4ilqfllewosp/branch/master?svg=true)](https://ci.appveyor.com/project/Zoltu/gradle-plugin-versioning/branch/master)
![License](https://img.shields.io/github/license/Zoltu/Gradle.Plugin.Versioning.svg)
[![Join the chat at https://gitter.im/Zoltu/Gradle.Plugin.Versioning](https://badges.gitter.im/Zoltu/Gradle.Plugin.Versioning.svg)](https://gitter.im/Zoltu/Gradle.Plugin.Versioning?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

# Gradle.Plugin.Versioning
A Gradle plugin for automatically generating versions from git tags and commits.

## Usage
1. Add the plugin to your build.gradle as shown [here](https://plugins.gradle.org/plugin/com.zoltu.git-versioning).
2. Tag your repository (anywhere in history) with a tag named like `v1.2` or a semantic version tag like `v1.2.3-tag1.tag2` or something in between like `v1.2-tag1.tag2`
3. Remove the `version` property from your `build.gradle` file
4. (Optional) Use `gradle version` or `./gradlew version` to see the current `version`.

## How it Works
When you run gradle, it uses jGit to execute `git describe --long` (technically: `git.describe().setLong(true).call)`) to create a string containing the nearest tag looking like `v1.2`, the number of commits since that tag, and the short sha.  The plugin then splits that apart and sets the project version to `{major}.{minor}.{count}` or `{major}.{minor}.{patch}-{tags}.{count}` or `{major}.{minor}.{count}-{tags}` (depending on what your git tag looks like).  It also sets the version in the jar manifest (if you are building a JAR):
* `Implementation-Version`: `{major}.{minor}.{count}` or `{major}.{minor}.{patch}-{tags}.{count}` or `{major}.{minor}.{count}-{tags}`
* `Implementation-Sha`: `{sha}`
* `Specification-Version`: `{major}.{minor}`

## Advanced Usage
If you want more control over how you version, you can directly access the [`VersionInfo`](https://github.com/Zoltu/Gradle.Plugin.Versioning/blob/master/src/main/kotlin/com/zoltu/gradle/plugin/VersionInfo.kt) object via the gradle project: `project.ZoltuGitVersioning.major.  Inside of your `build.gradle` this might look something like this:
```groovy
def versionInfo = ZoltuGitVersioning.versionInfo
print "${versionInfo.major}.${versionInfo.minor}.${versionInfo.commitCount}"
```

Of course, you can do more advanced logic such as squashing the three values into a single number like:
```groovy
def versionInfo = ZoltuGitVersioning.versionInfo
def versionCode =
	versionInfo.major.toInteger() * 10000
	+ versionInfo.minor.toInteger() * 100
	+ versionInfo.patch.toInteger()
```

If you want to perform your own processing on the version information produced by the plugin, you may do so as follows:
```groovy
// With input tag '1.0.0-36'
ZoltuGitVersioning.customVersionToString { versionInfo ->
		"${versionInfo.major}.${versionInfo.minor}.${versionInfo.patch}.${versionInfo.commitCount}"
}
```
Will produce: '1.0.0.36'
