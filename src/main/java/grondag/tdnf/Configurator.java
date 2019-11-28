/*******************************************************************************
 * Copyright 2019 grondag
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package grondag.tdnf;

import java.io.File;
import java.io.FileOutputStream;

import blue.endless.jankson.Comment;
import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import net.minecraft.block.Material;
import net.minecraft.util.math.MathHelper;

import net.fabricmc.loader.api.FabricLoader;

public class Configurator {
	//    public static enum SupportSurface {
	//        BOTTOM,
	//        BOTTOM_OR_ALL_SIDES,
	//        BOTTOM_OR_ANY_SIDE
	//    }

	public enum FallCondition {
		NO_SUPPORT, LOG_BREAK, USE_TOOL
	}

	@SuppressWarnings("hiding")
	public static class ConfigData {

		// BLOCKS

		@Comment("When do trees break? (NO_SUPPORT, LOG_BREAK, or USE_TOOL)")
		public FallCondition fallCondition = FallCondition.NO_SUPPORT;

		@Comment("Log blocks move to the ground instead of dropping as items. Can be laggy.")
		public boolean keepLogsIntact = false;

		@Comment("Render falling logs? (Affects client side only.) Can be laggy.")
		public boolean renderFallingLogs = false;

		@Comment("Falling logs break leaves and other plants on the way down.")
		public boolean fallingLogsBreakPlants = true;

		@Comment("Falling logs break glass and other fragile blocks.")
		public boolean fallingLogsBreakFragile = false;

		@Comment("Protect logs placed by players.")
		public boolean protectPlayerLogs = true;

		// PLAYERS

		@Comment("Place dropped items directly into player inventory. (Good for skyblock)")
		public boolean directDeposit = false;

		@Comment("Apply fortune from axe used to fell a tree. (If an axe was used.)")
		public boolean applyFortune = true;

		@Comment("Remove durability from an axe used to fell a tree. (If an axe was used.)")
		public boolean consumeDurability = true;

		@Comment("Tools take durability loss from leaves as well as logs.")
		public boolean leafDurability = false;

		@Comment("Don't break tools when using durability.")
		public boolean protectTools = true;

		@Comment("Players gain hunger from felling trees, as if they had broken each log.")
		public boolean applyHunger = true;

		@Comment("Players gain hunger from leaves in addition to logs.")
		public boolean leafHunger = false;

		// PERFORMANCE

		@Comment("Consolidate item drops into stacks to prevent lag.")
		public boolean stackDrops = true;

		//        @Comment("What counts as support for logs? BOTTOM, BOTTOM_OR_ALL_SIDE, or BOTTOM_OR_ANY_SIDE")
		//        public SupportSurface minimumSupportSurface;

		@Comment("Play particles and sounds? Number is max effects per second. 0-20")
		public int effectsPerSecond = 4;

		@Comment("Max log/leaf blocks to break per tick. 1 - 128")
		public int maxBreaksPerTick = 32;

		@Comment("Max percentage of each server tick that can be used by TDNF. 1 - 5")
		public int tickBudget = 1;

		@Comment("Max number of active falling block entities. 1 - 64")
		public int maxFallingBlocks = 16;

		@Comment("Tree cutting jobs will be abandoned if they take longer than tnis number of ticks. 20-2400")
		public int jobTimeoutTicks = 200;
	}

	public static final ConfigData DEFAULTS = new ConfigData();
	private static final Gson GSON = new GsonBuilder().create();
	private static final Jankson JANKSON = Jankson.builder().build();

	// BLOCKS
	public static FallCondition fallCondition = DEFAULTS.fallCondition;
	public static boolean keepLogsIntact = DEFAULTS.keepLogsIntact;
	public static boolean renderFallingLogs = DEFAULTS.renderFallingLogs;
	public static boolean fallingLogsBreakPlants = DEFAULTS.fallingLogsBreakPlants;
	public static boolean fallingLogsBreakFragile = DEFAULTS.fallingLogsBreakFragile;
	public static boolean protectPlayerLogs = DEFAULTS.protectPlayerLogs;

	// PLAYERS
	public static boolean directDeposit = DEFAULTS.directDeposit;
	public static boolean applyFortune = DEFAULTS.applyFortune;
	public static boolean consumeDurability = DEFAULTS.consumeDurability;
	public static boolean leafDurability = DEFAULTS.leafDurability;
	public static boolean protectTools = DEFAULTS.protectTools;
	public static boolean applyHunger = DEFAULTS.applyHunger;
	public static boolean leafHunger = DEFAULTS.leafHunger;

	// PERFORMANCE
	public static boolean stackDrops = DEFAULTS.stackDrops;
	public static int effectsPerSecond = DEFAULTS.effectsPerSecond;
	public static int maxBreaksPerTick = DEFAULTS.maxBreaksPerTick;
	public static int tickBudget = DEFAULTS.tickBudget;
	public static int maxFallingBlocks = DEFAULTS.maxFallingBlocks;
	public static int jobTimeoutTicks = DEFAULTS.jobTimeoutTicks;

	public static boolean hasBreaking = fallingLogsBreakPlants || fallingLogsBreakFragile;

	//    public static SupportSurface logSupportSurface = DEFAULTS.minimumSupportSurface;

	public static final ObjectOpenHashSet<Material> BREAKABLES = new ObjectOpenHashSet<>();

	private static File configFile;


	public static void init() {
		configFile = new File(FabricLoader.getInstance().getConfigDirectory(), "trees-do-not-float.json5");
		if (configFile.exists()) {
			loadConfig();
		} else {
			saveConfig();
		}
	}

	private static void loadConfig() {
		ConfigData config = new ConfigData();
		try {
			final JsonObject configJson = JANKSON.load(configFile);
			final String regularized = configJson.toJson(false, false, 0);
			config = GSON.fromJson(regularized, ConfigData.class);
		} catch (final Exception e) {
			e.printStackTrace();
			TreesDoNotFloat.LOG.error("Unable to load config. Using default values.");
		}

		// BLOCKS
		fallCondition = config.fallCondition;
		keepLogsIntact = config.keepLogsIntact;
		renderFallingLogs = config.renderFallingLogs;
		fallingLogsBreakPlants = config.fallingLogsBreakPlants;
		fallingLogsBreakFragile = config.fallingLogsBreakFragile;
		protectPlayerLogs = config.protectPlayerLogs;

		// PLAYERS
		directDeposit = config.directDeposit;
		applyFortune = config.applyFortune;
		consumeDurability = config.consumeDurability;
		leafDurability = config.leafDurability;
		protectTools = config.protectTools;
		applyHunger = config.applyHunger;
		leafHunger = config.leafHunger;

		// PERFORMANCE
		stackDrops = config.stackDrops;
		effectsPerSecond = config.effectsPerSecond;
		maxBreaksPerTick = MathHelper.clamp(config.maxBreaksPerTick, 1, 128);
		tickBudget = MathHelper.clamp(config.tickBudget, 1, 20);
		maxFallingBlocks = MathHelper.clamp(config.maxFallingBlocks, 1, 64);
		jobTimeoutTicks = MathHelper.clamp(config.jobTimeoutTicks, 20, 2400);
		computeDerived();
		//        logSupportSurface = config.minimumSupportSurface;
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
			BREAKABLES.add(Material.PUMPKIN);
		}

		if (fallingLogsBreakFragile) {
			BREAKABLES.add(Material.CARPET);
			BREAKABLES.add(Material.COBWEB);
			BREAKABLES.add(Material.GLASS);
			BREAKABLES.add(Material.PART);
		}
	}

	public static void saveConfig() {
		final ConfigData config = new ConfigData();

		// BLOCKS
		config.fallCondition = fallCondition;
		config.keepLogsIntact = keepLogsIntact;
		config.renderFallingLogs = renderFallingLogs;
		config.fallingLogsBreakPlants = fallingLogsBreakPlants;
		config.fallingLogsBreakFragile = fallingLogsBreakFragile;
		config.protectPlayerLogs = protectPlayerLogs;

		// PLAYERS
		config.directDeposit = directDeposit;
		config.applyFortune = applyFortune;
		config.consumeDurability = consumeDurability;
		config.leafDurability = leafDurability;
		config.protectTools = protectTools;
		config.applyHunger = applyHunger;
		config.leafHunger = leafHunger;

		// PERFORMANCE
		config.stackDrops = stackDrops;
		config.effectsPerSecond = effectsPerSecond;
		config.maxBreaksPerTick = maxBreaksPerTick;
		config.tickBudget = tickBudget;
		config.maxFallingBlocks = maxFallingBlocks;
		config.jobTimeoutTicks = jobTimeoutTicks;

		//        config.minimumSupportSurface = logSupportSurface;

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
			return;
		}
	}
}
