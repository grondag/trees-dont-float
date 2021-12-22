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

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;

import grondag.tdnf.config.ConfigData;
import grondag.tdnf.config.ConfigPreset;

public class PresetConfigScreen extends ConfigScreen {
	protected Toggle protectPlacedBlocks;
	protected Toggle protectTools;
	protected Toggle renderFallingLogs;

	protected PresetToggle presetDeforestation;
	protected PresetToggle presetLumberjack;
	protected PresetToggle presetPhysics;
	protected PresetToggle presetProgression;
	protected PresetToggle presetHardcore;
	protected PresetToggle presetSkyblock;

	private class PresetToggle extends Toggle {
		private final ConfigPreset preset;

		protected PresetToggle(int left, int top, int width, int height, String label_name, ConfigPreset preset) {
			super(left, top, width, height, label_name, preset.matches(config));
			this.preset = preset;
		}

		@Override
		public void onPress() {
			if (!this.selected()) {
				clearPresets();
				super.onPress();
				preset.apply(config);
			}
		}

		void clear() {
			if (selected()) {
				super.onPress();
			}
		}
	}

	public PresetConfigScreen(Screen parent, ConfigData config) {
		super(parent, config);
	}

	private void clearPresets() {
		presetDeforestation.clear();
		presetLumberjack.clear();
		presetPhysics.clear();
		presetProgression.clear();
		presetHardcore.clear();
		presetSkyblock.clear();
	}

	@Override
	protected void addControls() {
		int i = lineHeight;

		labels.add(new Label(new TranslatableComponent("config.tdnf.title.presets"), width / 2, i));
		i += lineHeight;

		presetDeforestation = addRenderableWidget(new PresetToggle(leftOffset, i, halfControlWidth, controlHeight, "preset.deforestation", ConfigPreset.DEFORESTATION));
		presetProgression = addRenderableWidget(new PresetToggle(halfOffset, i, halfControlWidth, controlHeight, "preset.progression", ConfigPreset.PROGRESSION));

		i += lineHeight;

		presetLumberjack = addRenderableWidget(new PresetToggle(leftOffset, i, halfControlWidth, controlHeight, "preset.lumberjack", ConfigPreset.LUMBERJACK));
		presetHardcore = addRenderableWidget(new PresetToggle(halfOffset, i, halfControlWidth, controlHeight, "preset.hardcore", ConfigPreset.HARDCORE));

		i += lineHeight;

		presetPhysics = addRenderableWidget(new PresetToggle(leftOffset, i, halfControlWidth, controlHeight, "preset.physics", ConfigPreset.PHYSICS));
		presetSkyblock = addRenderableWidget(new PresetToggle(halfOffset, i, halfControlWidth, controlHeight, "preset.skyblock", ConfigPreset.SKYBLOCK));

		i += lineHeight;
		i += lineHeight;

		protectPlacedBlocks = addRenderableWidget(new Toggle(rightOffset, i, controlWidth, controlHeight, "protect_placed_blocks", config.protectPlacedBlocks));
		protectTools = addRenderableWidget(new Toggle(middleOffset, i, controlWidth, controlHeight, "protect_tools", config.protectTools));
		renderFallingLogs = addRenderableWidget(new Toggle(leftOffset, i, controlWidth, controlHeight, "render_falling", config.renderFallingLogs));

		i += lineHeight;

		addRenderableWidget(new Button(leftOffset, i, halfControlWidth, controlHeight, new TranslatableComponent("config.tdnf.value.custom_config"), (buttonWidget) -> {
			saveValues();
			minecraft.setScreen(new DetailConfigScreen(parent, config));
		}));

		addRenderableWidget(new Button(halfOffset, i, halfControlWidth, controlHeight, new TranslatableComponent("config.tdnf.value.performance"), (buttonWidget) -> {
			saveValues();
			minecraft.setScreen(new PerformanceConfigScreen(parent, config));
		}));
	}

	@Override
	protected void saveValues() {
		config.protectPlacedBlocks = protectPlacedBlocks.selected();
		config.protectTools = protectTools.selected();
		config.renderFallingLogs = renderFallingLogs.selected();
	}
}
