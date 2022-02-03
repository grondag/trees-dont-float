/*
 * This file is part of Trees Do Not Float and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package grondag.tdnf.config;

import java.io.File;
import java.io.FileOutputStream;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Material;

import grondag.tdnf.Platform;
import grondag.tdnf.TreesDoNotFloat;

public class Configurator {
	public enum FallCondition {
		NO_SUPPORT, LOG_BREAK, USE_TOOL
	}

	public enum ActiveWhen {
		SNEAKING() {
			@Override
			public boolean test(Player playerEntity) {
				return playerEntity.isShiftKeyDown();
			}
		},

		NOT_SNEAKING() {
			@Override
			public boolean test(Player playerEntity) {
				return !playerEntity.isShiftKeyDown();
			}
		},

		ALWAYS;

		public boolean test(Player playerEntity) {
			return true;
		}
	}

	public static final ConfigData DEFAULTS = new ConfigData();
	private static final Gson GSON = new GsonBuilder().create();
	private static final Jankson JANKSON = Jankson.builder().build();

	// BLOCKS
	public static FallCondition fallCondition = DEFAULTS.fallCondition;
	public static boolean fastLeafDecay = DEFAULTS.fastLeafDecay;
	public static boolean keepLogsIntact = DEFAULTS.keepLogsIntact;
	public static boolean renderFallingLogs = DEFAULTS.renderFallingLogs;
	public static boolean fallingLogsBreakPlants = DEFAULTS.fallingLogsBreakPlants;
	public static boolean fallingLogsBreakFragile = DEFAULTS.fallingLogsBreakFragile;
	public static ActiveWhen activeWhen = DEFAULTS.activeWhen;
	public static ImmutableSet<String> moddedMushroomBlocks = ImmutableSet.copyOf(DEFAULTS.moddedMushroomBlocks);
	public static ImmutableSet<String> moddedFungusLogs = ImmutableSet.copyOf(DEFAULTS.moddedFungusLogs);
	public static ImmutableSet<String> moddedFungusLeaves = ImmutableSet.copyOf(DEFAULTS.moddedFungusLeaves);
	public static boolean breakFungalLeaves = DEFAULTS.breakFungalLeaves;

	// PLAYERS
	public static boolean directDeposit = DEFAULTS.directDeposit;
	public static boolean applyFortune = DEFAULTS.applyFortune;
	public static boolean consumeDurability = DEFAULTS.consumeDurability;
	public static boolean leafDurability = DEFAULTS.leafDurability;
	public static boolean protectTools = DEFAULTS.protectTools;
	public static boolean protectPlacedBlocks = DEFAULTS.protectPlacedBlocks;
	public static boolean applyHunger = DEFAULTS.applyHunger;
	public static boolean leafHunger = DEFAULTS.leafHunger;
	public static int nonPlayerLogLimit = DEFAULTS.nonPlayerLogLimit;
	public static int playerBaseLogLimit = DEFAULTS.playerBaseLogLimit;
	public static int toolTierLogBonus = DEFAULTS.toolTierLogBonus;
	public static boolean enableEfficiencyLogMultiplier = DEFAULTS.enableEfficiencyLogMultiplier;

	// PERFORMANCE
	public static boolean stackDrops = DEFAULTS.stackDrops;
	public static int effectsPerSecond = DEFAULTS.effectsPerSecond;
	public static int maxJobsPerWorld = DEFAULTS.maxJobsPerWorld;
	public static int maxBreaksPerSecond = DEFAULTS.maxBreaksPerSecond;
	public static int tickBudget = DEFAULTS.tickBudget;
	public static int maxFallingBlocks = DEFAULTS.maxFallingBlocks;
	public static int jobTimeoutSeconds = DEFAULTS.jobTimeoutSeconds;
	public static int jobTimeoutTicks = jobTimeoutSeconds * 20;

	public static boolean hasBreaking = fallingLogsBreakPlants || fallingLogsBreakFragile;

	//    public static SupportSurface logSupportSurface = DEFAULTS.minimumSupportSurface;

	public static final ObjectOpenHashSet<Material> BREAKABLES = new ObjectOpenHashSet<>();

	private static File configFile;

	public static void init() {
		configFile = new File(Platform.configDirectory().toFile(), "tdnf2.json5");

		if (configFile.exists()) {
			loadConfig();
		} else {
			createConfig();
		}
	}

	public static void readConfig(ConfigData config) {
		// BLOCKS
		fallCondition = config.fallCondition;
		fastLeafDecay = config.fastLeafDecay;
		keepLogsIntact = config.keepLogsIntact;
		renderFallingLogs = config.renderFallingLogs;
		fallingLogsBreakPlants = config.fallingLogsBreakPlants;
		fallingLogsBreakFragile = config.fallingLogsBreakFragile;
		activeWhen = config.activeWhen;
		moddedMushroomBlocks = ImmutableSet.copyOf(config.moddedMushroomBlocks);
		moddedFungusLogs = ImmutableSet.copyOf(config.moddedFungusLogs);
		moddedFungusLeaves = ImmutableSet.copyOf(config.moddedFungusLeaves);
		breakFungalLeaves = config.breakFungalLeaves;

		// PLAYERS
		directDeposit = config.directDeposit;
		applyFortune = config.applyFortune;
		consumeDurability = config.consumeDurability;
		leafDurability = config.leafDurability;
		protectTools = config.protectTools;
		protectPlacedBlocks = config.protectPlacedBlocks;

		applyHunger = config.applyHunger;
		leafHunger = config.leafHunger;
		nonPlayerLogLimit = Mth.clamp(config.nonPlayerLogLimit, 0, 256);
		playerBaseLogLimit = Mth.clamp(config.playerBaseLogLimit, 0, 256);
		toolTierLogBonus = Mth.clamp(config.toolTierLogBonus, 0, 64);
		enableEfficiencyLogMultiplier = config.enableEfficiencyLogMultiplier;

		// PERFORMANCE
		stackDrops = config.stackDrops;
		maxJobsPerWorld = Mth.clamp(config.maxJobsPerWorld, 1, 256);
		effectsPerSecond = Mth.clamp(config.effectsPerSecond, 0, 20);
		maxBreaksPerSecond = Mth.clamp(config.maxBreaksPerSecond, 1, 2560);
		tickBudget = Mth.clamp(config.tickBudget, 1, 5);
		maxFallingBlocks = Mth.clamp(config.maxFallingBlocks, 1, 64);
		jobTimeoutSeconds = Mth.clamp(config.jobTimeoutSeconds, 20, 1800);
		computeDerived();
	}

	private static void createConfig() {
		final ConfigData config = new ConfigData();
		readConfig(config);
		saveConfig(config);
	}

	private static ConfigData loadConfig() {
		ConfigData config = new ConfigData();

		try {
			final JsonObject configJson = JANKSON.load(configFile);
			final String regularized = configJson.toJson(false, false, 0);
			config = GSON.fromJson(regularized, ConfigData.class);
		} catch (final Exception e) {
			e.printStackTrace();
			TreesDoNotFloat.LOG.error("Unable to load config. Using default values.");
		}

		readConfig(config);

		return config;
	}

	public static void computeDerived() {
		hasBreaking = fallingLogsBreakPlants || fallingLogsBreakFragile;
		BREAKABLES.clear();

		if (fallingLogsBreakPlants) {
			BREAKABLES.add(Material.BAMBOO);
			BREAKABLES.add(Material.BAMBOO_SAPLING);
			BREAKABLES.add(Material.CACTUS);
			BREAKABLES.add(Material.LEAVES);
			BREAKABLES.add(Material.PLANT);
			BREAKABLES.add(Material.VEGETABLE);
		}

		if (fallingLogsBreakFragile) {
			BREAKABLES.add(Material.CLOTH_DECORATION);
			BREAKABLES.add(Material.WEB);
			BREAKABLES.add(Material.GLASS);
			BREAKABLES.add(Material.SAND);
		}

		jobTimeoutTicks = jobTimeoutSeconds * 20;
	}

	public static ConfigData writeConfig() {
		final ConfigData config = new ConfigData();

		// BLOCKS
		config.fallCondition = fallCondition;
		config.fastLeafDecay = fastLeafDecay;
		config.keepLogsIntact = keepLogsIntact;
		config.renderFallingLogs = renderFallingLogs;
		config.fallingLogsBreakPlants = fallingLogsBreakPlants;
		config.fallingLogsBreakFragile = fallingLogsBreakFragile;
		config.activeWhen = activeWhen;
		config.breakFungalLeaves = breakFungalLeaves;

		// PLAYERS
		config.directDeposit = directDeposit;
		config.applyFortune = applyFortune;
		config.consumeDurability = consumeDurability;
		config.leafDurability = leafDurability;
		config.protectTools = protectTools;
		config.protectPlacedBlocks = protectPlacedBlocks;
		config.applyHunger = applyHunger;
		config.leafHunger = leafHunger;
		config.nonPlayerLogLimit = nonPlayerLogLimit;
		config.playerBaseLogLimit = playerBaseLogLimit;
		config.toolTierLogBonus = toolTierLogBonus;
		config.enableEfficiencyLogMultiplier = enableEfficiencyLogMultiplier;

		// PERFORMANCE
		config.stackDrops = stackDrops;
		config.maxJobsPerWorld = maxJobsPerWorld;
		config.effectsPerSecond = effectsPerSecond;
		config.maxBreaksPerSecond = maxBreaksPerSecond;
		config.tickBudget = tickBudget;
		config.maxFallingBlocks = maxFallingBlocks;
		config.jobTimeoutSeconds = jobTimeoutSeconds;

		//        config.minimumSupportSurface = logSupportSurface;
		return config;
	}

	public static void saveConfig(ConfigData config) {
		try {
			final String result = JANKSON.toJson(config).toJson(true, true, 0);

			if (!configFile.exists()) {
				configFile.createNewFile();
			}

			try (FileOutputStream out = new FileOutputStream(configFile, false);) {
				out.write(result.getBytes());
				out.flush();
				out.close();
			}
		} catch (final Exception e) {
			e.printStackTrace();
			TreesDoNotFloat.LOG.error("Unable to save config.");
		}
	}
}
