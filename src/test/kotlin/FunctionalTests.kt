import org.eclipse.jgit.api.Git
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

class FunctionalTests {
	@Rule @JvmField
	val directory = TemporaryFolder()

	@Test
	fun `defaults`() {
		// arrange
		initializeBuildGradle()
		initializeGit()

		// act
		val result = runVersionTaskExpectingSuccess()

		// assert
		assertEquals(result.task(":version").outcome, TaskOutcome.SUCCESS)
		assertTrue(result.output.contains("Version: 1.2.0"))
	}

	@Test
	fun `not a git directory`() {
		// arrange
		initializeBuildGradle()

		// act
		val result = runVersionTaskExpectingFailure()

		// assert
		assertTrue(result.output.contains("Project must be in a git directory for git-versioning to work.  Recommended solution: git init"))
	}

	@Test
	fun `no commits`() {
		// arrange
		Git.init().setDirectory(directory.root).call()
		initializeBuildGradle()

		// act
		val result = runVersionTaskExpectingFailure()

		// assert
		assertTrue(result.output.contains("Your repository must have at least one commit in the repository for git-versioning to work.  Recommended solution: git commit"))
	}

	@Test
	fun `no tags`() {
		// arrange
		Git.init().setDirectory(directory.root).call().commit().setMessage("").call()
		initializeBuildGradle()

		// act
		val result = runVersionTaskExpectingFailure()

		// assert
		assertTrue(result.output.contains("Your repository must have at least one tag in it for git-versioning to work.  Recommended solution: git tag v0.0"))
	}

	@Test
	fun `no matching tag`() {
		// arrange
		initializeGit("my-custom-tag-name")
		initializeBuildGradle()

		// act
		val result = runVersionTaskExpectingFailure()

		// assert
		assertTrue(result.output.contains("Git describe didn't return the expected format."))
	}

	@Test
	fun `semantic version with tags`() {
		// arrange
		initializeGit("v1.2.3-beta-4.5").createFakeCommits(2)
		initializeBuildGradle("""
plugins { id 'com.zoltu.git-versioning' }
ZoltuGitVersioning.customDescribeProcessor = { describeResults ->
		def matcher = (describeResults =~ /v(?<major>[0-9]+?)\.(?<minor>[0-9]+?)\.(?<patch>[0-9]+?)(?:\-(?<tags>[0-9A-Za-z\.\-]+))?\-(?<commitCount>[0-9]+?)\-g(?<sha>[a-zA-Z0-9]+)/)
		matcher.matches()
		[ major: matcher.group('major'), minor: matcher.group('minor'), commitCount: matcher.group('commitCount'), sha: matcher.group('sha'), patch: matcher.group('patch'), tags: matcher.group('tags') ]
}
		""")

		// act
		val result = runVersionTaskExpectingSuccess()

		// assert
		assertTrue(result.output.contains("Version: 1.2.3-beta-4.5.2"))
	}

	@Test
	fun `semantic version without tags`() {
		// arrange
		initializeGit("v1.2.3").createFakeCommits(3)
		initializeBuildGradle("""
plugins { id 'com.zoltu.git-versioning' }
ZoltuGitVersioning.customDescribeProcessor = { describeResults ->
		def matcher = (describeResults =~ /v(?<major>[0-9]+?)\.(?<minor>[0-9]+?)\.(?<patch>[0-9]+?)(?:\-(?<tags>[0-9A-Za-z\.\-]+))?\-(?<commitCount>[0-9]+?)\-g(?<sha>[a-zA-Z0-9]+)/)
		matcher.matches()
		[ major: matcher.group('major'), minor: matcher.group('minor'), commitCount: matcher.group('commitCount'), sha: matcher.group('sha'), patch: matcher.group('patch'), tags: matcher.group('tags') ]
}
		""")

		// act
		val result = runVersionTaskExpectingSuccess()

		// assert
		assertTrue(result.output.contains("Version: 1.2.3-3"))
	}

	@Test
	fun `simple version with tags`() {
		// arrange
		initializeGit("v1.2-tag-1.tag2.4").createFakeCommits(3)
		initializeBuildGradle()

		// act
		val result = runVersionTaskExpectingSuccess()

		// assert
		assertTrue(result.output.contains("Version: 1.2.3-tag-1.tag2.4"))
	}

	@Test
	fun `simple version without tags`() {
		// arrange
		initializeGit("v1.2").createFakeCommits(3)
		initializeBuildGradle()

		// act
		val result = runVersionTaskExpectingSuccess()

		// assert
		assertTrue(result.output.contains("Version: 1.2.3"))
	}

	@Test
	fun `simple version without leading v without tags`() {
		// arrange
		initializeGit("1.2").createFakeCommits(3)
		initializeBuildGradle("""
plugins { id 'com.zoltu.git-versioning' }
ZoltuGitVersioning.customDescribeProcessor = { describeResults ->
		def matcher = (describeResults =~ /(?<major>[0-9]+?)\.(?<minor>[0-9]+?)(?:\-(?<tags>[0-9A-Za-z\.\-]+))?\-(?<commitCount>[0-9]+?)\-g(?<sha>[a-zA-Z0-9]+)/)
		matcher.matches()
		[ major: matcher.group('major'), minor: matcher.group('minor'), commitCount: matcher.group('commitCount'), sha: matcher.group('sha'), tags: matcher.group('tags') ]
}
		""")

		// act
		val result = runVersionTaskExpectingSuccess()

		// assert
		assertTrue(result.output.contains("Version: 1.2.3"))
	}
	@Test
	fun `simple version without leading v with tags`() {
		// arrange
		initializeGit("1.2-tag-1.tag2.4").createFakeCommits(3)
		initializeBuildGradle("""
plugins { id 'com.zoltu.git-versioning' }
ZoltuGitVersioning.customDescribeProcessor = { describeResults ->
		def matcher = (describeResults =~ /(?<major>[0-9]+?)\.(?<minor>[0-9]+?)(?:\-(?<tags>[0-9A-Za-z\.\-]+))?\-(?<commitCount>[0-9]+?)\-g(?<sha>[a-zA-Z0-9]+)/)
		matcher.matches()
		[ major: matcher.group('major'), minor: matcher.group('minor'), commitCount: matcher.group('commitCount'), sha: matcher.group('sha'), tags: matcher.group('tags') ]
}
		""")

		// act
		val result = runVersionTaskExpectingSuccess()

		// assert
		assertTrue(result.output.contains("Version: 1.2.3-tag-1.tag2.4"))
	}

	@Test
	fun `version info ends up in configuration`() {
		// arrange
		initializeGit("v1.2").createFakeCommits(9)
		initializeBuildGradle("""
plugins { id 'com.zoltu.git-versioning' }
println "Commit Count: ${'$'}{ZoltuGitVersioning.versionInfo.commitCount}"
		""")

		// act
		val result = runVersionTaskExpectingSuccess()

		// assert
		assertTrue(result.output.contains("Commit Count: 9"))
	}

	@Test
	fun `custom describe processor`() {
		// arrange
		initializeGit("foo.bar").createFakeCommits(9)
		initializeBuildGradle("""
plugins { id 'com.zoltu.git-versioning' }
ZoltuGitVersioning.customDescribeProcessor = { describeResult ->
		[ major: "apple", minor: "orange", commitCount: "banana", sha: "tomato", patch: "pear" ]
}
		""")

		// act
		val result = runVersionTaskExpectingSuccess()

		// assert
		assertTrue(result.output.contains("Version: apple.orange.pear-banana"))
	}

	@Test
	fun `invalid custom describe processor`() {
		// arrange
		initializeGit("v1.2").createFakeCommits(9)
		initializeBuildGradle("""
plugins { id 'com.zoltu.git-versioning' }
ZoltuGitVersioning.customDescribeProcessor = { describeResult ->
		[ major: "apple", minor: "orange", sha: "tomato", patch: "pear" ]
}
		""")

		// act
		val result = runVersionTaskExpectingFailure()

		// assert
		assertTrue(result.output.contains("Custom Describe Processors must return a map that includes a non null value for the key `commitCount`"))
	}

	@Test
	fun `custom version info toString`() {
		// arrange
		initializeGit("v1.2").createFakeCommits(9)
		initializeBuildGradle("""
plugins { id 'com.zoltu.git-versioning' }
ZoltuGitVersioning.customVersionToString = { versionInfo ->
		"${'$'}{versionInfo.commitCount}.${'$'}{versionInfo.minor}.${'$'}{versionInfo.major}"
}
		""")

		// act
		val result = runVersionTaskExpectingSuccess()

		// assert
		assertTrue(result.output.contains("Version: 9.2.1"))
	}

	private fun initializeBuildGradle(contents: String = "plugins { id 'com.zoltu.git-versioning' }") {
		val buildFile = directory.newFile("build.gradle")!!
		writeFile(buildFile, contents)
	}

	private fun initializeGit(tag: String = "v1.2"): Git {
		val git = Git.init().setDirectory(directory.root).call()
		val commit = git.commit().setMessage("").call()
		git.tag().setName(tag).setObjectId(commit).call()
		return git
	}

	private fun Git.createFakeCommits(count: Int): Git {
		(1..count).forEach {
			commit().setMessage("").call()
		}
		return this
	}

	private fun runVersionTaskExpectingSuccess() = createVersionTaskRunner().build()

	private fun runVersionTaskExpectingFailure() = createVersionTaskRunner().buildAndFail()

	private fun createVersionTaskRunner() = GradleRunner.create()
			.withProjectDir(directory.root)
			.withArguments("version")
			.withPluginClasspath()

	private fun writeFile(destination: File, contents: String) {
		val bufferedWriter = BufferedWriter(FileWriter(destination))
		try {
			bufferedWriter.write(contents)
		} finally {
			bufferedWriter.close()
		}
	}
}
