package com.myna.mnbt.annotations.processors

import com.myna.mnbt.annotations.LocateAt
import java.lang.IllegalStateException
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"

@SupportedOptions(KAPT_KOTLIN_GENERATED_OPTION_NAME)
class AnnotationProcessor: AbstractProcessor() {

    private var messager:Messager? = null
    private val linkToProcessor = LinkToProcessor()

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latest()
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(LocateAt::class.java.canonicalName)
    }

    override fun init(processingEnv: ProcessingEnvironment?) {
        super.init(processingEnv)
        messager = processingEnv!!.messager

        this.linkToProcessor.elementUtils = processingEnv.elementUtils
        this.linkToProcessor.messager = this.messager!!
        this.messager!!.printMessage(Diagnostic.Kind.NOTE, "Mnbt Processor is inited")
    }

    // the core idea of handle @MapTo annotation:
    // if it is a class, auto make this class implement an interface, use an static final companion object store interfaces related static data
    // if it is a field, find enclosed class and handle with the class
    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?): Boolean {
        this.linkToProcessor.processingEnv = this.processingEnv
        this.messager!!.printMessage(Diagnostic.Kind.NOTE, "Hello!")
        val annotated = roundEnv?.getElementsAnnotatedWith(LocateAt::class.java)!!
        this.messager!!.printMessage(Diagnostic.Kind.NOTE, "annotated element num: ${annotated?.size?:0}")
        linkToProcessor.writeNbtPath(annotated)

        return true
    }

    /**
     * group elements: each key is a class and value is set of field with @MapTo annotation in this class
     */
    private fun group(annotations:Set<Element>):Map<Element, MutableSet<Element>?> {
        val classes = annotations.filter { it.kind.isClass }
        val fields = annotations.filter { it.kind.isField }
        val groupMap = HashMap<Element, MutableSet<Element>?>()
        classes.onEach {
            groupMap[it] = null
        }
        fields.onEach { fieldElement->
            val enclosed = fieldElement.enclosingElement
            if (!enclosed.kind.isClass) throw IllegalStateException("field enclose ${fieldElement.simpleName} should be a class!")
            if (groupMap[enclosed] == null) groupMap[enclosed] = HashSet()
            groupMap[enclosed]!!.add(fieldElement)
        }
        return groupMap
    }

}