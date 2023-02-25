package com.example.emptyprojectwithmodule

import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteActionAndWait
import com.intellij.openapi.module.EmptyModuleType
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleTypeId
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vcs.changes.shelf.ShelveChangesManager.PostStartupActivity
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.VfsTestUtil
import com.intellij.workspaceModel.ide.WorkspaceModel
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.module.JpsModuleSourceRootType


class CreateEmptyProject() : PostStartupActivity(), DumbAware {
    private fun getProjectPath(project: Project): String? {
        return project.basePath
    }

    fun createModule(project: Project, name: String): Module {
        return runWriteActionAndWait {
            ModuleManager.getInstance(project).newModule(
                project.basePath.plus("/$name/$name.iml"), EmptyModuleType.EMPTY_MODULE
            )
        }
    }

    fun addSourceRoot(
        project: Project,
        module: Module,
        relativePath: String,
        rootType: JpsModuleSourceRootType<*>
    ): VirtualFile {
        var projectRootDir: String? = getProjectPath(project)


        val srcRoot = VfsUtil.createDirectories(projectRootDir + "/${module.name}/$relativePath")
        ModuleRootModificationUtil.updateModel(module) { model ->
            val contentRootUrl = VfsUtil.pathToUrl(projectRootDir.plus("/${module.name}"))
            val contentEntry =
                model.contentEntries.find { it.url == contentRootUrl } ?: model.addContentEntry(contentRootUrl)
            require(contentEntry.sourceFolders.none { it.url == srcRoot.url }) { "Source folder $srcRoot already exists" }
            contentEntry.addSourceFolder(srcRoot, rootType)
        }
        return srcRoot
    }


    private fun getMainClassCode(): String = """public class Main {
    public static void main(String[] args) {
        System.out.println("Hello World!");
    }
}"""


    override fun runActivity(project: Project) {
        val application: Application = ApplicationManager.getApplication()

        application.invokeLater {
            val workspaceModel = WorkspaceModel.getInstance(project)

            val module: Module = createModule(project, "main")
//            println(module.moduleFilePath)
            val mainSrc = addSourceRoot(project, module, "src", JavaSourceRootType.SOURCE)
            module.setModuleType(ModuleTypeId.JAVA_MODULE)
//        ModuleRootModificationUtil.addContentRoot(module, mainSrc)
            VfsTestUtil.createFile(mainSrc, "Main.java", getMainClassCode())

            val projectSdk = ProjectRootManager.getInstance(project).projectSdk
            val modifiableModule = ModuleRootManager.getInstance(module).modifiableModel
            modifiableModule.sdk = projectSdk
            runWriteActionAndWait {
                modifiableModule.commit()
            }
        }

    }

}


