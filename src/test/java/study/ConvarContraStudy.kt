package study

import org.junit.Test

class ConvarContraStudy {
    // to study covariance and contrivance in kt/java

    @Test
    fun callFun() {
        val foo1 = Foo<Int>()
        // compile error can not covariant from Int->Any
        //acceptFoo(foo1)

        // compile pass, because type var T of Foo<> in acceptFoo2 become covariance
        acceptFoo2(foo1)

        val foo2 = Foo<Foo<Int>>()
        //acceptFoo3(foo2)



        val actor:Action<Action<Cat>> = Action { innerActor->innerActor.act(Cat())}
        consumeAnimal(actor) // it will compile, because contravariance twice make it covariance!!!

        val actor2:Action<Animal> = Action {
            Animal("animal")
        }
        consumeAnimal2(actor2)

        val actor3:Action<Cat> = Action {Cat()}
        //consumeAnimal2(consumer3) // this fun will not compiled, because it is contravariance

        // what the fuck?
        val bar = BarChild<Int>()
        val bar2 = Bar<BarChild<Int>>()
    }


    fun consumeAnimal(actor:Action<Action<Animal>>) {
        val innerActor:Action<Animal> = Action { animal->println(animal.name)}
        actor.act(innerActor)
    }
    fun consumeAnimal2(consumer:Action<Animal>) {
    }

    class Foo<T> {}

    fun acceptFoo(foo:Foo<Any>) {}
    fun acceptFoo2(foo:Foo<out Any>) {}
    fun acceptFoo3(foo:Foo<in Any>) {}
}

// define a contrivance generic class
class Action<in T>(val act:(t:T)->Unit) {}
open class Animal(val name:String) {}
class Cat:Animal("cat"){}

// something what the fuck?
open class Bar<T:Bar<T>> {}
class BarChild<T>:Bar<BarChild<T>>() {}