package com.electrifiedded.printerblock;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/**
 * The 44x44 x 100-layer drawing volume above a printer.
 * Row 0 is the top row; layer 0 is the plane directly above the printer.
 */
public final class PrintPlane {
    public static final int W = 44, H = 44, LAYERS = 100;
    public static final int EMPTY = 16;

    public final BlockPos origin; // block above the printer
    public final EnumFacing facing;
    public final boolean flipped;

    public PrintPlane(BlockPos printerPos, EnumFacing facing, boolean flipped) {
        this.origin = printerPos.up();
        this.facing = facing;
        this.flipped = flipped;
    }

    /** World position of drawing pixel (col,row) on the given layer. */
    public BlockPos pos(int col, int row, int layer) {
        int dir = flipped ? -1 : 1;
        int step = flipped ? (W - 1 - col) : col;
        int cx, cz, lx, lz;
        switch (facing) {
            case NORTH: cx = -dir; cz = 0; lx = 0; lz = 1; break;
            case SOUTH: cx = dir; cz = 0; lx = 0; lz = -1; break;
            case WEST: cx = 0; cz = dir; lx = 1; lz = 0; break;
            default: cx = 0; cz = -dir; lx = -1; lz = 0; break; // EAST
        }
        return origin.add(cx * step + lx * layer, H - 1 - row, cz * step + lz * layer);
    }
}
