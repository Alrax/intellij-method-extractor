package com.github.alrax.intellijmethodextractor.startup

import com.github.alrax.intellijmethodextractor.services.JsonExporter
import com.github.alrax.intellijmethodextractor.services.MethodExtractor
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.platform.backend.observation.Observation
import com.intellij.openapi.project.waitForSmartMode

/**
 * Project startup activity: waits for configuration and smart mode, then writes `methods.json`.
 * Work runs off the UI thread and delegates to MethodExtractor and JsonExporter.
 */
class DumpMethodsOnOpen : ProjectActivity {
    /** Waits for readiness and dumps all Java methods to `<projectRoot>/methods.json`. */
    override suspend fun execute(project: Project) {
        Observation.awaitConfiguration(project)
        project.waitForSmartMode()

        val methods = MethodExtractor.collectAll(project)
        JsonExporter.dump(methods, project)
    }
}