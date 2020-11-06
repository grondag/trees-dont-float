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

package grondag.tdnf.client;

import java.util.Arrays;
import java.util.stream.Collectors;

import grondag.tdnf.Configurator;
import grondag.tdnf.Configurator.ActiveWhen;
import grondag.tdnf.Configurator.FallCondition;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;

import static grondag.tdnf.Configurator.DEFAULTS;
import static grondag.tdnf.Configurator.activeWhen;
import static grondag.tdnf.Configurator.applyFortune;
import static grondag.tdnf.Configurator.applyHunger;
import static grondag.tdnf.Configurator.consumeDurability;
import static grondag.tdnf.Configurator.directDeposit;
import static grondag.tdnf.Configurator.effectsPerSecond;
import static grondag.tdnf.Configurator.fallCondition;
import static grondag.tdnf.Configurator.fallingLogsBreakFragile;
import static grondag.tdnf.Configurator.fallingLogsBreakPlants;
import static grondag.tdnf.Configurator.fastLeafDecay;
import static grondag.tdnf.Configurator.jobTimeoutTicks;
import static grondag.tdnf.Configurator.keepLogsIntact;
import static grondag.tdnf.Configurator.leafDurability;
import static grondag.tdnf.Configurator.leafHunger;
import static grondag.tdnf.Configurator.maxBreaksPerTick;
import static grondag.tdnf.Configurator.maxFallingBlocks;
import static grondag.tdnf.Configurator.maxJobsPerWorld;
import static grondag.tdnf.Configurator.protectTools;
import static grondag.tdnf.Configurator.renderFallingLogs;
import static grondag.tdnf.Configurator.stackDrops;
import static grondag.tdnf.Configurator.tickBudget;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ConfigScreen {
	private static ConfigEntryBuilder ENTRY_BUILDER = ConfigEntryBuilder.create();

	static Text[] parse(String key) {
		return Arrays.stream(I18n.translate(key).split(";")).map(s ->  new LiteralText(s)).collect(Collectors.toList()).toArray(new Text[0]);
	}

	static Screen getScreen(Screen parent) {
		final ConfigBuilder builder = ConfigBuilder.create()
		.setParentScreen(parent)
		.setTitle(new TranslatableText("config.tdnf.title"))
		.setSavingRunnable(ConfigScreen::saveUserInput);

		builder.setGlobalized(true);
		builder.setGlobalizedExpanded(false);

		// BLOCKS
		final ConfigCategory blocks = builder.getOrCreateCategory(new TranslatableText("config.tdnf.category.blocks"));

		blocks.addEntry(ENTRY_BUILDER.startEnumSelector(
			new TranslatableText("config.tdnf.value.fall_condition"),
			FallCondition.class,
			fallCondition)
			.setDefaultValue(DEFAULTS.fallCondition)
			.setSaveConsumer(b -> fallCondition = b)
			.setEnumNameProvider(a -> new LiteralText(a.toString()))
			.setTooltip(parse("config.tdnf.help.fall_condition"))
			.build());

		blocks.addEntry(ENTRY_BUILDER.startBooleanToggle(new TranslatableText("config.tdnf.value.fast_leaf_decat"), fastLeafDecay)
			.setDefaultValue(DEFAULTS.fastLeafDecay)
			.setSaveConsumer(b -> fastLeafDecay = b)
			.setTooltip(parse("config.tdnf.help.fast_leaf_decat"))
			.build());

		blocks.addEntry(ENTRY_BUILDER.startBooleanToggle(new TranslatableText("config.tdnf.value.keep_logs_intact"), keepLogsIntact)
			.setDefaultValue(DEFAULTS.keepLogsIntact)
			.setSaveConsumer(b -> keepLogsIntact = b)
			.setTooltip(parse("config.tdnf.help.keep_logs_intact"))
			.build());

		blocks.addEntry(ENTRY_BUILDER.startBooleanToggle(new TranslatableText("config.tdnf.value.render_falling"), renderFallingLogs)
			.setDefaultValue(DEFAULTS.renderFallingLogs)
			.setSaveConsumer(b -> renderFallingLogs = b)
			.setTooltip(parse("config.tdnf.help.render_falling"))
			.build());

		blocks.addEntry(ENTRY_BUILDER.startBooleanToggle(new TranslatableText("config.tdnf.value.break_leaves"), fallingLogsBreakPlants)
			.setDefaultValue(DEFAULTS.fallingLogsBreakPlants)
			.setSaveConsumer(b -> fallingLogsBreakPlants = b)
			.setTooltip(parse("config.tdnf.help.break_leaves"))
			.build());

		blocks.addEntry(ENTRY_BUILDER.startBooleanToggle(new TranslatableText("config.tdnf.value.break_fragile"), fallingLogsBreakFragile)
			.setDefaultValue(DEFAULTS.fallingLogsBreakFragile)
			.setSaveConsumer(b -> fallingLogsBreakFragile = b)
			.setTooltip(parse("config.tdnf.help.break_fragile"))
			.build());

		blocks.addEntry(ENTRY_BUILDER.startEnumSelector(
			new TranslatableText("config.tdnf.value.active_when"),
			ActiveWhen.class,
			activeWhen)
			.setDefaultValue(DEFAULTS.activeWhen)
			.setSaveConsumer(b -> activeWhen = b)
			.setEnumNameProvider(a -> new LiteralText(a.toString()))
			.setTooltip(parse("config.tdnf.help.active_when"))
			.build());


		// PLAYERS
		final ConfigCategory players = builder.getOrCreateCategory(new TranslatableText("config.tdnf.category.players"));

		players.addEntry(ENTRY_BUILDER.startBooleanToggle(new TranslatableText("config.tdnf.value.direct_deposit"), directDeposit)
			.setDefaultValue(DEFAULTS.directDeposit)
			.setSaveConsumer(b -> directDeposit = b)
			.setTooltip(parse("config.tdnf.help.direct_deposit"))
			.build());

		players.addEntry(ENTRY_BUILDER.startBooleanToggle(new TranslatableText("config.tdnf.value.apply_fortune"), applyFortune)
			.setDefaultValue(DEFAULTS.applyFortune)
			.setSaveConsumer(b -> applyFortune = b)
			.setTooltip(parse("config.tdnf.help.apply_fortune"))
			.build());

		players.addEntry(ENTRY_BUILDER.startBooleanToggle(new TranslatableText("config.tdnf.value.consume_durability"), consumeDurability)
			.setDefaultValue(DEFAULTS.consumeDurability)
			.setSaveConsumer(b -> consumeDurability = b)
			.setTooltip(parse("config.tdnf.help.consume_durability"))
			.build());

		players.addEntry(ENTRY_BUILDER.startBooleanToggle(new TranslatableText("config.tdnf.value.leaf_durability"), leafDurability)
			.setDefaultValue(DEFAULTS.leafDurability)
			.setSaveConsumer(b -> leafDurability = b)
			.setTooltip(parse("config.tdnf.help.leaf_durability"))
			.build());

		players.addEntry(ENTRY_BUILDER.startBooleanToggle(new TranslatableText("config.tdnf.value.protect_tools"), protectTools)
			.setDefaultValue(DEFAULTS.protectTools)
			.setSaveConsumer(b -> protectTools = b)
			.setTooltip(parse("config.tdnf.help.protect_tools"))
			.build());

		players.addEntry(ENTRY_BUILDER.startBooleanToggle(new TranslatableText("config.tdnf.value.apply_hunger"), applyHunger)
			.setDefaultValue(DEFAULTS.applyHunger)
			.setSaveConsumer(b -> applyHunger = b)
			.setTooltip(parse("config.tdnf.help.apply_hunger"))
			.build());

		players.addEntry(ENTRY_BUILDER.startBooleanToggle(new TranslatableText("config.tdnf.value.leaf_hunger"), leafHunger)
			.setDefaultValue(DEFAULTS.leafHunger)
			.setSaveConsumer(b -> leafHunger = b)
			.setTooltip(parse("config.tdnf.help.leaf_hunger"))
			.build());


		// PERFORMANCE
		final ConfigCategory performance = builder.getOrCreateCategory(new TranslatableText("config.tdnf.category.performance"));

		performance.addEntry(ENTRY_BUILDER.startBooleanToggle(new TranslatableText("config.tdnf.value.consolidate_drops"), stackDrops)
			.setDefaultValue(DEFAULTS.stackDrops)
			.setSaveConsumer(b -> stackDrops = b)
			.setTooltip(parse("config.tdnf.help.consolidate_drops"))
			.build());

		performance.addEntry(ENTRY_BUILDER.startIntSlider(new TranslatableText("config.tdnf.value.max_jobs_per_world"), maxJobsPerWorld, 1, 256)
			.setDefaultValue(DEFAULTS.maxJobsPerWorld)
			.setSaveConsumer(b -> maxJobsPerWorld = b)
			.setTooltip(parse("config.tdnf.help.max_jobs_per_world"))
			.build());

		performance.addEntry(ENTRY_BUILDER.startIntSlider(new TranslatableText("config.tdnf.value.effect_level"), effectsPerSecond, 0, 60)
			.setDefaultValue(DEFAULTS.effectsPerSecond)
			.setSaveConsumer(b -> effectsPerSecond = b)
			.setTooltip(parse("config.tdnf.help.effect_level"))
			.build());

		performance.addEntry(ENTRY_BUILDER.startIntSlider(new TranslatableText("config.tdnf.value.max_breaks_per_tick"), maxBreaksPerTick, 1, 128)
			.setDefaultValue(DEFAULTS.maxBreaksPerTick)
			.setSaveConsumer(b -> maxBreaksPerTick = b)
			.setTooltip(parse("config.tdnf.help.max_breaks_per_tick"))
			.build());

		performance.addEntry(ENTRY_BUILDER.startIntSlider(new TranslatableText("config.tdnf.value.tick_budget"), tickBudget, 1, 5)
			.setDefaultValue(DEFAULTS.tickBudget)
			.setSaveConsumer(b -> tickBudget = b)
			.setTooltip(parse("config.tdnf.help.tick_budget"))
			.build());

		performance.addEntry(ENTRY_BUILDER.startIntSlider(new TranslatableText("config.tdnf.value.max_falling_blocks"), maxFallingBlocks, 1, 64)
			.setDefaultValue(DEFAULTS.maxFallingBlocks)
			.setSaveConsumer(b -> maxFallingBlocks = b)
			.setTooltip(parse("config.tdnf.help.max_falling_blocks"))
			.build());

		performance.addEntry(ENTRY_BUILDER.startIntSlider(new TranslatableText("config.tdnf.value.job_timeout_ticks"), jobTimeoutTicks, 20, 2400)
			.setDefaultValue(DEFAULTS.jobTimeoutTicks)
			.setSaveConsumer(b -> jobTimeoutTicks = b)
			.setTooltip(parse("config.tdnf.help.job_timeout_ticks"))
			.build());

		builder.setDoesConfirmSave(false);

		return builder.build();
	}

	private static void saveUserInput() {
		Configurator.computeDerived();
		Configurator.saveConfig();
	}
}
