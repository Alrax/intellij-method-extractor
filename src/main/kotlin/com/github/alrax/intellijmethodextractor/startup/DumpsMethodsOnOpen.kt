package com.github.alrax.intellijmethodextractor.startup

import com.github.alrax.intellijmethodextractor.services.JsonExporter
import com.github.alrax.intellijmethodextractor.services.MethodExtractor
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.platform.backend.observation.Observation
import com.intellij.openapi.project.waitForSmartMode

class DumpMethodsOnOpen : ProjectActivity {
    override suspend fun execute(project: Project) {
        // Wait for indexing and configuration to complete
        Observation.awaitConfiguration(project)
        project.waitForSmartMode()

        val methods = MethodExtractor.collectAll(project)
        JsonExporter.dump(methods, project)
    }
}