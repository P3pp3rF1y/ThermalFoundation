package cofh.thermalfoundation.fluid;

import cofh.core.fluid.BlockFluidInteractive;
import cofh.core.util.CoreUtils;
import cofh.lib.util.BlockWrapper;
import cofh.thermalfoundation.ThermalFoundation;
import cofh.thermalfoundation.block.TFBlocks;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class BlockFluidMana extends BlockFluidInteractive {

	public static final int LEVELS = 6;
	public static final Material materialFluidMana = new MaterialLiquid(MapColor.PURPLE);

	private static boolean effect = true;
	private static boolean enableSourceFall = true;

	public BlockFluidMana(Fluid fluid) {

		super(fluid, materialFluidMana, "thermalfoundation", "mana");
		setQuantaPerBlock(LEVELS);
		setTickRate(10);

		setHardness(2000F);
		setLightOpacity(2);
		setParticleColor(0.2F, 0.0F, 0.4F);
	}

	@Override
	public int getWeakPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {

		return effect ? 15 : 0;
	}

	@Override
	public void onEntityCollidedWithBlock(World world, BlockPos pos, IBlockState state, Entity entity) {

		if (!effect || world.isRemote) {
			return;
		}
		if (world.getTotalWorldTime() % 4 == 0) {
			if (world.rand.nextInt(10) != 0) {
				return;
			}
			BlockPos pos2 = pos.add(-8 + world.rand.nextInt(17), world.rand.nextInt(8), -8 + world.rand.nextInt(17));

			if (!world.getBlockState(pos2).getMaterial().isSolid()) {
				CoreUtils.teleportEntityTo(entity, pos2.getX(), pos2.getY(), pos2.getZ());
			}
		}
	}

	@Override
	public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {

		if (effect) {
			checkForInteraction(world, pos, state);
		}
		if (enableSourceFall && getMetaFromState(state) == 0) {
			IBlockState stateDown = world.getBlockState(pos.add(0, densityDir, 0));
			Block block = stateDown.getBlock();

			if (block == this && getMetaFromState(stateDown) != 0) {
				world.setBlockState(pos.add(0, densityDir, 0), getDefaultState(), 2);
				world.setBlockToAir(pos);
				return;
			}
		}
		super.updateTick(world, pos, state, rand);
	}

	protected void checkForInteraction(World world, BlockPos pos, IBlockState state) {

		if (state.getBlock() != this) {
			return;
		}
		interactWithBlock(world, pos.add(0, -1, 0));
		interactWithBlock(world, pos.add(0, 1, 0));
		interactWithBlock(world, pos.add(-1, 0, 0));
		interactWithBlock(world, pos.add(1, 0, 0));
		interactWithBlock(world, pos.add(0, 0, -1));
		interactWithBlock(world, pos.add(0, 0, 1));
		interactWithBlock(world, pos.add(-2, 0, 0));
		interactWithBlock(world, pos.add(2, 0, 0));
		interactWithBlock(world, pos.add(0, 0, -2));
		interactWithBlock(world, pos.add(0, 0, 2));

		interactWithBlock(world, pos.add(-1, 0, -1));
		interactWithBlock(world, pos.add(-1, 0, 1));
		interactWithBlock(world, pos.add(1, 0, -1));
		interactWithBlock(world, pos.add(1, 0, 1));
	}

	protected void interactWithBlock(World world, BlockPos pos) {

		IBlockState state = world.getBlockState(pos);
		Block block = state.getBlock();

		if (block.isAir(state, world, pos) || block == this) {
			return;
		}
		int bMeta = block.getMetaFromState(state);
		BlockWrapper result;

		if (hasInteraction(block, bMeta)) {
			result = getInteraction(block, bMeta);
			world.setBlockState(pos, block.getStateFromMeta(bMeta), 2);
			triggerInteractionEffects(world, pos);
		} else if (world.isSideSolid(pos, EnumFacing.UP) && world.isAirBlock(pos.add(0, 1, 0))) {
			if (world.rand.nextInt(2) % 2 == 0) {
				world.setBlockState(pos.add(0, 1, 0), Blocks.SNOW_LAYER.getDefaultState(), 2);
			} else {
				world.setBlockState(pos.add(0, 1, 0), Blocks.FIRE.getDefaultState(), 2);
			}
		}
	}

	protected void triggerInteractionEffects(World world, BlockPos pos) {

		if (world.rand.nextInt(10) == 0) {
			world.playSound(pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.BLOCKS, 0.5F,
					2.6F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.8F, false);
		}
		for (int i = 0; i < 8; i++) {
			world.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE, pos.getX() + Math.random() * 1.1, pos.getY() + 1.3D, pos.getZ() + Math.random() * 1.1,
					0.0D, -0.5D, 0.0D, new int[0]);
		}
	}

	/* IInitializer */
	@Override
	public boolean preInit() {

		GameRegistry.register(this.setRegistryName(new ResourceLocation(ThermalFoundation.modId, "FluidMana")));

		addInteraction(Blocks.DIRT, 0, Blocks.GRASS);
		addInteraction(Blocks.DIRT, 1, Blocks.DIRT, 2);
		addInteraction(Blocks.GLASS, Blocks.SAND);

		//TODO figure out which of these are still needed
		// addInteraction(Blocks.stained_glass, -1, Blocks.glass);
		// addInteraction(Blocks.diamond_ore, -1, TFBlocks.blockOre, 1);
		// addInteraction(Blocks.cauldron, -1, Blocks.carpet, 1);
		// addInteraction(Blocks.cactus, -1, Blocks.cake, 5);
		// addInteraction(Blocks.enchanting_table, -1, Blocks.brewing_stand, 0);
		// addInteraction(Blocks.bookshelf, 0, Blocks.chest, 0);
		// addInteraction(Blocks.ender_chest, -1, TFBlocks.blockFluidEnder, 1);
		// addInteraction(Blocks.dragon_egg, -1, Blocks.bedrock, 1);
		addInteraction(Blocks.REDSTONE_ORE, -1, Blocks.LIT_REDSTONE_ORE, 0);
		addInteraction(Blocks.LAPIS_ORE, -1, Blocks.LAPIS_BLOCK, 0);
		addInteraction(Blocks.FARMLAND, -1, Blocks.MYCELIUM, 0);
		for (int i = 8; i-- > 0;) {
			addInteraction(Blocks.DOUBLE_STONE_SLAB, i, Blocks.DOUBLE_STONE_SLAB, i + 8);
		}
		addInteraction(TFBlocks.blockOre, 2, TFBlocks.blockOre, 6);
		addInteraction(TFBlocks.blockOre, 3, Blocks.GOLD_ORE);
		addInteraction(TFBlocks.blockStorage, 2, TFBlocks.blockStorage, 6);
		addInteraction(TFBlocks.blockStorage, 3, Blocks.GOLD_BLOCK);

		String category = "Fluid.Mana";
		String comment = "Enable this for Fluid Mana to do...things.";
		effect = ThermalFoundation.CONFIG.get(category, "Effect", true, comment);

		comment = "Enable this for Fluid Mana Source blocks to gradually fall downwards.";
		enableSourceFall = ThermalFoundation.CONFIG.get(category, "Fall", enableSourceFall, comment);

		return true;
	}

}
