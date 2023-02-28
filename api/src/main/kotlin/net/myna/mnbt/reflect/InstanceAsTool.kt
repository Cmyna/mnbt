package net.myna.mnbt.reflect

import net.myna.mnbt.annotations.InstanceAs
import net.myna.mnbt.codec.binary.RArray
import net.myna.mnbt.exceptions.InvalidInstanceAsClassException
import net.myna.mnbt.reflect.TypeCheckTool.isInterfaceOrAbstract
import java.lang.reflect.Field
import java.lang.reflect.Modifier

object InstanceAsTool {

    fun resolveAnnotation(field: Field):MTypeToken<out Any> {
        val instanceAsAnnotation = field.getAnnotation(InstanceAs::class.java)
        if (instanceAsAnnotation!=null) {
            val wrappedClass = instanceAsAnnotation.instanceClass.java
            val token = MTypeToken.of(wrappedClass)
            //val declaredType = if (field.type.isArray) field.type.componentType else field.type
            val declaredType = field.type

            if (!TypeCheckTool.isCastable(token, MTypeToken.of(declaredType))) {
                throw InvalidInstanceAsClassException(
                    wrappedClass,
                    declaredType,
                    InvalidInstanceAsClassException.ExceptionType.NOT_SUBTYPE
                )
            }
            if (isInterfaceOrAbstract(wrappedClass)) {
                throw InvalidInstanceAsClassException(
                    wrappedClass, declaredType,
                    InvalidInstanceAsClassException.ExceptionType.IS_ABSTRACT
                )
            }

//            if (field.type.isArray) {
//                // return array type declaration
//                return RArray.newInstance(token.rawType, 0).javaClass.let { MTypeToken.of(it) }
//            }
            return token
        } else return MTypeToken.of(field.genericType)
    }
}