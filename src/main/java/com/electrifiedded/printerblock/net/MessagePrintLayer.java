package com.electrifiedded.printerblock.net;

import com.electrifiedded.printerblock.BlockPrinter;
import com.electrifiedded.printerblock.PrintPlane;
import com.electrifiedded.printerblock.PrinterBlock;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.BlockColored;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** One drawn layer sent client to server; the server builds it out of wool. */
public class MessagePrintLayer implements IMessage {
    public BlockPos printer;
    public int layer;
    public byte[] pixels = new byte[PrintPlane.W * PrintPlane.H];

    public MessagePrintLayer() {}

    public MessagePrintLayer(BlockPos printer, int layer, byte[] pixels) {
        this.printer = printer;
        this.layer = layer;
        this.pixels = pixels;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(printer.toLong());
        buf.writeByte(layer);
        buf.writeBytes(pixels);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        printer = BlockPos.fromLong(buf.readLong());
        layer = buf.readUnsignedByte();
        buf.readBytes(pixels);
    }

    public static class Handler implements IMessageHandler<MessagePrintLayer, IMessage> {
        @Override
        public IMessage onMessage(MessagePrintLayer m, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> apply(player, m));
            return null;
        }
    }

    static void apply(EntityPlayerMP player, MessagePrintLayer m) {
        World world = player.world;
        if (m.layer >= PrintPlane.LAYERS || player.getDistanceSq(m.printer) > 128 * 128 || !world.isBlockLoaded(m.printer)) return;
        IBlockState ps = world.getBlockState(m.printer);
        if (ps.getBlock() != PrinterBlock.printer) return;
        PrintPlane plane = new PrintPlane(m.printer, ps.getValue(BlockPrinter.FACING), ps.getValue(BlockPrinter.FLIPPED));
        for (int row = 0; row < PrintPlane.H; row++) {
            for (int col = 0; col < PrintPlane.W; col++) {
                int c = m.pixels[row * PrintPlane.W + col];
                BlockPos p = plane.pos(col, row, m.layer);
                if (!world.isBlockLoaded(p)) continue;
                IBlockState cur = world.getBlockState(p);
                boolean wool = cur.getBlock() == Blocks.WOOL;
                if (c < 0 || c >= PrintPlane.EMPTY) {
                    // 1.7 behaviour: an erased pixel turns existing wool back to white
                    if (wool) world.setBlockState(p, Blocks.WOOL.getDefaultState(), 3);
                } else if (wool || cur.getBlock().isAir(cur, world, p)) {
                    world.setBlockState(p, Blocks.WOOL.getDefaultState().withProperty(BlockColored.COLOR, EnumDyeColor.byMetadata(c)), 3);
                }
            }
        }
    }
}
