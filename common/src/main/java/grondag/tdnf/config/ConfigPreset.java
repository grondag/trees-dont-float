/*
 * This file is part of True Darkness and is licensed to the project under
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

import grondag.tdnf.config.Configurator.ActiveWhen;
import grondag.tdnf.config.Configurator.FallCondition;

public class ConfigPreset {
	public final String name;
	public final FallCondition fallCondition;
	public final ActiveWhen activeWhen;
	public final int nonPlayerLogLimit;
	public final int playerBaseLogLimit;
	public final int toolTierLogBonus;
	public final boolean fastLeafDecay;
	public final boolean keepLogsIntact;
	public final boolean fallingLogsBreakPlants;
	public final boolean fallingLogsBreakFragile;
	public final boolean breakFungalLeaves;
	public final boolean directDeposit;
	public final boolean applyFortune;
	public final boolean consumeDurability;
	public final boolean leafDurability;
	public final boolean applyHunger;
	public final boolean leafHunger;
	public final boolean enableEfficiencyLogMultiplier;

	private ConfigPreset(
			String name,
			FallCondition fallCondition,
			ActiveWhen activeWhen,
			int nonPlayerLogLimit,
			int playerBaseLogLimit,
			int toolTierLogBonus,
			int flags
	) {
		this.name = name;
		this.fallCondition = fallCondition;
		this.activeWhen = activeWhen;

		this.nonPlayerLogLimit = nonPlayerLogLimit;
		this.playerBaseLogLimit = playerBaseLogLimit;
		this.toolTierLogBonus = toolTierLogBonus;

		this.fastLeafDecay = (flags & FAST_LEAF_DECAY) != 0;
		this.keepLogsIntact = (flags & KEEP_LOGS_INTACT) != 0;
		this.fallingLogsBreakPlants = (flags & BREAK_PLANTS) != 0;
		this.fallingLogsBreakFragile = (flags & BREAK_FRAGILE) != 0;

		this.breakFungalLeaves = (flags & BREAK_FUNGAL) != 0;
		this.directDeposit = (flags & DIRECT_DEPOSIT) != 0;
		this.applyFortune = (flags & APPLY_FORTUNE) != 0;
		this.consumeDurability = (flags & CONSUME_DURABILITY) != 0;

		this.leafDurability = (flags & LEAF_DURABILITY) != 0;
		this.applyHunger = (flags & APPLY_HUNGER) != 0;
		this.leafHunger = (flags & LEAF_HUNGER) != 0;
		this.enableEfficiencyLogMultiplier = (flags & EFFICIENCY_MULTIPLIER) != 0;
	}

	public boolean matches(ConfigData data) {
		return data.fallCondition == fallCondition
			&& data.activeWhen == activeWhen

			&& data.nonPlayerLogLimit == nonPlayerLogLimit
			&& data.playerBaseLogLimit == playerBaseLogLimit
			&& data.toolTierLogBonus == toolTierLogBonus

			&& data.fastLeafDecay == fastLeafDecay
			&& data.keepLogsIntact == keepLogsIntact
			&& data.fallingLogsBreakPlants == fallingLogsBreakPlants
			&& data.fallingLogsBreakFragile == fallingLogsBreakFragile

			&& data.breakFungalLeaves == breakFungalLeaves
			&& data.directDeposit == directDeposit
			&& data.applyFortune == applyFortune
			&& data.consumeDurability == consumeDurability

			&& data.leafDurability == leafDurability
			&& data.applyHunger == applyHunger
			&& data.leafHunger == leafHunger
			&& data.enableEfficiencyLogMultiplier == enableEfficiencyLogMultiplier;
	}

	public void apply(ConfigData data) {
		data.fallCondition = fallCondition;
		data.activeWhen = activeWhen;

		data.nonPlayerLogLimit = nonPlayerLogLimit;
		data.playerBaseLogLimit = playerBaseLogLimit;
		data.toolTierLogBonus = toolTierLogBonus;

		data.fastLeafDecay = fastLeafDecay;
		data.keepLogsIntact = keepLogsIntact;
		data.fallingLogsBreakPlants = fallingLogsBreakPlants;
		data.fallingLogsBreakFragile = fallingLogsBreakFragile;

		data.breakFungalLeaves = breakFungalLeaves;
		data.directDeposit = directDeposit;
		data.applyFortune = applyFortune;
		data.consumeDurability = consumeDurability;

		data.leafDurability = leafDurability;
		data.applyHunger = applyHunger;
		data.leafHunger = leafHunger;
		data.enableEfficiencyLogMultiplier = enableEfficiencyLogMultiplier;
	}

	private static final int FAST_LEAF_DECAY = 1;
	private static final int KEEP_LOGS_INTACT = 2;
	private static final int BREAK_PLANTS = 4;
	private static final int BREAK_FRAGILE = 8;
	private static final int BREAK_FUNGAL = 16;
	private static final int DIRECT_DEPOSIT = 32;
	private static final int APPLY_FORTUNE = 64;
	private static final int CONSUME_DURABILITY = 128;
	private static final int LEAF_DURABILITY = 256;
	private static final int APPLY_HUNGER = 512;
	private static final int LEAF_HUNGER = 1024;
	private static final int EFFICIENCY_MULTIPLIER = 2048;

	public static final ConfigPreset DEFORESTATION = new ConfigPreset(
		"deforestation",
		FallCondition.NO_SUPPORT,
		ActiveWhen.NOT_SNEAKING,
		256, 256, 0,
		FAST_LEAF_DECAY | BREAK_FUNGAL | APPLY_HUNGER
	);

	public static final ConfigPreset PHYSICS = new ConfigPreset(
		"physics",
		FallCondition.NO_SUPPORT,
		ActiveWhen.ALWAYS,
		256, 256, 0,
		FAST_LEAF_DECAY | BREAK_FUNGAL | APPLY_HUNGER | KEEP_LOGS_INTACT | BREAK_PLANTS | BREAK_FRAGILE
	);

	public static final ConfigPreset LUMBERJACK = new ConfigPreset(
		"lumberjack",
		FallCondition.USE_TOOL,
		ActiveWhen.ALWAYS,
		0, 0, 8,
		FAST_LEAF_DECAY | BREAK_FUNGAL | APPLY_HUNGER | APPLY_FORTUNE | CONSUME_DURABILITY | EFFICIENCY_MULTIPLIER
	);

	public static final ConfigPreset PROGRESSION = new ConfigPreset(
		"progression",
		FallCondition.LOG_BREAK,
		ActiveWhen.NOT_SNEAKING,
		64, 8, 8,
		FAST_LEAF_DECAY | BREAK_FUNGAL | APPLY_HUNGER | APPLY_FORTUNE | CONSUME_DURABILITY | EFFICIENCY_MULTIPLIER
	);

	public static final ConfigPreset HARDCORE = new ConfigPreset(
		"hardcore",
		FallCondition.USE_TOOL,
		ActiveWhen.NOT_SNEAKING,
		0, 5, 5,
		FAST_LEAF_DECAY | BREAK_FUNGAL | APPLY_HUNGER | APPLY_FORTUNE | CONSUME_DURABILITY | LEAF_DURABILITY | EFFICIENCY_MULTIPLIER | KEEP_LOGS_INTACT | BREAK_PLANTS | BREAK_FRAGILE
	);

	public static final ConfigPreset SKYBLOCK = new ConfigPreset(
		"skyblock",
		FallCondition.LOG_BREAK,
		ActiveWhen.NOT_SNEAKING,
		64, 6, 6,
		FAST_LEAF_DECAY | BREAK_FUNGAL | APPLY_HUNGER | APPLY_FORTUNE | CONSUME_DURABILITY | EFFICIENCY_MULTIPLIER | DIRECT_DEPOSIT
	);
}
