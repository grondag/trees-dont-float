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

import static grondag.tdnf.Configurator.DEFAULTS;
import static grondag.tdnf.Configurator.breakCooldownTicks;
import static grondag.tdnf.Configurator.effectLevel;
import static grondag.tdnf.Configurator.fallingBlocks;
import static grondag.tdnf.Configurator.keepLogsIntact;
import static grondag.tdnf.Configurator.maxBreaksPerTick;
import static grondag.tdnf.Configurator.maxSearchPosPerTick;
import static grondag.tdnf.Configurator.preventSuffocation;
import static grondag.tdnf.Configurator.stackDrops;

import java.util.Optional;

import grondag.tdnf.Configurator;
import grondag.tdnf.Configurator.EffectLevel;
import me.shedaniel.cloth.api.ConfigScreenBuilder;
import me.shedaniel.cloth.api.ConfigScreenBuilder.SavedConfig;
import me.shedaniel.cloth.gui.entries.BooleanListEntry;
import me.shedaniel.cloth.gui.entries.EnumListEntry;
import me.shedaniel.cloth.gui.entries.IntegerSliderEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resource.language.I18n;

@Environment(EnvType.CLIENT)
public class ConfigScreen {
    @SuppressWarnings({ "unchecked", "rawtypes" })
    static Screen getScreen(Screen parent) {
        
        ConfigScreenBuilder builder = ConfigScreenBuilder.create(parent, "config.tdnf.title", ConfigScreen::saveUserInput);
        
        // FEATURES
        ConfigScreenBuilder.CategoryBuilder features = builder.addCategory("config.tdnf.category.features");
        
        features.addOption(new BooleanListEntry("config.tdnf.value.keep_logs_intact", keepLogsIntact, "config.tdnf.reset", 
                () -> DEFAULTS.keepLogsIntact, b -> keepLogsIntact = b, 
                () -> Optional.of(I18n.translate("config.tdnf.help.keep_logs_intact").split(";"))));
        
        features.addOption(new BooleanListEntry("config.tdnf.value.falling_blocks", fallingBlocks, "config.tdnf.reset", 
                () -> DEFAULTS.fallingBlocks, b -> fallingBlocks = b, 
                () -> Optional.of(I18n.translate("config.tdnf.help.falling_blocks").split(";"))));
        
        features.addOption(new BooleanListEntry("config.tdnf.value.prevent_suffocation", preventSuffocation, "config.tdnf.reset", 
                () -> DEFAULTS.preventSuffocation, b -> preventSuffocation = b, 
                () -> Optional.of(I18n.translate("config.tdnf.help.prevent_suffocation").split(";"))));
        
        features.addOption(new BooleanListEntry("config.tdnf.value.consolidate_drops", stackDrops, "config.tdnf.reset", 
                () -> DEFAULTS.stackDrops, b -> stackDrops = b, 
                () -> Optional.of(I18n.translate("config.tdnf.help.consolidate_drops").split(";"))));
        
        features.addOption(new EnumListEntry(
                "config.tdnf.value.effect_level", 
                EffectLevel.class, 
                effectLevel, 
                "config.tdnf.reset", 
                () -> DEFAULTS.effectLevel, 
                (b) -> effectLevel = (EffectLevel) b,
                a -> a.toString(),
                () -> Optional.of(I18n.translate("config.tdnf.help.effect_level").split(";"))));
        
        features.addOption(new IntegerSliderEntry("config.tdnf.value.max_breaks_per_tick", 1, 128, maxBreaksPerTick, "config.tdnf.reset", 
                () -> DEFAULTS.maxBreaksPerTick, b -> maxBreaksPerTick = b, 
                () -> Optional.of(I18n.translate("config.tdnf.help.max_breaks_per_tick").split(";"))));
        
        features.addOption(new IntegerSliderEntry("config.tdnf.value.break_cooldown_ticks", 0, 40, breakCooldownTicks, "config.tdnf.reset", 
                () -> DEFAULTS.breakCooldownTicks, b -> breakCooldownTicks = b, 
                () -> Optional.of(I18n.translate("config.tdnf.help.break_cooldown_ticks").split(";"))));
        
        features.addOption(new IntegerSliderEntry("config.tdnf.value.max_search_pos_per_tick", 1, 512, maxSearchPosPerTick, "config.tdnf.reset", 
                () -> DEFAULTS.maxSearchPosPerTick, b -> maxSearchPosPerTick = b, 
                () -> Optional.of(I18n.translate("config.tdnf.help.max_search_pos_per_tick").split(";"))));
        
        builder.setDoesConfirmSave(false);
        
        return builder.build();
    }
    
    private static void saveUserInput(SavedConfig config) {
        Configurator.saveConfig();
    }
}
