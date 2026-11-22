package com.electrifiedded.printerblock;

import com.electrifiedded.archaiclib.legacy.registry.LegacyGameRegistry;
import com.electrifiedded.archaiclib.legacy.registry.LegacyRecipes;
import com.electrifiedded.archaiclib.original.OriginalModAssets;
import com.electrifiedded.printerblock.net.MessagePrintLayer;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.VERSION, dependencies = "required-after:archaiclib@[1.29.0,)")
public class PrinterBlock {
    public static final String ORIGINAL_NAME = "tlprinterblock_1.7.10.jar - TeeLuk's PrinterBlock for 1.7.10 (by pitman-87, TeeLuk)";
    public static final String ORIGINAL_URL = "http://www.minecraftforum.net/topic/967925-164sspsmplan-teeluks-printerblock-specialarmor/";

    @SidedProxy(clientSide = "com.electrifiedded.printerblock.client.ClientProxy", serverSide = "com.electrifiedded.printerblock.CommonProxy")
    public static CommonProxy proxy;

    public static SimpleNetworkWrapper NET;
    public static Block printer;

    @EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        OriginalModAssets.require(Reference.MOD_ID, "PrinterBlock", ORIGINAL_NAME, ORIGINAL_URL, "printerblock", "tlprinterblock");
        printer = LegacyGameRegistry.registerBlock(new BlockPrinter(), "tlprinterblock");
        LegacyGameRegistry.registerItemModel(printer, 0, Reference.MOD_ID + ":tlprinterblock", "inventory");
        NET = NetworkRegistry.INSTANCE.newSimpleChannel(Reference.MOD_ID);
        NET.registerMessage(MessagePrintLayer.Handler.class, MessagePrintLayer.class, 0, Side.SERVER);
    }

    @EventHandler
    public void init(FMLInitializationEvent e) {
        LegacyRecipes.addRecipe(new ItemStack(printer), "ABC", "#X#", "###",
                '#', Items.IRON_INGOT, 'X', new ItemStack(Blocks.WOOL, 1, 0),
                'A', new ItemStack(Items.DYE, 1, 1), 'B', new ItemStack(Items.DYE, 1, 2), 'C', new ItemStack(Items.DYE, 1, 4));
    }
}
