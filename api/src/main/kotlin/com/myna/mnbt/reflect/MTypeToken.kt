package com.myna.mnbt.reflect

import com.google.common.reflect.TypeToken
import java.lang.IllegalArgumentException
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable

open class MTypeToken<T> {

    @Deprecated("This property will be replaced in the future")
    private val gToken: TypeToken<T>

    val rawType get() = gToken.rawType
    val type get() = gToken.type
    val isArray get() = gToken.isArray
    val componentType get() = MTypeToken(gToken.componentType as TypeToken<out Any>)

    constructor() {
        val superclass = javaClass.genericSuperclass
        if (superclass !is ParameterizedType) {
            throw IllegalArgumentException("class $superclass is not parameterized")
        }
        val runtimeType = superclass.actualTypeArguments[0]
        if (runtimeType is TypeVariable<*>) throw IllegalArgumentException("Cannot construct a TypeToken for a type variable")
        gToken = TypeToken.of(runtimeType) as TypeToken<T>
    }


    constructor(field: Field) {
        this.gToken = TypeToken.of(field.genericType) as TypeToken<T>
    }

    constructor(typeToken: TypeToken<T>) {
        this.gToken = typeToken
    }

    fun resolveType(type: Type) = MTypeToken(gToken.resolveType(type))

    fun isSubtypeOf(typeToken: MTypeToken<out Any>) = this.gToken.isSubtypeOf(typeToken.gToken)
    fun wrap(): MTypeToken<T> = MTypeToken(this.gToken.wrap())


    companion object {
        @JvmStatic
        fun of(type: Type) = MTypeToken(TypeToken.of(type) as TypeToken<*>)

        @JvmStatic
        fun <T> of(clazz: Class<T>) = MTypeToken(TypeToken.of(clazz) as TypeToken<T>)
    }
}