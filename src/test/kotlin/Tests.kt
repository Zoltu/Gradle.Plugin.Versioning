import com.zoltu.gradle.plugin.GitVersioning
import org.jetbrains.spek.api.Spek
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class Tests : Spek() {
	init {
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
	}
}
