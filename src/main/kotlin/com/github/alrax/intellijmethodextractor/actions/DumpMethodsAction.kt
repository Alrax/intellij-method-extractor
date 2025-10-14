package com.github.alrax.intellijmethodextractor.actions

import com.github.alrax.intellijmethodextractor.services.JsonExporter
import com.github.alrax.intellijmethodextractor.services.MethodExtractor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task

/**
 * Tools menu action that extracts all Java methods and writes them to `methods.json`.
 * - Waits for smart mode (indices ready) before PSI traversal.
 * - Runs work in a background task; delegates to MethodExtractor and JsonExporter.
 */
class DumpMethodsAction : AnAction() {
    /** Triggers background extraction and write to `<projectRoot>/methods.json`. */
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        DumbService.getInstance(project).runWhenSmart {
            ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Dump Methods to JSON", false) {
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = true
                    try {
                        val methods = MethodExtractor.collectAll(project)
                        JsonExporter.dump(methods, project)
                    } catch (t: Throwable) {
                        t.printStackTrace()
                    }
                }
            })
        }
    }
}
