package com.github.sieves.util

import com.github.sieves.Toys
import com.github.sieves.registry.internal.IRegister
import com.github.sieves.registry.internal.Registry
import com.github.sieves.registry.internal.net.Packet
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.server.ServerLifecycleHooks
import org.apache.logging.log4j.LogManager
import thedarkcolour.kotlinforforge.forge.FORGE_BUS
import thedarkcolour.kotlinforforge.forge.MOD_BUS
import kotlin.reflect.full.isSubclassOf

object Log {
    val logger = LogManager.getLogger(Toys.ModId)

    inline fun info(supplier: () -> String) = logger.info(supplier())
    inline fun warn(supplier: () -> String) = logger.warn(supplier())
    inline fun debug(supplier: () -> String) = logger.debug(supplier())
    inline fun error(supplier: () -> String) = logger.error(supplier())
    inline fun trace(supplier: () -> String) = logger.trace(supplier())
}

fun Packet.getLevel(key: ResourceKey<Level>): Level {
    return ServerLifecycleHooks.getCurrentServer().getLevel(key)!!
}

/**
 * Gets a resource location based upon the give string
 */
val String.resLoc: ResourceLocation
    get() = ResourceLocation(Toys.ModId, this)


/**
 * This is used for easy block entity registration
 */
inline fun <reified T : BlockEntity> Registry<BlockEntityType<*>>.tile(
    block: Block,
    crossinline supplier: (Pair<BlockPos, BlockState>) -> T
): BlockEntityType<T> {
    return BlockEntityType.Builder.of({ pos, state -> supplier(pos to state) }, block).build(null)
}

inline fun <reified T : IRegister> T.registerAll(
    modID: String = Toys.ModId,
    modBus: IEventBus = MOD_BUS,
    forgeBus: IEventBus = FORGE_BUS
) {
    for (child in this::class.nestedClasses) {
        if (child.isSubclassOf(IRegister::class)) {
            val instance = child.objectInstance ?: continue
            if (instance is IRegister) {
                instance.register(modID, modBus, forgeBus)
                Log.info { "Successfully registered the '${child.simpleName}' registry" }
            }
        }
    }
}

/**
 * This will raytrace the given distance for the given player
 */
fun Player.rayTrace(distance: Double = 75.0): BlockHitResult {
    val rayTraceResult = pick(distance, 0f, false) as BlockHitResult
    var xm = rayTraceResult.location.x
    var ym = rayTraceResult.location.y
    var zm = rayTraceResult.location.z
    var pos = BlockPos(xm, ym, zm)
    val block = level.getBlockState(pos)
    if (block.isAir) {
        if (rayTraceResult.direction == Direction.SOUTH)
            zm--
        if (rayTraceResult.direction == Direction.EAST)
            xm--
        if (rayTraceResult.direction == Direction.UP)
            ym--
    }
    pos = BlockPos(xm, ym, zm)
    return BlockHitResult(rayTraceResult.location, rayTraceResult.direction, pos, false)
}