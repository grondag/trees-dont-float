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
import net.minecraft.network.chat.Component;

import grondag.tdnf.config.ConfigData;

public class PerformanceConfigScreen extends ConfigScreen {
	protected Slider maxJobsPerWorld;
	protected Slider effectsPerSecond;
	protected Slider maxBreaksPerSecond;

	protected Slider tickBudget;
	protected Slider maxFallingBlocks;
	protected Slider jobTimeoutSeconds;

	public PerformanceConfigScreen(Screen parent, ConfigData config) {
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

		// Mojang sliders don't render correctly when too wide
		final int sliderWidth = Math.min(380, fullControlWidth);
		final int sliderLeft = (width - sliderWidth) / 2;

		maxJobsPerWorld = addRenderableWidget(new Slider(sliderLeft, i, sliderWidth, controlHeight, "max_jobs_per_world", 1, 256, config.maxJobsPerWorld));
		i += lineHeight;

		effectsPerSecond = addRenderableWidget(new Slider(sliderLeft, i, sliderWidth, controlHeight, "effect_level", 0, 20, config.effectsPerSecond));
		i += lineHeight;

		maxBreaksPerSecond = addRenderableWidget(new Slider(sliderLeft, i, sliderWidth, controlHeight, "max_breaks_per_second", 1, 2560, config.maxBreaksPerSecond));
		i += lineHeight;

		tickBudget = addRenderableWidget(new Slider(sliderLeft, i, sliderWidth, controlHeight, "tick_budget", 1, 5, config.tickBudget));
		i += lineHeight;

		maxFallingBlocks = addRenderableWidget(new Slider(sliderLeft, i, sliderWidth, controlHeight, "max_falling_blocks", 1, 64, config.maxFallingBlocks));
		i += lineHeight;

		jobTimeoutSeconds = addRenderableWidget(new Slider(sliderLeft, i, sliderWidth, controlHeight, "job_timeout_seconds", 0, 4096, config.jobTimeoutSeconds));
		i += lineHeight;

		addRenderableWidget(Button.builder(Component.translatable("config.tdnf.value.presets"), (buttonWidget) -> {
			saveValues();
			minecraft.setScreen(new PresetConfigScreen(parent, config));
		}).bounds(leftOffset, i, halfControlWidth, controlHeight).build());

		addRenderableWidget(Button.builder(Component.translatable("config.tdnf.value.custom_config"), (buttonWidget) -> {
			saveValues();
			minecraft.setScreen(new DetailConfigScreen(parent, config));
		}).bounds(rightMargin - halfControlWidth, i, halfControlWidth, controlHeight).build());
	}

	@Override
	protected void saveValues() {
		config.maxJobsPerWorld = maxJobsPerWorld.getValue();
		config.effectsPerSecond = effectsPerSecond.getValue();
		config.maxBreaksPerSecond = maxBreaksPerSecond.getValue();
		config.tickBudget = tickBudget.getValue();
		config.maxFallingBlocks = maxFallingBlocks.getValue();
		config.jobTimeoutSeconds = jobTimeoutSeconds.getValue();
	}
}
