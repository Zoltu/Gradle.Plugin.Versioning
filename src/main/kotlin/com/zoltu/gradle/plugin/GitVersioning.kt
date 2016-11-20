package com.zoltu.gradle.plugin

import org.codehaus.groovy.runtime.MethodClosure
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import java.io.File
import java.util.function.Function
import kotlin.properties.Delegates.observable

class GitVersioning : Plugin<Project> {
	companion object {
		val regexSimpleVersionInfo = Regex("""[v]?([0-9]+?)\.([0-9]+?)(?:\-([0-9A-Za-z\.\-]+))?\-([0-9]+?)\-g(.*)""")
		val regexSemanticVersionInfo = Regex("""[v]?([0-9]+?)\.([0-9]+?)\.([0-9]+?)(?:\-([0-9A-Za-z\.\-]+))?\-([0-9]+?)\-g([a-zA-Z0-9]+?)""")
	}

	private lateinit var configuration: Configuration

	override fun apply(project: Project?) {
		if (project == null) return

		project.task("version").doLast { println("Version: ${project.version}") }
		val describeResults = getGitDescribeResults(project.rootDir)
		val processVersioning = {
			configuration.versionInfo = getVersionInfo(describeResults)
			setProjectVersion(project, configuration.versionInfo)
			setJarManifestVersion(project, configuration.versionInfo)
		}
		configuration = exposeConfigurationObject(project, processVersioning)
		processVersioning()
	}

	private fun getVersionInfo(describeResults: String): VersionInfo {
		return tryGetCustomVersionInfo(describeResults) ?: tryGetSemanticVersionInfo(describeResults) ?: getSimpleVersionInfo(describeResults)
	}

	private fun getSimpleVersionInfo(describeResults: String): VersionInfo {
		val match = regexSimpleVersionInfo.matchEntire(describeResults) ?: throw Exception("Git describe didn't return the expected format.")
		val major = match.groups[1]?.value ?: throw Exception("Git describe matched the expected format but the matcher didn't return group 1.")
		val minor = match.groups[2]?.value ?: throw Exception("Git describe matched the expected format but the matcher didn't return group 2.")
		val tags = match.groups[3]?.value
		val commitCount = match.groups[4]?.value ?: throw Exception("Git describe matched the expected format but the matcher didn't return group 4.")
		val sha = match.groups[5]?.value ?: throw Exception("Git describe matched the expected format but the matcher didn't return group 5.")
		return VersionInfo(major = major, minor = minor, commitCount = commitCount, sha = sha, tags = tags)
	}

	private fun tryGetSemanticVersionInfo(describeResults: String): VersionInfo? {
		val match = regexSemanticVersionInfo.matchEntire(describeResults) ?: return null

		val major = match.groups[1]?.value ?: return null
		val minor = match.groups[2]?.value ?: return null
		val patch = match.groups[3]?.value ?: return null
		val tags = match.groups[4]?.value
		val commitCount = match.groups[5]?.value ?: return null
		val sha = match.groups[6]?.value ?: return null

		return VersionInfo(major = major, minor = minor, patch = patch, tags = tags, commitCount = commitCount, sha = sha)
	}

	private fun tryGetCustomVersionInfo(describeResults: String): VersionInfo? = configuration.customDescribeProcessor.apply(describeResults)

	private fun getGitDescribeResults(rootDirectory: File): String {
		val repository = FileRepositoryBuilder()
				.findGitDir(rootDirectory)!!
				.apply { gitDir ?: throw Exception("Project must be in a git directory for git-versioning to work.  Recommended solution: git init") }
				.build()!!
		val git = Git.wrap(repository)!!
		if (git.repository.allRefs.count() == 0) throw Exception("Your repository must have at least one commit in the repository for git-versioning to work.  Recommended solution: git commit")
		return git.describe().setLong(true).call() ?: throw Exception("Your repository must have at least one tag in it for git-versioning to work.  Recommended solution: git tag v0.0")
	}

	private fun setProjectVersion(project: Project, versionInfo: VersionInfo) {
		project.version = configuration.customVersionToString.apply(versionInfo)
	}

	private fun setJarManifestVersion(project: Project, versionInfo: VersionInfo) {
		val closure = object {
			@Suppress("unused")
			fun apply(jar: Jar) {
				val attributes = jar.manifest?.attributes ?: throw Exception("The Jar task has no manifest.")
				attributes.put("Implementation-Version", configuration.customVersionToString.apply(versionInfo))
				attributes.put("Implementation-Sha", versionInfo.sha)
				attributes.put("Specification-Version", "${versionInfo.major}.${versionInfo.minor}")
			}
		}
		(project.tasks ?: throw Exception("The project has no tasks.")).withType(Jar::class.java, MethodClosure(closure, "apply"))
	}

	private fun exposeConfigurationObject(project: Project, configurationChangeCallback: () -> Unit) = project.extensions.create("ZoltuGitVersioning", Configuration::class.java, configurationChangeCallback)

	open class Configuration(private val configurationChangeCallback: () -> Unit) {
		companion object {
			@Suppress("unused")
			const val NAME = "versioning"
			private val defaultVersionToString = Function<VersionInfo, String> { versionInfo: VersionInfo ->
				val suffix = if (versionInfo.tags != null && versionInfo.patch != null) {
					// semantic versioning
					"${versionInfo.patch}-${versionInfo.tags}.${versionInfo.commitCount}"
				} else if (versionInfo.patch != null) {
					// semantic versioning
					"${versionInfo.patch}-${versionInfo.commitCount}"
				} else if (versionInfo.tags != null) {
					// simple versioning
					"${versionInfo.commitCount}-${versionInfo.tags}"
				} else {
					// simple versioning
					"${versionInfo.commitCount}"
				}
				"${versionInfo.major}.${versionInfo.minor}.$suffix".toString()
			}
			private val defaultCustomDescribeProcessor = Function<String, VersionInfo?> { null }
		}
		/**
		 * Contains version information for consumption.
		 */
		lateinit var versionInfo: VersionInfo

		/**
		 * A custom describe processor that will take in a string as input and return a VersionInfo object as output.
		 */
		var customDescribeProcessor: Function<String, VersionInfo?> by observable(defaultCustomDescribeProcessor, { property, old, new -> configurationChangeCallback() })

		/**
		 * A custom toString for VersionInfo that will take in a VersionInfo as input and return a string as output.
		 */
		var customVersionToString: Function<VersionInfo, String> by observable(defaultVersionToString, { property, old, new -> configurationChangeCallback() })
	}
}
