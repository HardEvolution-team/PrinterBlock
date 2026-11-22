package com.electrifiedded.printerblock.client;

import com.electrifiedded.printerblock.CommonProxy;
import com.electrifiedded.printerblock.PrintPlane;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;

public class ClientProxy extends CommonProxy {
    @Override
    public void openPrinter(EntityPlayer player, BlockPos printerPos, PrintPlane plane) {
        Minecraft.getMinecraft().displayGuiScreen(new GuiPrinter(player.world, printerPos, plane));
    }
}
