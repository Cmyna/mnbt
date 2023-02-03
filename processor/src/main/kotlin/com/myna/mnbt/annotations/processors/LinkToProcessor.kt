package com.myna.mnbt.annotations.processors

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import java.io.File
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.util.Elements
import javax.tools.Diagnostic

class LinkToProcessor {

    var elementUtils: Elements? = null
    var messager:Messager? = null
    var processingEnv:ProcessingEnvironment? = null


    fun writeNbtPath(elements: MutableSet<out Element>) {
        val annotatedClasses = elements.filter {
            it.kind == ElementKind.CLASS
        }
        val annotatedFields = elements.filter {
            it.kind==ElementKind.FIELD
        }
        if (annotatedClasses.isNotEmpty()) writeClass(annotatedClasses.first())


//        annotatedClasses.onEach {
//            writeClass(it)
//        }
    }

    private fun writeClass(element: Element) {
        element.enclosingElement
        val packageElements = elementUtils!!.getPackageOf(element)
        messager!!.printMessage(Diagnostic.Kind.NOTE, "class name: ${element.simpleName}")
        messager!!.printMessage(Diagnostic.Kind.NOTE, "class package: ${packageElements.qualifiedName} \n")

        val generatedSourcesRoot = processingEnv!!.options[KAPT_KOTLIN_GENERATED_OPTION_NAME].orEmpty()
        if (generatedSourcesRoot.isEmpty()) {
            processingEnv!!.messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Can't find the target directory for generated Kotlin files."
            )
            return
        }

        val type = TypeSpec.Companion.classBuilder(element.simpleName.toString())
        type.addOriginatingElement(element)

        val file = FileSpec.builder(packageElements.qualifiedName.toString(), element.simpleName.toString())
                .addType(type.build())
                .build()

        val testDir = File(generatedSourcesRoot)
        testDir.mkdir()
        file.writeTo(testDir)
    }
}