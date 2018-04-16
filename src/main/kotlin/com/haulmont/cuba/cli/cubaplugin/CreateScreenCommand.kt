/*
 * Copyright (c) 2008-2018 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.haulmont.cuba.cli.cubaplugin

import com.beust.jcommander.Parameters
import com.google.common.base.CaseFormat
import com.haulmont.cuba.cli.ModuleType
import com.haulmont.cuba.cli.ProjectFiles
import com.haulmont.cuba.cli.commands.GeneratorCommand
import com.haulmont.cuba.cli.generation.TemplateProcessor
import com.haulmont.cuba.cli.generation.parse
import com.haulmont.cuba.cli.generation.save
import com.haulmont.cuba.cli.model.ProjectModel
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList
import net.sf.practicalxml.DomUtil
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

@Parameters
class CreateScreenCommand : GeneratorCommand<ScreenModel>() {
    override fun getModelName(): String = ScreenModel.MODEL_NAME

    override fun QuestionsList.prompting() {
        val projectModel = context.getModel<ProjectModel>(ProjectModel.MODEL_NAME)

        question("screenName", "Screen name") {
            default("screen")
            validate {
                checkRegex("([a-zA-Z]*[a-zA-Z0-9]+)(-[a-zA-Z]*[a-zA-Z0-9]+)*", "Invalid screen name")
            }
        }
        options("module", "Choose module", listOf("web", "gui")) {
            default(1)
        }
        question("package", "Package name") {
            default { "${projectModel.rootPackage}.${it["module"]}.screens" }
            validate {
                checkIsPackage()
            }
        }
    }

    override fun createModel(answers: Answers): ScreenModel {
        val screenName = answers["screenName"] as String
        val controllerName = CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, screenName)

        val packageName = answers["package"] as String
        val packageDirectory = (packageName).replace('.', '/')

        return ScreenModel(
                screenName,
                controllerName,
                answers["module"] as String,
                packageName,
                packageDirectory
        )
    }

    override fun beforeGeneration(bindings: MutableMap<String, Any>) {
        val screenModel = context.getModel<ScreenModel>(ScreenModel.MODEL_NAME)

        bindings["packageDirectory"] = screenModel.packageDirectory
        bindings["module"] = screenModel.module
        bindings["screenName"] = screenModel.screenName
        bindings["controllerName"] = screenModel.controllerName

        super.beforeGeneration(bindings)
    }

    override fun generate(bindings: Map<String, Any>) {
        val screenModel = context.getModel<ScreenModel>(ScreenModel.MODEL_NAME)

        TemplateProcessor("templates/screen")
                .copyTo(Paths.get(""), bindings)

        val screensXml = when (screenModel.module) {
            "web" -> ProjectFiles().getModule(ModuleType.WEB)
            "gui" -> ProjectFiles().getModule(ModuleType.GUI)
            else -> return
        }.screensXml

        addToScreensXml(screensXml, screenModel)
    }

    private fun addToScreensXml(screensXml: Path, screenModel: ScreenModel) {
        val document = parse(screensXml)

        DomUtil.appendChild(document.documentElement, "screen").apply {
            setAttribute("id", screenModel.screenName)
            setAttribute("template", screenModel.packageDirectory + File.separatorChar + screenModel.screenName + ".xml")
        }

        save(document, screensXml)
    }

    override fun checkPreconditions() = onlyInProject()
}

data class ScreenModel(
        val screenName: String,
        val controllerName: String,
        val module: String,
        val packageName: String,
        val packageDirectory: String) {
    companion object {
        const val MODEL_NAME = "screen"
    }
}