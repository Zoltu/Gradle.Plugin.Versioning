package com.zoltu.gradle.plugin

import org.codehaus.groovy.runtime.MethodClosure
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import java.io.File

class GitVersioning : Plugin<Project> {
	override fun apply(project: Project?) {
		if (project == null) return;

		val versionInfo = getVersionInfo(project.rootDir)
		setProjectVersion(project, versionInfo)
		setJarManifestVersion(project, versionInfo)
	}

	fun getVersionInfo(rootDirectory: File): VersionInfo {
		val repository = FileRepositoryBuilder().findGitDir(rootDirectory)!!.build()!!
		val git = Git.wrap(repository)!!
		val describeResults = git.describe().setLong(true).call() ?: throw Exception("Your repository must have at least one tag in it with the format `v1.2`.")
		val regex = Regex("""v([0-9]+)\.([0-9]+)\-([0-9]+)\-g(.*)""")
		val match = regex.matchEntire(describeResults) ?: throw Exception("Git describe didn't return the expected format.")
		val major = match.groups[1]?.value ?: throw Exception("Git describe matched the expected format but the matcher didn't return group 1.")
		val minor = match.groups[2]?.value ?: throw Exception("Git describe matched the expected format but the matcher didn't return group 2.")
		val patch = match.groups[3]?.value ?: throw Exception("Git describe matched the expected format but the matcher didn't return group 3.")
		val sha = match.groups[4]?.value ?: throw Exception("Git describe matched the expected format but the matcher didn't return group 4.")
		return VersionInfo(major, minor, patch, sha)
	}

	fun setProjectVersion(project: Project, versionInfo: VersionInfo) {
		project.version = versionInfo.toString()
	}

	fun setJarManifestVersion(project: Project, versionInfo: VersionInfo) {
		val closure = object {
			fun apply(jar: Jar) {
				val attributes = jar.manifest?.attributes ?: throw Exception("The Jar task has no manifest.")
				attributes.put("Implementation-Version", versionInfo.toString())
				attributes.put("Implementation-Sha", versionInfo.sha)
				attributes.put("Specification-Version", "${versionInfo.major}.${versionInfo.minor}")
			}
		}
		(project.tasks ?: throw Exception("The project has no tasks.")).withType(Jar::class.java, MethodClosure(closure, "apply"))
	}
}
