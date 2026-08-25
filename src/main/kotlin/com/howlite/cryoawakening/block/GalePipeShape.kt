package com.howlite.cryoawakening.block

import net.minecraft.core.Direction
import net.minecraft.util.StringRepresentable

/**
 * Formes de raccordement du Gale Pipe (Point A vers Point B).
 * 3 droites, 12 coudes à 90°, 6 terminaisons à bride.
 */
enum class GalePipeShape(
    val id: String,
    val direction1: Direction?,
    val direction2: Direction?
) : StringRepresentable {
    NORTH_SOUTH("north_south", Direction.NORTH, Direction.SOUTH),
    EAST_WEST("east_west", Direction.EAST, Direction.WEST),
    UP_DOWN("up_down", Direction.UP, Direction.DOWN),

    DOWN_NORTH("down_north", Direction.DOWN, Direction.NORTH),
    DOWN_SOUTH("down_south", Direction.DOWN, Direction.SOUTH),
    DOWN_EAST("down_east", Direction.DOWN, Direction.EAST),
    DOWN_WEST("down_west", Direction.DOWN, Direction.WEST),

    UP_NORTH("up_north", Direction.UP, Direction.NORTH),
    UP_SOUTH("up_south", Direction.UP, Direction.SOUTH),
    UP_EAST("up_east", Direction.UP, Direction.EAST),
    UP_WEST("up_west", Direction.UP, Direction.WEST),

    NORTH_EAST("north_east", Direction.NORTH, Direction.EAST),
    NORTH_WEST("north_west", Direction.NORTH, Direction.WEST),
    SOUTH_EAST("south_east", Direction.SOUTH, Direction.EAST),
    SOUTH_WEST("south_west", Direction.SOUTH, Direction.WEST),

    END_NORTH("end_north", Direction.NORTH, null),
    END_SOUTH("end_south", Direction.SOUTH, null),
    END_EAST("end_east", Direction.EAST, null),
    END_WEST("end_west", Direction.WEST, null),
    END_UP("end_up", Direction.UP, null),
    END_DOWN("end_down", Direction.DOWN, null);

    override fun getSerializedName(): String = id

    fun connectsTo(dir: Direction): Boolean =
        direction1 == dir || direction2 == dir

    companion object {
        fun fromDirections(d1: Direction, d2: Direction): GalePipeShape {
            val set = setOf(d1, d2)
            for (shape in entries) {
                if (shape.direction1 != null && shape.direction2 != null) {
                    if (setOf(shape.direction1, shape.direction2) == set) {
                        return shape
                    }
                }
            }
            return NORTH_SOUTH
        }

        fun fromSingleDirection(dir: Direction): GalePipeShape = when (dir) {
            Direction.NORTH -> END_NORTH
            Direction.SOUTH -> END_SOUTH
            Direction.EAST -> END_EAST
            Direction.WEST -> END_WEST
            Direction.UP -> END_UP
            Direction.DOWN -> END_DOWN
        }

        fun fromAxis(axis: Direction.Axis): GalePipeShape = when (axis) {
            Direction.Axis.X -> EAST_WEST
            Direction.Axis.Y -> UP_DOWN
            Direction.Axis.Z -> NORTH_SOUTH
        }
    }
}
