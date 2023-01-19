package com.example.demo

import kotlin.reflect.KClass
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.AfterThrowing
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import org.springframework.expression.EvaluationContext
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.stereotype.Component

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class LogOnReturn(
    val log: String,
    val level: Level = Level.INFO,
)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class LogOnThrow(
    val log: String,
    val level: Level = Level.ERROR,
    val except: Array<KClass<out Exception>> = [],
)

@Aspect
@Component
class LogAspect {
    val parser = SpelExpressionParser()

    @AfterReturning("@annotation(com.example.demo.LogOnReturn)", returning = "return")
    fun afterReturnAdvice(joinPoint: JoinPoint, `return`: Any?) {
        val signature = joinPoint.signature as MethodSignature
        val annotation = signature.method.getAnnotation(LogOnReturn::class.java)

        val context = buildContext(joinPoint, signature).apply {
            setVariable("return", `return`)
            setVariable("returnTypeName", `return`?.javaClass?.simpleName)
            setVariable("returnTypePackage", `return`?.javaClass?.`package`)
        }
        val log = loggerFor(signature.method.declaringClass)
        val message = buildMessage(context, annotation.log)

        log.makeLoggingEventBuilder(annotation.level).log(message)
    }

    @AfterThrowing("@annotation(com.example.demo.LogOnThrow)", throwing = "exception")
    fun afterThrowingAdvice(joinPoint: JoinPoint, exception: Exception) {
        val signature = joinPoint.signature as MethodSignature
        val annotation = signature.method.getAnnotation(LogOnThrow::class.java)

        if (annotation.except.contains(exception::class)) return

        val context = buildContext(joinPoint, signature).apply {
            setVariable("exception", exception)
            setVariable("exceptionName", exception::class.simpleName)
            setVariable("package", exception::class.java.`package`)
        }
        val log = loggerFor(signature.method.declaringClass)
        val message = buildMessage(context, annotation.log)

        log.makeLoggingEventBuilder(annotation.level).log(message)
    }

    private fun loggerFor(clazz: Class<*>): Logger = LoggerFactory.getLogger(clazz)

    private fun buildMessage(
        context: EvaluationContext,
        spelString: String,
    ): String {
        val spel = parser.parseRaw(spelString)
        return spel.getValue(context, String::class.java)!!
    }

    private fun buildContext(
        joinPoint: JoinPoint,
        signature: MethodSignature
    ): StandardEvaluationContext {
        val context = StandardEvaluationContext().apply {
            setVariable("args", joinPoint.args)
            setVariable("method", signature.method.name)
            setVariable("it", joinPoint.`this`)
            setVariable("className", signature.method.declaringClass.simpleName)
            setVariable("package", signature.method.declaringClass.`package`)
        }
        return context
    }
}

@Component
class Testa {
    @LogOnReturn(level = Level.INFO, log = RETURN_LOG_SPEL)
    @LogOnThrow(level = Level.WARN, log = EXCEPTION_LOG_SPEL)
    fun log(key: String) = println(key)

    companion object {
        private const val COMMOM_LOG_SPEL = "'[method:' + #method + '][key:' + #args[0] + ']'"
        private const val RETURN_LOG_SPEL = "$COMMOM_LOG_SPEL + '[returned:' + #return + ']'"
        private const val EXCEPTION_LOG_SPEL = "$COMMOM_LOG_SPEL + '[threw:' + #exceptionName + ']'"
    }
}
