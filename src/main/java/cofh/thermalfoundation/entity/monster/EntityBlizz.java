package cofh.thermalfoundation.entity.monster;

import cofh.core.CoFHProps;
import cofh.core.util.CoreUtils;
import cofh.lib.util.helpers.ItemHelper;
import cofh.lib.util.helpers.MathHelper;
import cofh.thermalfoundation.ThermalFoundation;
import cofh.thermalfoundation.entity.projectile.EntityBlitzBolt;
import cofh.thermalfoundation.entity.projectile.EntityBlizzBolt;
import cofh.thermalfoundation.item.ItemMaterial;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMoveTowardsRestriction;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;

import javax.annotation.Nullable;

public class EntityBlizz extends EntityElemental {

	static int entityId = -1;

	static boolean enable = true;
	static boolean restrictLightLevel = true;

	static int spawnLightLevel = 8;

	static int spawnWeight = 10;
	static int spawnMin = 1;
	static int spawnMax = 4;

	private static final ResourceLocation LOOT_TABLE_BLIZZ = new ResourceLocation(ThermalFoundation.modId, "entities/blizz");
	private static final SoundEvent attackSound = CoreUtils.getSoundEvent(ThermalFoundation.modId, "mob_blizz_attack");
	private static final SoundEvent ambientSound0 = CoreUtils.getSoundEvent(ThermalFoundation.modId, "mob_blizz_breathe0");
	private static final SoundEvent ambientSound1 = CoreUtils.getSoundEvent(ThermalFoundation.modId, "mob_blizz_breathe1");
	private static final SoundEvent ambientSound2 = CoreUtils.getSoundEvent(ThermalFoundation.modId, "mob_blizz_breathe2");
	private static final SoundEvent specialAmbientSound = CoreUtils.getSoundEvent(ThermalFoundation.modId, "mob_blizz_ambient");
	private static final SoundEvent[] ambientSounds = new SoundEvent[] {ambientSound0, ambientSound1, ambientSound2};

	static {
		String category = "Mob.Blizz";
		String comment = "";

		comment = "Set this to false to disable Blizzes entirely. Jerk.";
		enable = ThermalFoundation.CONFIG.get(category, "Enable", enable, comment);

		category = "Mob.Blizz.Spawn";

		comment = "Set this to false for Blizzes to spawn at any light level.";
		restrictLightLevel = ThermalFoundation.CONFIG.get(category, "Light.Limit", restrictLightLevel, comment);

		comment = "This sets the maximum light level Blizzes can spawn at, if restricted.";
		spawnLightLevel = MathHelper.clamp(ThermalFoundation.CONFIG.get(category, "Light.Level", spawnLightLevel, comment), 0, 15);

		comment = "This sets the minimum number of Blizzes that spawn in a group.";
		spawnMin = MathHelper.clamp(ThermalFoundation.CONFIG.get(category, "MinGroupSize", spawnMin, comment), 1, 10);

		comment = "This sets the maximum light number of Blizzes that spawn in a group.";
		spawnMax = MathHelper.clamp(ThermalFoundation.CONFIG.get(category, "MaxGroupSize", spawnMax, comment), spawnMin, 24);

		comment = "This sets the relative spawn weight for Blizzes.";
		spawnWeight = ThermalFoundation.CONFIG.get(category, "SpawnWeight", spawnWeight, comment);
	}

	public static void initialize() {

		if (!enable) {
			return;
		}
		entityId = CoreUtils.getEntityId();
		EntityRegistry.registerModEntity(EntityBlizz.class, "blizz", entityId, ThermalFoundation.instance, CoFHProps.ENTITY_TRACKING_DISTANCE, 1, true,
				0xE0FBFF, 0x6BE6FF);

		// Add Blizz spawn to Cold biomes
		List<Biome> validBiomes = new ArrayList<Biome>(Arrays.asList(BiomeDictionary.getBiomesForType(Type.COLD)));

		// Add Blizz spawn to Snowy biomes (in vanilla, all snowy are also cold)
		for (Biome biome : BiomeDictionary.getBiomesForType(Type.SNOWY)) {
			if (!validBiomes.contains(biome)) {
				validBiomes.add(biome);
			}
		}
		// Remove Blizz spawn from End biomes
		for (Biome biome : BiomeDictionary.getBiomesForType(Type.END)) {
			if (validBiomes.contains(biome)) {
				validBiomes.remove(biome);
			}
		}
		EntityRegistry.addSpawn(EntityBlizz.class, spawnWeight, spawnMin, spawnMax, EnumCreatureType.MONSTER, validBiomes.toArray(new Biome[0]));

		LootTableList.register(LOOT_TABLE_BLIZZ);

		GameRegistry.register(attackSound.setRegistryName(new ResourceLocation(ThermalFoundation.modId, "mob_blizz_attack")));
		GameRegistry.register(ambientSound0.setRegistryName(new ResourceLocation(ThermalFoundation.modId, "mob_blizz_breathe0")));
		GameRegistry.register(ambientSound1.setRegistryName(new ResourceLocation(ThermalFoundation.modId, "mob_blizz_breathe1")));
		GameRegistry.register(ambientSound2.setRegistryName(new ResourceLocation(ThermalFoundation.modId, "mob_blizz_breathe2")));
		GameRegistry.register(specialAmbientSound.setRegistryName(new ResourceLocation(ThermalFoundation.modId, "mob_blizz_ambient")));
	}

	public EntityBlizz(World world) {

		super(world);
		this.tasks.addTask(4, new EntityBlizz.AIBlizzballAttack(this));
		this.tasks.addTask(5, new EntityAIMoveTowardsRestriction(this, 1.0D));
		this.tasks.addTask(7, new EntityAIWander(this, 1.0D));
		this.tasks.addTask(8, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
		this.tasks.addTask(8, new EntityAILookIdle(this));
		this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, true, new Class[0]));
		this.targetTasks.addTask(2, new EntityAINearestAttackableTarget(this, EntityPlayer.class, true));

		ambientParticle = EnumParticleTypes.SNOWBALL;
	}

	@Override
	protected SoundEvent[] getAmbientSounds() {

		return ambientSounds;
	}

	@Override
	protected SoundEvent getSpecialAmbientSound() {
		return specialAmbientSound;
	}

	@Nullable
	@Override
	protected ResourceLocation getLootTable() {
		return LOOT_TABLE_BLIZZ;
	}

	@Override
	protected boolean restrictLightLevel() {

		return restrictLightLevel;
	}

	@Override
	protected int getSpawnLightLevel() {

		return spawnLightLevel;
	}

	/* ATTACK */
	static class AIBlizzballAttack extends AIElementalboltAttack {

		public AIBlizzballAttack(EntityElemental entity) {

			super(entity);
		}

		@Override
		protected EntityThrowable getBolt(World world, EntityElemental elemental) {

			return new EntityBlizzBolt(world, elemental);
		}

		@Override
		protected SoundEvent getAttackSound() {

			return attackSound;
		}
	}
}
