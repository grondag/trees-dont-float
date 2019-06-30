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
import static grondag.tdnf.Configurator.keepLogsIntact;
import static grondag.tdnf.Configurator.maxBreaksPerTick;
import static grondag.tdnf.Configurator.maxSearchPosPerTick;
import static grondag.tdnf.Configurator.renderFallingLogs;
import static grondag.tdnf.Configurator.fallingLogsBreakPlants;
import static grondag.tdnf.Configurator.fallingLogsBreakFragile;
import static grondag.tdnf.Configurator.protectPlayerLogs;
//import static grondag.tdnf.Configurator.logSupportSurface;
import static grondag.tdnf.Configurator.requireLogBreak;
import static grondag.tdnf.Configurator.stackDrops;

import java.util.Optional;

import grondag.tdnf.Configurator;
import grondag.tdnf.Configurator.EffectLevel;
//import grondag.tdnf.Configurator.SupportSurface;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.EnumListEntry;
import me.shedaniel.clothconfig2.gui.entries.IntegerSliderEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resource.language.I18n;

@Environment(EnvType.CLIENT)
public class ConfigScreen {
    @SuppressWarnings({ "unchecked", "rawtypes" })
    static Screen getScreen(Screen parent) {
        
        ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent)
                .setTitle("config.tdnf.title").setSavingRunnable(ConfigScreen::saveUserInput);
        
        // FEATURES
        ConfigCategory features = builder.getOrCreateCategory("config.tdnf.category.features");
        
        features.addEntry(new BooleanListEntry("config.tdnf.value.keep_logs_intact", keepLogsIntact, "config.tdnf.reset", 
                () -> DEFAULTS.keepLogsIntact, b -> keepLogsIntact = b, 
                () -> Optional.of(I18n.translate("config.tdnf.help.keep_logs_intact").split(";"))));
        
        features.addEntry(new BooleanListEntry("config.tdnf.value.render_falling", renderFallingLogs, "config.tdnf.reset", 
                () -> DEFAULTS.renderFallingLogs, b -> renderFallingLogs = b, 
                () -> Optional.of(I18n.translate("config.tdnf.help.render_falling").split(";"))));
        
        features.addEntry(new BooleanListEntry("config.tdnf.value.break_leaves", fallingLogsBreakPlants, "config.tdnf.reset", 
                () -> DEFAULTS.fallingLogsBreakPlants, b -> fallingLogsBreakPlants = b, 
                () -> Optional.of(I18n.translate("config.tdnf.help.break_leaves").split(";"))));
        
        features.addEntry(new BooleanListEntry("config.tdnf.value.break_fragile", fallingLogsBreakFragile, "config.tdnf.reset", 
                () -> DEFAULTS.fallingLogsBreakFragile, b -> fallingLogsBreakFragile = b, 
                () -> Optional.of(I18n.translate("config.tdnf.help.break_fragile").split(";"))));
        
        features.addEntry(new BooleanListEntry("config.tdnf.value.require_log_break", requireLogBreak, "config.tdnf.reset", 
                () -> DEFAULTS.requireLogBreak, b -> requireLogBreak = b, 
                () -> Optional.of(I18n.translate("config.tdnf.help.require_log_break").split(";"))));
        
        features.addEntry(new BooleanListEntry("config.tdnf.value.protect_player_logs", protectPlayerLogs, "config.tdnf.reset", 
                () -> DEFAULTS.protectPlayerLogs, b -> protectPlayerLogs = b, 
                () -> Optional.of(I18n.translate("config.tdnf.help.protect_player_logs").split(";"))));
        
//        features.addEntry(new EnumListEntry(
//                "config.tdnf.value.support_surface", 
//                SupportSurface.class, 
//                logSupportSurface, 
//                "config.tdnf.reset", 
//                () -> DEFAULTS.minimumSupportSurface, 
//                (b) -> logSupportSurface = (SupportSurface) b,
//                a -> a.toString(),
//                () -> Optional.of(I18n.translate("config.tdnf.help.support_surface").split(";"))));
        
        // PERFORMANCE
        ConfigCategory performance = builder.getOrCreateCategory("config.tdnf.category.performance");
        
        performance.addEntry(new BooleanListEntry("config.tdnf.value.consolidate_drops", stackDrops, "config.tdnf.reset", 
                () -> DEFAULTS.stackDrops, b -> stackDrops = b, 
                () -> Optional.of(I18n.translate("config.tdnf.help.consolidate_drops").split(";"))));
        
        performance.addEntry(new EnumListEntry(
                "config.tdnf.value.effect_level", 
                EffectLevel.class, 
                effectLevel, 
                "config.tdnf.reset", 
                () -> DEFAULTS.effectLevel, 
                (b) -> effectLevel = (EffectLevel) b,
                a -> a.toString(),
                () -> Optional.of(I18n.translate("config.tdnf.help.effect_level").split(";"))));
        
        performance.addEntry(new IntegerSliderEntry("config.tdnf.value.max_breaks_per_tick", 1, 128, maxBreaksPerTick, "config.tdnf.reset", 
                () -> DEFAULTS.maxBreaksPerTick, b -> maxBreaksPerTick = b, 
                () -> Optional.of(I18n.translate("config.tdnf.help.max_breaks_per_tick").split(";"))));
        
        performance.addEntry(new IntegerSliderEntry("config.tdnf.value.break_cooldown_ticks", 0, 40, breakCooldownTicks, "config.tdnf.reset", 
                () -> DEFAULTS.breakCooldownTicks, b -> breakCooldownTicks = b, 
                () -> Optional.of(I18n.translate("config.tdnf.help.break_cooldown_ticks").split(";"))));
        
        performance.addEntry(new IntegerSliderEntry("config.tdnf.value.max_search_pos_per_tick", 1, 512, maxSearchPosPerTick, "config.tdnf.reset", 
                () -> DEFAULTS.maxSearchPosPerTick, b -> maxSearchPosPerTick = b, 
                () -> Optional.of(I18n.translate("config.tdnf.help.max_search_pos_per_tick").split(";"))));
        
        builder.setDoesConfirmSave(false);
        
        return builder.build();
    }
    
    private static void saveUserInput() {
        Configurator.computeDerived();
        Configurator.saveConfig();
    }
}
