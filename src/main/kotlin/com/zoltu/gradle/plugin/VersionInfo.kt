package com.zoltu.gradle.plugin

class VersionInfo(val major: String, val minor: String, val commitCount: String, val sha: String, val patch: String? = null, val tags: String? = null) {
	override fun toString(): String {
		return "$major.$minor.${patch ?: commitCount}${if (tags != null) "-$tags" else ""}${if (patch != null) "-$commitCount" else ""}".toString()
	}
}
