package com.github.sieves.registry.internal

import com.github.sieves.content.container.SieveContainer
import com.google.common.collect.Queues
import net.minecraft.resources.ResourceKey
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.network.IContainerFactory
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.IForgeRegistry
import net.minecraftforge.registries.IForgeRegistryEntry
import net.minecraftforge.registries.RegistryObject
import java.util.*
import java.util.function.Supplier
import kotlin.collections.HashMap
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty


abstract class Registry<B : IForgeRegistryEntry<B>>(private val registry: IForgeRegistry<B>) : IRegister {
    private lateinit var deferredRegister: DeferredRegister<B>
    private val registers: Queue<Pair<String, () -> B>> = Queues.newArrayDeque()
    private val objects: MutableMap<String, RegistryObject<B>> = HashMap()

    override fun register(modId: String, modBus: IEventBus, forgeBus: IEventBus) {
        deferredRegister = DeferredRegister.create(registry, modId)
        while (registers.peek() != null) {
            val data = registers.remove()
            objects[data.first] = deferredRegister.register(data.first, data.second)
        }
        deferredRegister.register(modBus)
    }


    /**
     * This is used delegate registration using properties
     */
    protected fun <T : B> register(
        name: String,
        supplier: () -> T
    ): ReadOnlyProperty<Any?, T> {
        registers.add(name to supplier)
        return object : ReadOnlyProperty<Any?, T>, Supplier<T>, () -> T {
            override fun get(): T = objects[name]!!.get() as T

            override fun getValue(thisRef: Any?, property: KProperty<*>): T = get()

            override fun invoke(): T = get()
        }
    }
}

//ResourceKey<Registry<RecipeType<?>>>
abstract class MojangRegistry<R : net.minecraft.core.Registry<T>, T>(private val registry: ResourceKey<R>) :
    IRegister {
    private lateinit var deferredRegister: DeferredRegister<T>
    private val registers: Queue<Pair<String, () -> T>> = Queues.newArrayDeque()
    private val objects: MutableMap<String, RegistryObject<T>> = HashMap()

    override fun register(modId: String, modBus: IEventBus, forgeBus: IEventBus) {
        deferredRegister = DeferredRegister.create(registry, modId)
        while (registers.peek() != null) {
            val data = registers.remove()
            objects[data.first] = deferredRegister.register(data.first, data.second)
        }
        deferredRegister.register(modBus)
    }


    /**
     * This is used delegate registration using properties
     */
    protected fun<B : T> register(
        name: String,
        supplier: () -> B
    ): ReadOnlyProperty<Any?, B> {
        registers.add(name to supplier)
        return object : ReadOnlyProperty<Any?, B>, Supplier<B>, () -> B {
            override fun get(): B = objects[name]!!.get() as B

            override fun getValue(thisRef: Any?, property: KProperty<*>): B = get()

            override fun invoke(): B = get()
        }
    }
}