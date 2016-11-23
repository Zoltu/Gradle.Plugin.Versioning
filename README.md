[![Build status](https://ci.appveyor.com/api/projects/status/x1xa4ilqfllewosp/branch/master?svg=true)](https://ci.appveyor.com/project/Zoltu/gradle-plugin-versioning/branch/master)
![License](https://img.shields.io/github/license/Zoltu/Gradle.Plugin.Versioning.svg)
[![Join the chat at https://gitter.im/Zoltu/Gradle.Plugin.Versioning](https://badges.gitter.im/Zoltu/Gradle.Plugin.Versioning.svg)](https://gitter.im/Zoltu/Gradle.Plugin.Versioning?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

A Gradle plugin for automatically generating versions from git tags and commits.

## Usage
1. Add the plugin to your `build.gradle` file as shown [here](https://plugins.gradle.org/plugin/com.zoltu.git-versioning).
2. Tag your repository (anywhere in history) with a tag named like `v1.2`.
3. Remove the `version` property from your `build.gradle` file.
4. (Optional) Use `gradle version` or `./gradlew version` to see the current `version`.

## Advanced Usage
If you want more control over how you version, you can directly access the [`VersionInfo`](https://github.com/Zoltu/Gradle.Plugin.Versioning/blob/master/src/main/kotlin/com/zoltu/gradle/plugin/VersionInfo.kt) object inside your gradle script.
```groovy
def versionInfo = ZoltuGitVersioning.versionInfo
print "${versionInfo.major}.${versionInfo.minor}.${versionInfo.commitCount}"
```
Additionally, you may also change how the plugin processes version information by specifying custom processors.

### Custom Describe Processor
The plugin no longer supports semantic versioning out-of-box. Instead, you may specify a custom provider function which accepts the result of `git describe` and returns a map of each versioning component like so:
```groovy
ZoltuGitVersioning {
    customDescribeProcessor { describeResults ->
        def matcher = (describeResults =~ /(?<major>[0-9]+?)\.(?<minor>[0-9]+?)(?:\-(?<tags>[0-9A-Za-z\.\-]+))?\-(?<commitCount>[0-9]+?)\-g(?<sha>[a-zA-Z0-9]+)/)
        matcher.matches()
        [
            major: matcher.group('major'),
            minor: matcher.group('minor'),
            commitCount: matcher.group('commitCount'),
            sha: matcher.group('sha'),
            tags: matcher.group('tags')
        ]
    }
}
```

### Custom Output Processor
The default implementation merely assigns the value of `VersionInfo.toString()` as the project version. If you want to perform your own processing on the version information produced by the plugin, you may do so as follows:
```groovy
// With input tag '1.0.0-36'
ZoltuGitVersioning {
    customVersionToString { versionInfo ->
        "${versionInfo.major}.${versionInfo.minor}.${versionInfo.patch}.${versionInfo.commitCount}"
    }
}
```
Will produce: '1.0.0.36'.

## Notes
Please keep in mind that any calls to `ZoltuGitVersioning.versionInfo` must occur only *after* declaring any custom processors.
