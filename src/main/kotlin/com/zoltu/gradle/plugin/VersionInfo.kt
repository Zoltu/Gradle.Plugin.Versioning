package com.zoltu.gradle.plugin

class VersionInfo(val major: String, val minor: String, val patch: String, val sha: String) {
	override fun toString(): String = "$major.$minor.$patch".toString()
}
