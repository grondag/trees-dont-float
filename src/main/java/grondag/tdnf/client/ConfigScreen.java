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

import static grondag.tdnf.Configurator.*;

import static grondag.tdnf.Configurator.stackDrops;

import java.util.Optional;

import grondag.tdnf.Configurator;
import grondag.tdnf.Configurator.FallCondition;
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
    static Screen getScreen(Screen parent) {

        ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent).setTitle("config.tdnf.title").setSavingRunnable(ConfigScreen::saveUserInput);

        // BLOCKS
        ConfigCategory blocks = builder.getOrCreateCategory("config.tdnf.category.blocks");

        blocks.addEntry(new BooleanListEntry("config.tdnf.value.keep_logs_intact", keepLogsIntact, "config.tdnf.reset", () -> DEFAULTS.keepLogsIntact,
                b -> keepLogsIntact = b, () -> Optional.of(I18n.translate("config.tdnf.help.keep_logs_intact").split(";"))));

        blocks.addEntry(new BooleanListEntry("config.tdnf.value.render_falling", renderFallingLogs, "config.tdnf.reset", () -> DEFAULTS.renderFallingLogs,
                b -> renderFallingLogs = b, () -> Optional.of(I18n.translate("config.tdnf.help.render_falling").split(";"))));

        blocks.addEntry(
                new BooleanListEntry("config.tdnf.value.break_leaves", fallingLogsBreakPlants, "config.tdnf.reset", () -> DEFAULTS.fallingLogsBreakPlants,
                        b -> fallingLogsBreakPlants = b, () -> Optional.of(I18n.translate("config.tdnf.help.break_leaves").split(";"))));

        blocks.addEntry(
                new BooleanListEntry("config.tdnf.value.break_fragile", fallingLogsBreakFragile, "config.tdnf.reset", () -> DEFAULTS.fallingLogsBreakFragile,
                        b -> fallingLogsBreakFragile = b, () -> Optional.of(I18n.translate("config.tdnf.help.break_fragile").split(";"))));

        blocks.addEntry(
                new BooleanListEntry("config.tdnf.value.protect_player_logs", protectPlayerLogs, "config.tdnf.reset", () -> DEFAULTS.protectPlayerLogs,
                        b -> protectPlayerLogs = b, () -> Optional.of(I18n.translate("config.tdnf.help.protect_player_logs").split(";"))));

        
        // PLAYERS
        ConfigCategory players = builder.getOrCreateCategory("config.tdnf.category.players");
        
        players.addEntry(new EnumListEntry<>(
                "config.tdnf.value.fall_condition",
                FallCondition.class, 
                fallCondition, 
                "config.tdnf.reset", 
                () -> DEFAULTS.fallCondition, 
                (b) -> fallCondition = (FallCondition) b,
                a -> a.toString(),
                () -> Optional.of(I18n.translate("config.tdnf.help.fall_condition").split(";"))));

        players.addEntry(
                new BooleanListEntry("config.tdnf.value.direct_deposit", directDeposit, "config.tdnf.reset", () -> DEFAULTS.directDeposit,
                        b -> directDeposit = b, () -> Optional.of(I18n.translate("config.tdnf.help.direct_deposit").split(";"))));

        players.addEntry(new BooleanListEntry("config.tdnf.value.apply_fortune", applyFortune, "config.tdnf.reset", () -> DEFAULTS.applyFortune,
                b -> applyFortune = b, () -> Optional.of(I18n.translate("config.tdnf.help.apply_fortune").split(";"))));

        players.addEntry(new BooleanListEntry("config.tdnf.value.consume_durability", consumeDurability, "config.tdnf.reset", () -> DEFAULTS.consumeDurability,
                b -> consumeDurability = b, () -> Optional.of(I18n.translate("config.tdnf.help.consume_durability").split(";"))));

        players.addEntry(new BooleanListEntry("config.tdnf.value.protect_tools", protectTools, "config.tdnf.reset", () -> DEFAULTS.protectTools,
                b -> protectTools = b, () -> Optional.of(I18n.translate("config.tdnf.help.protect_tools").split(";"))));

        players.addEntry(new BooleanListEntry("config.tdnf.value.apply_hunger", applyHunger, "config.tdnf.reset", () -> DEFAULTS.applyHunger,
                b -> applyHunger = b, () -> Optional.of(I18n.translate("config.tdnf.help.apply_hunger").split(";"))));

        players.addEntry(new BooleanListEntry("config.tdnf.value.leaf_hunger", leafHunger, "config.tdnf.reset", () -> DEFAULTS.leafHunger,
                b -> leafHunger = b, () -> Optional.of(I18n.translate("config.tdnf.help.leaf_hunger").split(";"))));



        // PERFORMANCE
        ConfigCategory performance = builder.getOrCreateCategory("config.tdnf.category.performance");

        performance.addEntry(new BooleanListEntry("config.tdnf.value.consolidate_drops", stackDrops, "config.tdnf.reset", () -> DEFAULTS.stackDrops,
                b -> stackDrops = b, () -> Optional.of(I18n.translate("config.tdnf.help.consolidate_drops").split(";"))));

        performance.addEntry(
            new IntegerSliderEntry("config.tdnf.value.effect_level", 0, 60, effectsPerSecond, "config.tdnf.reset", () -> DEFAULTS.effectsPerSecond,
                    b -> effectsPerSecond = b, () -> Optional.of(I18n.translate("config.tdnf.help.effect_level").split(";"))));
        
        performance.addEntry(
                new IntegerSliderEntry("config.tdnf.value.max_breaks_per_tick", 1, 128, maxBreaksPerTick, "config.tdnf.reset", () -> DEFAULTS.maxBreaksPerTick,
                        b -> maxBreaksPerTick = b, () -> Optional.of(I18n.translate("config.tdnf.help.max_breaks_per_tick").split(";"))));

        performance.addEntry(new IntegerSliderEntry("config.tdnf.value.tick_budget", 1, 5, tickBudget, "config.tdnf.reset",
                () -> DEFAULTS.tickBudget, b -> tickBudget = b,
                () -> Optional.of(I18n.translate("config.tdnf.help.tick_budget").split(";"))));

        builder.setDoesConfirmSave(false);

        return builder.build();
    }

    private static void saveUserInput() {
        Configurator.computeDerived();
        Configurator.saveConfig();
    }
}
