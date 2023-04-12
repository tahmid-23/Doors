package com.github.tahmid_23.doors.game.generation;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.BlockFace;

public class BlockAdjustment {

    public static Point adjustPoint(Point point, BlockFace newOrientation, boolean shouldInvert) {
        Pos pos = Pos.fromPoint(point);
        double x = shouldInvert ? -pos.x() : pos.x();
        float yaw = shouldInvert ? -pos.yaw() : pos.yaw();

        switch (newOrientation) {
            case NORTH -> {
                return new Pos(-x, pos.y(), -pos.z(), yaw + 180.0F, pos.pitch());
            }
            case SOUTH -> {
                return new Pos(x, pos.y(), pos.z(), yaw, pos.pitch());
            }
            case WEST -> {
                return new Pos(-pos.z(), pos.y(), x, yaw + 90.0F, pos.pitch());
            }
            case EAST -> {
                return new Pos(pos.z(), pos.y(), -x, yaw - 90.0F, pos.pitch());
            }
        }

        return null;
    }

    public static BlockFace adjustFace(BlockFace previousFace, BlockFace newOrientation, boolean shouldInvert) {
        if (shouldInvert && (previousFace == BlockFace.WEST || previousFace == BlockFace.EAST)) {
            previousFace = previousFace.getOppositeFace();
        }

        switch (previousFace) {
            case SOUTH -> {
                return newOrientation;
            }
            case NORTH -> {
                return newOrientation.getOppositeFace();
            }
            case WEST -> {
                switch (newOrientation) {
                    case SOUTH -> {
                        return BlockFace.WEST;
                    }
                    case NORTH -> {
                        return BlockFace.EAST;
                    }
                    case WEST -> {
                        return BlockFace.NORTH;
                    }
                    case EAST -> {
                        return BlockFace.SOUTH;
                    }
                }
            }
            case EAST -> {
                switch (newOrientation) {
                    case SOUTH -> {
                        return BlockFace.EAST;
                    }
                    case NORTH -> {
                        return BlockFace.WEST;
                    }
                    case WEST -> {
                        return BlockFace.SOUTH;
                    }
                    case EAST -> {
                        return BlockFace.NORTH;
                    }
                }
            }
        }

        return null;
    }

    public static String adjustFacingProperty(String facingProperty, BlockFace newOrientation, boolean shouldInvert) {
        if (shouldInvert) {
            facingProperty = switch (facingProperty) {
                case "west" -> "east";
                case "east" -> "west";
                default -> facingProperty;
            };
        }
        switch (newOrientation) {
            case SOUTH -> {
                return facingProperty;
            }
            case NORTH -> {
                switch (facingProperty) {
                    case "north" -> {
                        return "south";
                    }
                    case "south" -> {
                        return "north";
                    }
                    case "west" -> {
                        return "east";
                    }
                    case "east" -> {
                        return "west";
                    }
                }
            }
            case WEST -> {
                switch (facingProperty) {
                    case "north" -> {
                        return "east";
                    }
                    case "south" -> {
                        return "west";
                    }
                    case "west" -> {
                        return "north";
                    }
                    case "east" -> {
                        return "south";
                    }
                }
            }
            case EAST -> {
                switch (facingProperty) {
                    case "north" -> {
                        return "west";
                    }
                    case "south" -> {
                        return "east";
                    }
                    case "west" -> {
                        return "south";
                    }
                    case "east" -> {
                        return "north";
                    }
                }
            }
        }

        return null;
    }

}
