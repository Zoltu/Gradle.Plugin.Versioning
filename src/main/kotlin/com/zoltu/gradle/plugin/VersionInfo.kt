package com.zoltu.gradle.plugin

data class VersionInfo @JvmOverloads constructor(
		val major: String,
		val minor: String,
		val commitCount: String,
		val sha: String,
		val patch: String? = null,
		val tags: String? = null)
