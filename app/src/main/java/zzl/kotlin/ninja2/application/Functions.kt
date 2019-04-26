package zzl.kotlin.ninja2.application

/**
 * 函数类型别名扩展
 * Func 命名系列为不带返回类型的函数别名扩展
 * FuncReturn 命名系列为带返回类型的函数别名扩展，需要指定返回类型
 */
typealias Func = () -> Unit
typealias Func1<P> = (it: P) -> Unit
typealias Func2<P1, P2> = (p1: P1, p2: P2) -> Unit
typealias Func3<P1, P2, P3> = (p1: P1, p2: P2, p3: P3) -> Unit

typealias FuncReturn<R> = () -> R
typealias FuncReturn1<P, R> = (it: P) -> R
typealias FuncReturn2<P1, P2, R> = (p1: P1, p2: P2) -> R
typealias FuncReturn3<P1, P2, P3, R> = (p1: P1, p2: P2, p3: P3) -> R