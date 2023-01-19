package com.example.demo

import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.stereotype.Component

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class Bar(val i: Int)

@Aspect
@Component
class Aspect {
    @Before("(@within(com.example.demo.Bar) && args(int)) || (@annotation(com.example.demo.Bar) && args(int))")
    fun before(joinPoint: JoinPoint, int: Int) {
        val signature = joinPoint.signature as MethodSignature
        val annotation = signature.method.getAnnotation(Bar::class.java) ?: signature.method.declaringClass.getAnnotation(Bar::class.java)

        println(annotation.i)
        println(int)
    }
}

@Component
@Bar(1)
class Foo {
    fun foo(int: Int) {
        println("foo")
    }
}
