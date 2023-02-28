package net.myna.mnbt.exceptions

class InvalidInstanceAsClassException(
    private val fieldDeclaredClass: Class<*>,
    private val AnnotationDeclaredClass: Class<*>,
    val type: ExceptionType): RuntimeException() {

    override val message: String
        get() {
            return when(type) {
                ExceptionType.NOT_SUBTYPE -> {
                    "The Annotation InstanceAs declared class ($AnnotationDeclaredClass) " +
                            "is not subtype of field class ($fieldDeclaredClass)!"
                }
                ExceptionType.IS_ABSTRACT -> "The Annotation InstanceAs declared class ($AnnotationDeclaredClass) is an abstract class or an interface!"
                else -> throw NotImplementedError()
            }
        }

    enum class ExceptionType {
        NOT_SUBTYPE, IS_ABSTRACT
    }
}