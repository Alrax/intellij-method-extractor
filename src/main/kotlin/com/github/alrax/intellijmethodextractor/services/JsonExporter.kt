package com.github.alrax.intellijmethodextractor.services

import com.google.gson.GsonBuilder
import com.intellij.openapi.project.Project
import java.io.File

object JsonExporter {
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create()

    fun dump(methods: List<MethodInfo>, project: Project) {
        val json = gson.toJson(methods)
        val basePath = project.basePath ?: project.presentableUrl ?: "."
        val output = File(basePath, "methods.json")
        output.writeText(json)
    }
}