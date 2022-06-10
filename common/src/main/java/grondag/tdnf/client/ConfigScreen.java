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

import java.util.List;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import grondag.tdnf.config.ConfigData;
import grondag.tdnf.config.Configurator;

public abstract class ConfigScreen extends Screen {
	protected final Screen parent;
	protected final ObjectArrayList<Label> labels = new ObjectArrayList<>();
	protected final ConfigData config;

	protected int controlWidth;
	protected int controlHeight;
	protected int padding;
	protected int lineHeight;
	protected int leftOffset;
	protected int middleOffset;
	protected int rightOffset;
	protected int fullControlWidth;
	protected int halfControlWidth;
	protected int rightMargin;
	protected int halfOffset;

	protected void initSizes() {
		controlWidth = Mth.clamp((width - 16) / 3, 120, 200);
		controlHeight = Mth.clamp(height / 12, 16, 20);
		padding = Mth.clamp(Math.min((width - controlWidth * 3) / 4, (height - controlHeight * 8) / 8), 2, 10);
		lineHeight = controlHeight + padding;
		leftOffset = width / 2 - controlWidth - padding - controlWidth / 2;
		middleOffset = width / 2 - controlWidth / 2;
		rightOffset = width / 2 + controlWidth / 2 + padding;
		rightMargin = rightOffset + controlWidth;
		fullControlWidth = rightMargin - leftOffset;
		halfControlWidth = (fullControlWidth - padding) / 2;
		halfOffset = rightMargin - halfControlWidth;
	}

	protected class Label {
		protected final Component label;
		protected final int center;
		protected final int top;

		Label(Component label, int center, int top) {
			this.label = label;
			this.center = center;
			this.top = top;
		}

		protected void render(PoseStack matrixStack) {
			drawCenteredString(matrixStack, font, label, center, top, 16777215);
		}
	}

	protected class Toggle extends Checkbox {
		protected final List<FormattedCharSequence> toolTip;

		protected Toggle(int left, int top, int width, int height, String label_name, boolean value) {
			super(left, top, width, height, Component.translatable("config.tdnf.value." + label_name), value);
			toolTip = minecraft.font.split(Component.translatable("config.tdnf.help." + label_name), 200);
		}

		@Override
		public void renderButton(PoseStack matrices, int mouseX, int mouseY, float delta) {
			super.renderButton(matrices, mouseX, mouseY, delta);

			if (isHovered) {
				ConfigScreen.this.renderTooltip(matrices, toolTip, mouseX, mouseY);
			}
		}
	}

	protected class CycleButton<V extends Enum<?>> extends AbstractButton {
		protected final List<FormattedCharSequence> toolTip;
		protected final V[] values;
		protected final Component[] valueLabels;
		protected V value;

		protected CycleButton(int left, int top, int width, int height, String label_name, Class<V> e, V value) {
			super(left, top, width, height, Component.translatable("config.tdnf.value." + label_name));
			this.values = e.getEnumConstants();
			this.value = value;
			this.valueLabels = new Component[values.length];

			final String baseLabel = I18n.get("config.tdnf.value." + label_name) + ": ";

			for (int i = 0; i < values.length; ++i) {
				valueLabels[i] = Component.literal(baseLabel + I18n.get("config.tdnf.value." + label_name + "." + values[i].name().toLowerCase()));
			}

			toolTip = minecraft.font.split(Component.translatable("config.tdnf.help." + label_name), 200);
			setMessage(valueLabels[value.ordinal()]);
		}

		@Override
		public void updateNarration(NarrationElementOutput var1) {
			// TODO implement?
		}

		@Override
		public void onPress() {
			if (Screen.hasShiftDown()) {
				this.cycleValue(-1);
			} else {
				this.cycleValue(1);
			}
		}

		protected void cycleValue(int i) {
			final int index = Mth.positiveModulo(value.ordinal() + i, values.length);
			value = values[index];
			setMessage(valueLabels[index]);
		}

		protected V getValue() {
			return value;
		}

		protected void setValue(V value) {
			this.value = value;
			setMessage(valueLabels[value.ordinal()]);
		}

		@Override
		public void renderButton(PoseStack matrices, int mouseX, int mouseY, float delta) {
			super.renderButton(matrices, mouseX, mouseY, delta);

			if (isHovered) {
				ConfigScreen.this.renderTooltip(matrices, toolTip, mouseX, mouseY);
			}
		}
	}

	protected class Slider extends AbstractSliderButton {
		protected final int min;
		protected final int max;
		protected int intValue;
		protected final String baseLabel;
		protected final List<FormattedCharSequence> toolTip;

		protected Slider(int left, int top, int width, int height, String label_name, int min, int max, int value) {
			super(left, top, width, height, Component.translatable("config.tdnf.value." + label_name), normalize(min, max, value));
			toolTip = minecraft.font.split(Component.translatable("config.tdnf.help." + label_name), 200);
			this.intValue = value;
			this.min = min;
			this.max = max;
			baseLabel = I18n.get("config.tdnf.value." + label_name) + ": ";
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			this.setMessage(Component.literal(baseLabel + intValue));
		}

		@Override
		protected void applyValue() {
			intValue = (int) Mth.lerp(Mth.clamp(this.value, 0.0, 1.0), min, max);
		}

		protected int getValue() {
			return intValue;
		}

		protected void setValue(int value) {
			this.intValue = value;
			this.value = normalize(min, max, value);
			updateMessage();
		}

		protected static double normalize(int min, int max, int value) {
			value = Mth.clamp(value, min, max);
			return (value - min) / (double) (max - min + 1);
		}

		@Override
		public void renderButton(PoseStack matrices, int mouseX, int mouseY, float delta) {
			super.renderButton(matrices, mouseX, mouseY, delta);

			if (isHovered) {
				ConfigScreen.this.renderTooltip(matrices, toolTip, mouseX, mouseY);
			}
		}
	}

	public ConfigScreen(Screen parent, ConfigData config) {
		super(Component.translatable("config.tdnf.title"));
		this.parent = parent;
		this.config = config;
	}

	@Override
	public void removed() {
		// NOOP
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	@Override
	protected void init() {
		initSizes();
		labels.clear();
		clearWidgets();

		labels.add(new Label(title, width / 2, padding));

		addControls();

		addRenderableWidget(new Button(width / 2 - 120 - padding / 2, height - lineHeight, 120, controlHeight, CommonComponents.GUI_CANCEL, (buttonWidget) -> {
			minecraft.setScreen(parent);
		}));

		addRenderableWidget(new Button(width / 2 + padding / 2, height - lineHeight, 120, controlHeight, Component.translatable("config.tdnf.value.save"), (buttonWidget) -> {
			saveValues();
			Configurator.readConfig(config);
			Configurator.saveConfig(config);
			minecraft.setScreen(parent);
		}));
	}

	protected abstract void addControls();

	protected abstract void saveValues();

	@Override
	public void render(PoseStack matrixStack, int i, int j, float f) {
		renderDirtBackground(i);

		for (final var l : labels) {
			l.render(matrixStack);
		}

		super.render(matrixStack, i, j, f);
	}
}
