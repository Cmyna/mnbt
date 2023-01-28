package com.myna.mnbt.annotation.processor

import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic


@SupportedAnnotationTypes("com.myna.mnbt.annotation.MapTo")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
class MapToProcessor: AbstractProcessor() {

    private var messager:Messager? = null

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf()
    }

    override fun init(processingEnv: ProcessingEnvironment?) {
        super.init(processingEnv)
        messager = processingEnv?.messager
    }

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?): Boolean {
        this.messager?.printMessage(Diagnostic.Kind.ERROR, "hello annotation processor!")
        TODO("Not yet implemented")
    }
}