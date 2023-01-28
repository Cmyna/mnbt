package com.myna.mnbt.annotations.processors

import com.myna.mnbt.annotations.MapTo
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.tools.Diagnostic

private const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"

@SupportedOptions(KAPT_KOTLIN_GENERATED_OPTION_NAME)
class MapToProcessor: AbstractProcessor() {

    private var messager:Messager? = null

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latest()
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(MapTo::class.java.canonicalName)
    }

    override fun init(processingEnv: ProcessingEnvironment?) {
        super.init(processingEnv)
        messager = processingEnv!!.messager
        this.messager!!.printMessage(Diagnostic.Kind.NOTE, "AAAAAAAAAAAAAAAAAAAAAA")
    }

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?): Boolean {
        this.messager!!.printMessage(Diagnostic.Kind.NOTE, "Hello!")
        roundEnv?.getElementsAnnotatedWith(MapTo::class.java)?.onEach {
            this.messager!!.printMessage(Diagnostic.Kind.NOTE, "enclosindClass: ${it.enclosingElement.simpleName}")
            this.messager!!.printMessage(Diagnostic.Kind.NOTE, "kind: ${it.kind}")
            this.messager!!.printMessage(Diagnostic.Kind.NOTE, "class type: ${it.kind.declaringClass}")
            // Element Subinterfaces see: https://docs.oracle.com/javase/9/docs/api/javax/lang/model/element/class-use/Element.html
            if (it is VariableElement) {
                this.messager!!.printMessage(Diagnostic.Kind.NOTE, "field name: ${it.simpleName}")
                this.messager!!.printMessage(Diagnostic.Kind.NOTE, "class type: ${it.asType()}")
            } else if (it is TypeElement) {
                this.messager!!.printMessage(Diagnostic.Kind.NOTE, "class name: ${it.qualifiedName}")
            }
        }

        return true
    }
}