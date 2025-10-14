package com.github.alrax.intellijmethodextractor.services

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.openapi.fileTypes.StdFileTypes
import com.intellij.openapi.vfs.VirtualFile

/**

 * - [name]: simple, unqualified method name.
 * - [body]: full text of the method body including braces, or null if there is no body
 */
data class MethodInfo(val name: String, val body: String?)

/**
 * Scans the IntelliJ project for Java source files and extracts declared methods via PSI.
 *
 * - Runs inside a ReadAction to safely access PSI structures.
 * - Uses FileTypeIndex over GlobalSearchScope.projectScope(project) to enumerate JAVA VirtualFiles quickly
 *   without loading modules/content roots manually.
 * - This does not block indexing. For consistent coverage, callers should ensure smart mode beforehand
 */
object MethodExtractor {

    /**
     * Collects [MethodInfo] for all methods declared in Java source files of the given [project].
     *
     * - Uses an in-memory list to accumulate results. No threading is used internally.
     * - Logs a message and skips files that don't resolve to PsiJavaFile (e.g., binary or non-Java).
     */
    fun collectAll(project: Project): List<MethodInfo> {
        return ReadAction.compute<List<MethodInfo>, RuntimeException> {
            val methods = mutableListOf<MethodInfo>()
            val psiManager = PsiManager.getInstance(project)

            // Iterate over all Java files in the project using file-based index
            val javaFiles: Collection<VirtualFile> =
                FileTypeIndex.getFiles(StdFileTypes.JAVA, GlobalSearchScope.projectScope(project))

            for (file in javaFiles) {
                val psiFile = psiManager.findFile(file)
                if (psiFile is PsiJavaFile) {
                    val fileName = psiFile.name
                    psiFile.accept(object : JavaRecursiveElementVisitor() {
                        override fun visitClass(aClass: PsiClass) {
                            super.visitClass(aClass)
                            for (method in aClass.methods) {
                                val info = extractMethodInfo(method)
                                methods.add(info)
                            }
                        }
                    })
                } else {
                    println("[MethodExtractor] Skipped non-Java PSI for file: ${'$'}{file.path}")
                }
            }
            methods
        }
    }

    /**
     * Converts a [PsiMethod] into a [MethodInfo].
     */
    private fun extractMethodInfo(method: PsiMethod): MethodInfo {
        val name = method.name
        val bodyText = method.body?.text?.trim()
        return MethodInfo(name, bodyText)
    }
}