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

import blue.endless.jankson.Comment;

import grondag.tdnf.config.Configurator.ActiveWhen;
import grondag.tdnf.config.Configurator.FallCondition;

public class ConfigData {
	// BLOCKS

	@Comment("When do trees break? (NO_SUPPORT, LOG_BREAK, or USE_TOOL)")
	public FallCondition fallCondition = FallCondition.NO_SUPPORT;

	@Comment("Leaves decay instantly. Ignored (leaves decay) when keepLogsIntact is true.")
	public boolean fastLeafDecay = true;

	@Comment("Log blocks move to the ground instead of dropping as items. Can be laggy. Leaves alwasy break when true.")
	public boolean keepLogsIntact = false;

	@Comment("Render falling logs? (Affects client side only.) Can be laggy.")
	public boolean renderFallingLogs = false;

	@Comment("Falling logs break leaves and other plants on the way down.")
	public boolean fallingLogsBreakPlants = false;

	@Comment("Falling logs break glass and other fragile blocks.")
	public boolean fallingLogsBreakFragile = false;

	@Comment("Players can sneak (or not sneak) to disable mod for building. (SNEAKING, NOT_SNEAKING, or ALWAYS)")
	public ActiveWhen activeWhen = ActiveWhen.NOT_SNEAKING;

	@Comment("Break large fungal wart blocks and Shroomlamps")
	public boolean breakFungalLeaves = true;

	// PLAYERS

	@Comment("Place dropped items directly into player inventory. (Good for skyblock)")
	public boolean directDeposit = false;

	@Comment("Apply fortune from axe used to fell a tree. (If an axe was used.)")
	public boolean applyFortune = false;

	@Comment("Remove durability from an axe used to fell a tree. (If an axe was used.)")
	public boolean consumeDurability = false;

	@Comment("Tools take durability loss from leaves as well as logs.")
	public boolean leafDurability = false;

	@Comment("Don't break tools when using durability.")
	public boolean protectTools = true;

	@Comment("Don't break blocks placed by players.")
	public boolean protectPlacedBlocks = true;

	@Comment("Players gain hunger from felling trees, as if they had broken each log.")
	public boolean applyHunger = true;

	@Comment("Players gain hunger from leaves in addition to logs.")
	public boolean leafHunger = false;

	@Comment("Max logs that can be broken by non-player mechanics like pistons, fire and TNT.")
	public int nonPlayerLogLimit = 256;

	@Comment("Logs that can be broken by a player without using a tool.")
	public int playerBaseLogLimit = 256;

	@Comment("Additional logs that can be broken when player uses a tool, per tier of tool. Set zero to disable.")
	public int toolTierLogBonus = 0;

	@Comment("When true, log breaking limit is multiplied by Efficency enchantment level.")
	public boolean enableEfficiencyLogMultiplier = false;

	// PERFORMANCE

	@Comment("Consolidate item drops into stacks to prevent lag.")
	public boolean stackDrops = true;

	//        @Comment("What counts as support for logs? BOTTOM, BOTTOM_OR_ALL_SIDE, or BOTTOM_OR_ANY_SIDE")
	//        public SupportSurface minimumSupportSurface;

	@Comment("Play particles and sounds? Number is max effects per second. 0-20")
	public int effectsPerSecond = 4;

	@Comment("Maximum number of concurrent breaking tasks in each world. 1-256")
	public int maxJobsPerWorld = 16;

	@Comment("Max log/leaf blocks to break per second, per tree. 1 - 2560")
	public int maxBreaksPerSecond = 640;

	@Comment("Max percentage of each server tick that can be used by TDNF in each world. 1 - 5")
	public int tickBudget = 1;

	@Comment("Max number of active falling block entities. 1 - 64")
	public int maxFallingBlocks = 16;

	@Comment("Tree cutting jobs will be abandoned if they take longer than this number of seconds. Use larger values if breaking speed is slow. 20-1800")
	public int jobTimeoutSeconds = 360;

	@Comment("IDs of modded blocks to be handled the same as Minecraft big mushrooms. Not strictly needed if the block is in the LOGS tag or is a subtype of MushroomBlock.")
	public String[] moddedMushroomBlocks = {
		"byg:weeping_milkcap_mushroom_block",
		"byg:green_mushroom_block",
		"byg:soul_shroom_stem",
		"byg:death_cap_mushroom_block"
	};

	@Comment("IDs of modded blocks to handle the same as vanilla Warped/Crimson stems. Activates dropping of wart blocks / shroomlights.")
	public String[] moddedFungusLogs = {
		"cinderscapes:umbral_stem",
		"byg:sythian_stems"
	};

	@Comment("IDs of modded blocks to handle the same as vanilla Warped/Crimson wart blocks and shroomlights.")
	public String[] moddedFungusLeaves = {
		"cinderscapes:umbral_flesh_block",
		"cinderscapes:umbral_wart_block",
		"byg:sythian_wart_block",
		"byg:shulkren_wart_block"
	};
}
