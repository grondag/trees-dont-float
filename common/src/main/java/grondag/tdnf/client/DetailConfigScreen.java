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

package grondag.tdnf.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;

import grondag.tdnf.config.ConfigData;
import grondag.tdnf.config.Configurator.ActiveWhen;
import grondag.tdnf.config.Configurator.FallCondition;

public class DetailConfigScreen extends ConfigScreen {
	// block toggles
	protected CycleButton<FallCondition> fallCondition;
	protected CycleButton<ActiveWhen> activeWhen;
	protected Toggle fastLeafDecay;
	protected Toggle breakFungalLeaves;
	protected Toggle keepLogsIntact;

	protected Toggle applyFortune;
	protected Toggle enableEfficiencyLogMultiplier;
	protected Toggle protectPlacedBlocks;

	protected Toggle consumeDurability;
	protected Toggle protectTools;
	protected Toggle leafDurability;

	protected Toggle applyHunger;
	protected Toggle leafHunger;
	protected Toggle directDeposit;

	protected Toggle renderFallingLogs;
	protected Toggle fallingLogsBreakPlants;
	protected Toggle fallingLogsBreakFragile;

	protected Slider nonPlayerLogLimit;
	protected Slider playerBaseLogLimit;
	protected Slider toolTierLogBonus;

	public DetailConfigScreen(Screen parent, ConfigData config) {
		super(parent, config);
	}

	@Override
	public void render(PoseStack matrixStack, int i, int j, float f) {
		renderDirtBackground(i);

		for (final var l : labels) {
			l.render(matrixStack);
		}

		super.render(matrixStack, i, j, f);
	}

	@Override
	protected void addControls() {
		int i = lineHeight;

		fallCondition = addRenderableWidget(new CycleButton<>(leftOffset, i, halfControlWidth, controlHeight, "fall_condition", FallCondition.class, config.fallCondition));
		activeWhen = addRenderableWidget(new CycleButton<>(halfOffset, i, halfControlWidth, controlHeight, "active_when", ActiveWhen.class, config.activeWhen));

		i += lineHeight;

		fastLeafDecay = addRenderableWidget(new Toggle(leftOffset, i, controlWidth, controlHeight, "fast_leaf_decay", config.fastLeafDecay));
		breakFungalLeaves = addRenderableWidget(new Toggle(middleOffset, i, controlWidth, controlHeight, "break_fungal_leaves", config.breakFungalLeaves));
		keepLogsIntact = addRenderableWidget(new Toggle(rightOffset, i, controlWidth, controlHeight, "keep_logs_intact", config.keepLogsIntact));

		i += lineHeight;

		applyFortune = addRenderableWidget(new Toggle(leftOffset, i, controlWidth, controlHeight, "apply_fortune", config.applyFortune));
		enableEfficiencyLogMultiplier = addRenderableWidget(new Toggle(middleOffset, i, controlWidth, controlHeight, "enable_efficiency_log_multiplier", config.enableEfficiencyLogMultiplier));
		protectPlacedBlocks = addRenderableWidget(new Toggle(rightOffset, i, controlWidth, controlHeight, "protect_placed_blocks", config.protectPlacedBlocks));

		i += lineHeight;

		consumeDurability = addRenderableWidget(new Toggle(leftOffset, i, controlWidth, controlHeight, "consume_durability", config.consumeDurability));
		protectTools = addRenderableWidget(new Toggle(middleOffset, i, controlWidth, controlHeight, "protect_tools", config.protectTools));
		leafDurability = addRenderableWidget(new Toggle(rightOffset, i, controlWidth, controlHeight, "leaf_durability", config.leafDurability));

		i += lineHeight;

		applyHunger = addRenderableWidget(new Toggle(leftOffset, i, controlWidth, controlHeight, "apply_hunger", config.applyHunger));
		leafHunger = addRenderableWidget(new Toggle(middleOffset, i, controlWidth, controlHeight, "leaf_hunger", config.leafHunger));
		directDeposit = addRenderableWidget(new Toggle(rightOffset, i, controlWidth, controlHeight, "direct_deposit", config.directDeposit));

		i += lineHeight;

		renderFallingLogs = addRenderableWidget(new Toggle(leftOffset, i, controlWidth, controlHeight, "render_falling", config.renderFallingLogs));
		fallingLogsBreakPlants = addRenderableWidget(new Toggle(middleOffset, i, controlWidth, controlHeight, "break_leaves", config.fallingLogsBreakPlants));
		fallingLogsBreakFragile = addRenderableWidget(new Toggle(rightOffset, i, controlWidth, controlHeight, "break_fragile", config.fallingLogsBreakFragile));

		i += lineHeight;

		nonPlayerLogLimit = addRenderableWidget(new Slider(leftOffset, i, controlWidth, controlHeight, "non_player_log_limit", 0, 4096, config.nonPlayerLogLimit));
		playerBaseLogLimit = addRenderableWidget(new Slider(middleOffset, i, controlWidth, controlHeight, "player_base_log_limit", 0, 4096, config.playerBaseLogLimit));
		toolTierLogBonus = addRenderableWidget(new Slider(rightOffset, i, controlWidth, controlHeight, "tool_tier_log_bonus", 0, 256, config.toolTierLogBonus));

		i += lineHeight;

		addRenderableWidget(new Button(leftOffset, i, halfControlWidth, controlHeight, new TranslatableComponent("config.tdnf.value.presets"), (buttonWidget) -> {
			saveValues();
			minecraft.setScreen(new PresetConfigScreen(parent, config));
		}));

		addRenderableWidget(new Button(halfOffset, i, halfControlWidth, controlHeight, new TranslatableComponent("config.tdnf.value.performance"), (buttonWidget) -> {
			saveValues();
			minecraft.setScreen(new PerformanceConfigScreen(parent, config));
		}));
	}

	@Override
	protected void saveValues() {
		config.fallCondition = fallCondition.getValue();
		config.activeWhen = activeWhen.getValue();

		config.fastLeafDecay = fastLeafDecay.selected();
		config.breakFungalLeaves = breakFungalLeaves.selected();
		config.keepLogsIntact = keepLogsIntact.selected();

		config.applyFortune = applyFortune.selected();
		config.enableEfficiencyLogMultiplier = enableEfficiencyLogMultiplier.selected();
		config.protectPlacedBlocks = protectPlacedBlocks.selected();

		config.consumeDurability = consumeDurability.selected();
		config.protectTools = protectTools.selected();
		config.leafDurability = leafDurability.selected();

		config.applyHunger = applyHunger.selected();
		config.leafHunger = leafHunger.selected();
		config.directDeposit = directDeposit.selected();

		config.renderFallingLogs = renderFallingLogs.selected();
		config.fallingLogsBreakPlants = fallingLogsBreakPlants.selected();
		config.fallingLogsBreakFragile = fallingLogsBreakFragile.selected();
	}
}
