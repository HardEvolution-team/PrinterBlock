package com.electrifiedded.printerblock;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Printer: front face shows the print direction, left click flips it, right click opens the drawing GUI. */
public class BlockPrinter extends Block {
    public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
    public static final PropertyBool FLIPPED = PropertyBool.create("flipped");

    public BlockPrinter() {
        super(Material.GROUND);
        setHardness(0.2F);
        setSoundType(SoundType.METAL);
        setTranslationKey("TLPrinterBlock");
        setCreativeTab(CreativeTabs.DECORATIONS);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.SOUTH).withProperty(FLIPPED, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, FLIPPED);
    }

    // same metadata layout as 1.7: 2..5 facing, +5 when flipped
    @Override
    public int getMetaFromState(IBlockState s) {
        return s.getValue(FACING).getIndex() + (s.getValue(FLIPPED) ? 5 : 0);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        boolean f = meta > 6;
        EnumFacing face = EnumFacing.byIndex(f ? meta - 5 : meta);
        if (face.getAxis() == EnumFacing.Axis.Y) face = EnumFacing.SOUTH;
        return getDefaultState().withProperty(FACING, face).withProperty(FLIPPED, f);
    }

    @Override
    public IBlockState getStateForPlacement(World w, BlockPos pos, EnumFacing side, float hx, float hy, float hz, int meta, EntityLivingBase placer) {
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public void onBlockClicked(World world, BlockPos pos, EntityPlayer player) {
        world.setBlockState(pos, world.getBlockState(pos).cycleProperty(FLIPPED), 3);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hx, float hy, float hz) {
        if (world.isRemote && hand == EnumHand.MAIN_HAND) {
            PrinterBlock.proxy.openPrinter(player, pos, new PrintPlane(pos, state.getValue(FACING), state.getValue(FLIPPED)));
        }
        return true;
    }
}
