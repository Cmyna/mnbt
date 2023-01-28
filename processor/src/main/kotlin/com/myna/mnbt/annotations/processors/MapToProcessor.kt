package com.myna.mnbt.annotations.processors

import com.myna.mnbt.annotations.MapTo
import java.lang.IllegalStateException
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic


@SupportedAnnotationTypes("com.myna.mnbt.annotations.MapTo")
class MapToProcessor: AbstractProcessor() {

    private var messager:Messager? = null

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latest()
    }

    override fun init(processingEnv: ProcessingEnvironment?) {
        super.init(processingEnv)
        messager = processingEnv!!.messager
        this.messager!!.printMessage(Diagnostic.Kind.NOTE, "AAAAAAAAAAAAAAAAAAAAAA")
    }

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?): Boolean {
        throw IllegalStateException()
//        this.messager!!.printMessage(Diagnostic.Kind.ERROR, "hello annotation processor!OOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO")
//        return true
    }
}