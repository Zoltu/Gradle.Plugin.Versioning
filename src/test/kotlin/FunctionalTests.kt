import org.eclipse.jgit.api.Git
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
		assertTrue(result.output.contains("Version: 1.2.3"))
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

	private fun initializeBuildGradle(contents: String = "plugins { id 'com.zoltu.git-versioning' }") {
		val buildFile = directory.newFile("build.gradle")!!
		writeFile(buildFile, contents)
	}

	private fun initializeGit(tag: String = "1.2.3"): Git {
		val git = Git.init().setDirectory(directory.root).call()
		val commit = git.commit().setMessage("").call()
		git.tag().setName(tag).setObjectId(commit).call()
		return git
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
