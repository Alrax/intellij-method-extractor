package com.github.alrax.intellijmethodextractor.services

import com.google.gson.GsonBuilder
import com.intellij.openapi.project.Project
import java.io.File

/**
 * Writes a List<MethodInfo> to `<projectRoot>/methods.json` using Gson (pretty, HTML escaping disabled).
 * Overwrites any existing file.
 */
object JsonExporter {
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create()

    /** Serialize [methods] to JSON and write to `<projectRoot>/methods.json`. */
    fun dump(methods: List<MethodInfo>, project: Project) {
        val json = gson.toJson(methods)
        val basePath = project.basePath ?: project.presentableUrl ?: "."
        val output = File(basePath, "methods.json")
        output.writeText(json)
    }
}