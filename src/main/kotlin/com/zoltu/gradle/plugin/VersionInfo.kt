package com.zoltu.gradle.plugin

data class VersionInfo(
		val major: String,
		val minor: String,
		val commitCount: String,
		val sha: String,
		val patch: String? = null,
		val tags: String? = null) {
	override fun toString(): String {
		val suffix = if (tags != null && patch != null) {
			// semantic versioning
			"$patch-$tags.$commitCount"
		} else if (patch != null) {
			// semantic versioning
			"$patch-$commitCount"
		} else if (tags != null) {
			// simple versioning
			"$commitCount-$tags"
		} else {
			// simple versioning
			"$commitCount"
		}
		return "$major.$minor.$suffix".toString()
	}
}
