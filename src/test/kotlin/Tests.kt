import com.zoltu.gradle.plugin.GitVersioning
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.spek.api.Spek
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class Tests : Spek({
	given("a semantic version with tags describe result") {
		val describeResult = "v1.2.3-beta-4.5-2-g8885517"

		on("tryGetSemanticVersionInfo") {
			val versionInfo = GitVersioning().getVersionInfo(describeResult)

			it("parses into expected VersionInfo") {
				assertNotNull(versionInfo)
				assertEquals("1", versionInfo.major)
				assertEquals("2", versionInfo.minor)
				assertEquals("3", versionInfo.patch)
				assertEquals("2", versionInfo.commitCount)
				assertEquals("8885517", versionInfo.sha)
				assertEquals("beta-4.5", versionInfo.tags)
				assertEquals("1.2.3-beta-4.5.2", versionInfo.toString())
			}
		}
	}

	given("a semantic version without tags describe result") {
		val describeResult = "v1.2.3-5-gabcd1234"
		on("tryGetSemanticVersionInfo") {

			val versionInfo = GitVersioning().getVersionInfo(describeResult)

			it("parses into expected VersionInfo") {
				assertNotNull(versionInfo)
				assertEquals("1", versionInfo.major)
				assertEquals("2", versionInfo.minor)
				assertEquals("3", versionInfo.patch)
				assertEquals("5", versionInfo.commitCount)
				assertEquals("abcd1234", versionInfo.sha)
				assertEquals(null, versionInfo.tags)
				assertEquals("1.2.3-5", versionInfo.toString())
			}
		}
	}

	given("a simple version with tags describe result") {
		val describeResult = "v1.2-tag-1.tag2.4-3-gabcd1234"
		on("tryGetSemanticVersionInfo") {

			val versionInfo = GitVersioning().getVersionInfo(describeResult)

			it("parses into expected VersionInfo") {
				assertNotNull(versionInfo)
				assertEquals("1", versionInfo.major)
				assertEquals("2", versionInfo.minor)
				assertEquals(null, versionInfo.patch)
				assertEquals("3", versionInfo.commitCount)
				assertEquals("abcd1234", versionInfo.sha)
				assertEquals("tag-1.tag2.4", versionInfo.tags)
				assertEquals("1.2.3-tag-1.tag2.4", versionInfo.toString())
			}
		}
	}

	given("a simple version describe result") {
		val describeResult = "v1.2-3-gabcd1234"
		on("tryGetSemanticVersionInfo") {

			val versionInfo = GitVersioning().getVersionInfo(describeResult)

			it("parses into expected VersionInfo") {
				assertNotNull(versionInfo)
				assertEquals("1", versionInfo.major)
				assertEquals("2", versionInfo.minor)
				assertEquals(null, versionInfo.patch)
				assertEquals("3", versionInfo.commitCount)
				assertEquals("abcd1234", versionInfo.sha)
				assertEquals(null, versionInfo.tags)
				assertEquals("1.2.3", versionInfo.toString())
			}
		}
	}

	// unfortunately, this test fails unless you swap out the call to `getGitDescribeResults(project.rootDir)` with `"v2.0-23-gee473b7"` because the test doesn't have a git directory in its path hierarchy
//	given("a project") {
//		val project = ProjectBuilder.builder().build()
//
//		on("applying the plugin") {
//			project.pluginManager.apply(GitVersioning::class.java)
//
//			it("has access to the extension") {
//				assertNotNull(project.extensions.getByName("ZoltuGitVersioning") as GitVersioning.Extension)
//			}
//		}
//	}
}) {}
