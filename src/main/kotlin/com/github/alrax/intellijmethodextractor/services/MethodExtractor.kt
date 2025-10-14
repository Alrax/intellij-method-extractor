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

data class MethodInfo(val name: String, val body: String?)

object MethodExtractor {

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

    private fun extractMethodInfo(method: PsiMethod): MethodInfo {
        val name = method.name
        val bodyText = method.body?.text?.trim()
        return MethodInfo(name, bodyText)
    }
}