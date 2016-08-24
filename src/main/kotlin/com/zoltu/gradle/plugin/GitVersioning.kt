package com.zoltu.gradle.plugin

import org.codehaus.groovy.runtime.MethodClosure
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import java.io.File

class GitVersioning : Plugin<Project> {
	companion object {
		val regexSimpleVersionInfo = Regex("""v([0-9]+?)\.([0-9]+?)(?:\-([0-9A-Za-z\.\-]+))?\-([0-9]+?)\-g(.*)""")
		val regexSemanticVersionInfo = Regex("""v([0-9]+?)\.([0-9]+?)\.([0-9]+?)(?:\-([0-9A-Za-z\.\-]+))?\-([0-9]+?)\-g([a-zA-Z0-9]+?)""")
	}

	override fun apply(project: Project?) {
		if (project == null) return

		project.task("version").doLast { println("Version: ${project.version}") }

		val describeResults = getGitDescribeResults(project.rootDir)
		val versionInfo = getVersionInfo(describeResults)
		exposeVersionInfoToUser(project, versionInfo)
		setProjectVersion(project, versionInfo)
		setJarManifestVersion(project, versionInfo)
	}

	fun getVersionInfo(describeResults: String): VersionInfo {
		return tryGetSemanticVersionInfo(describeResults) ?: getSimpleVersionInfo(describeResults)
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

	private fun getGitDescribeResults(rootDirectory: File): String {
		val repository = FileRepositoryBuilder().findGitDir(rootDirectory)!!.build()!!
		val git = Git.wrap(repository)!!
		return git.describe().setLong(true).call() ?: throw Exception("Your repository must have at least one tag in it for git-versioning to work.  Recommended solution: tag the initial commit with `v0.0`.")
	}

	private fun setProjectVersion(project: Project, versionInfo: VersionInfo) {
		project.version = versionInfo.toString()
	}

	private fun setJarManifestVersion(project: Project, versionInfo: VersionInfo) {
		val closure = object {
			@Suppress("unused")
			fun apply(jar: Jar) {
				val attributes = jar.manifest?.attributes ?: throw Exception("The Jar task has no manifest.")
				attributes.put("Implementation-Version", versionInfo.toString())
				attributes.put("Implementation-Sha", versionInfo.sha)
				attributes.put("Specification-Version", "${versionInfo.major}.${versionInfo.minor}")
			}
		}
		(project.tasks ?: throw Exception("The project has no tasks.")).withType(Jar::class.java, MethodClosure(closure, "apply"))
	}

	private fun exposeVersionInfoToUser(project: Project, versionInfo: VersionInfo) {
		project.extensions.create("ZoltuGitVersioning", Extension::class.java, versionInfo)
	}

	data class Extension(val versionInfo: VersionInfo)
}
