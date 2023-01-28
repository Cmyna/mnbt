package com.myna.mnbt.reflect

import com.myna.mnbt.exceptions.ConversionException
import java.lang.IllegalArgumentException
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException

/**
 * a class for finding a constructor by passed raw type
 */
object ObjectInstanceHandler {


    /**
     * @param type the object want to create
     * @param onlyJavaBean only JavaBean object (which has empty varag constructor) can be accepted
     *
     * @throws ConversionException when onlyJavaBean is true, and not found empty constructor
     */
    fun <T> newInstance(type: Class<T>, onlyJavaBean:Boolean):T? {
        var instance:T? = null
        try {
            instance = fromObjectConstructor(type)
        } catch (exc:NoSuchMethodException) {
            if (onlyJavaBean) throw ConversionException("Can not construct instance of `$type`, " +
                    "no constructor with empty varags exists."
            )
        }
        if (instance == null) instance = fromUnsafe(type)
        return instance
    }

    fun <T> fromObjectConstructor(type:Class<T>):T? {
        // try find empty constructor by reflection
        return try {
            val constructor = type.getDeclaredConstructor()
            // if not accessible, try set it accessible
            if (!constructor.canAccess(null)) constructor.trySetAccessible()
            constructor.newInstance() as T
        }
        catch (exc:SecurityException) {
            null
        }
        catch (exc:IllegalArgumentException) {
            null
        }
        catch (exc:InstantiationException) {
            null
        }
        catch (exc: InvocationTargetException) {
            null
        }
        catch (exc:ExceptionInInitializerError) {
            null
        }
    }

    /**
     * find all fields and superclass fields (include private fields), and return a list
     */
    fun getAllFields(type:Class<*>?):List<Field> {
        if (type==null) return mutableListOf()
        val fields = mutableListOf<Field>()
        fields.addAll(type.declaredFields)
        getAllFields(type.superclass).also {fields.addAll(it)}
        return fields
    }

    private fun <T> fromUnsafe(type:Class<T>):T? {
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        val field = unsafeClass.getDeclaredField("theUnsafe")
        val accessible = field.trySetAccessible()
        if (!accessible) return null // can not access unsafe, creation failed
        val method = unsafeClass.getDeclaredMethod("allocateInstance", Class::class.java)
        val unsafe = field.get(null)
        return method.invoke(unsafe, type) as T
    }

}