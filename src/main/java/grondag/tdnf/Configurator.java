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

package grondag.tdnf;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Optional;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import grondag.fermion.shadow.jankson.Comment;
import grondag.fermion.shadow.jankson.Jankson;
import grondag.fermion.shadow.jankson.JsonObject;
import me.shedaniel.cloth.api.ConfigScreenBuilder;
import me.shedaniel.cloth.api.ConfigScreenBuilder.SavedConfig;
import me.shedaniel.cloth.gui.entries.BooleanListEntry;
import me.shedaniel.cloth.gui.entries.EnumListEntry;
import me.shedaniel.cloth.gui.entries.IntegerSliderEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resource.language.I18n;

@Environment(EnvType.CLIENT)
public class Configurator {
    public static enum EffectLevel {
        SOME,
        NONE,
        ALL
    }
    
    @SuppressWarnings("hiding")
    static class ConfigData {
        @Comment("Consolidate item drops into stacks to prevent lag.")
        boolean stackDrops = true;
        
        @Comment("Play particles and sounds? Choises are SOME, NONE, and ALL.")
        EffectLevel effectLevel = EffectLevel.SOME;
        
        @Comment("Max log/leaf blocks to break per tick. 1 - 128")
        int maxBreaksPerTick = 32;
        
        @Comment("Ticks to wait between breaking blocks. 0 - 40")
        int breakCooldownTicks = 0;
        
        @Comment("Max blocks checked per tick when searching for logs/leaves. 1 - 512")
        int maxSearchPosPerTick = 128;
    }
    
    static final ConfigData DEFAULTS = new ConfigData();
    private static final Gson GSON = new GsonBuilder().create();
    private static final Jankson JANKSON = Jankson.builder().build();
    
    public static boolean stackDrops = DEFAULTS.stackDrops;
    public static EffectLevel effectLevel = DEFAULTS.effectLevel;
    public static int maxBreaksPerTick = DEFAULTS.maxBreaksPerTick;
    public static int breakCooldownTicks = DEFAULTS.breakCooldownTicks;
    public static int maxSearchPosPerTick = DEFAULTS.maxSearchPosPerTick;
    
    /** use to stash parent screen during display */
    private static Screen screenIn;
    
    private static File configFile;
    
    public static void init() {
        configFile = new File(FabricLoader.getInstance().getConfigDirectory(), "trees-do-not-float.json5");
        if(configFile.exists()) {
            loadConfig();
        } else {
            saveConfig();
        }
    }
    
    private static void loadConfig() {
        ConfigData config = new ConfigData();
        try {
            JsonObject configJson = JANKSON.load(configFile);
            String regularized = configJson.toJson(false, false, 0);
            config = GSON.fromJson(regularized, ConfigData.class);
        } catch (Exception e) {
            e.printStackTrace();
            TreesDoNotFloat.LOG.error("Unable to load config. Using default values.");
        }
        stackDrops = config.stackDrops;
        effectLevel = config.effectLevel;
        maxBreaksPerTick = config.maxBreaksPerTick;
        breakCooldownTicks = config.breakCooldownTicks;
        maxSearchPosPerTick = config.maxSearchPosPerTick;
        
    }

    private static void saveConfig() {
        ConfigData config = new ConfigData();
        config.stackDrops = stackDrops;
        config.effectLevel = effectLevel;
        config.maxBreaksPerTick = maxBreaksPerTick;
        config.breakCooldownTicks = breakCooldownTicks;
        config.maxSearchPosPerTick = maxSearchPosPerTick;
        
        try {
            String result = JANKSON.toJson(config).toJson(true, true, 0);
            if (!configFile.exists())
                configFile.createNewFile();
            
            try(
                    FileOutputStream out = new FileOutputStream(configFile, false);
            ) {
                out.write(result.getBytes());
                out.flush();
                out.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            TreesDoNotFloat.LOG.error("Unable to save config.");
            return;
        }
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Screen display() {
        
        ConfigScreenBuilder builder = ConfigScreenBuilder.create(screenIn, "config.tdnf.title", Configurator::saveUserInput);
        
        // FEATURES
        ConfigScreenBuilder.CategoryBuilder features = builder.addCategory("config.tdnf.category.features");
        
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
    
    public static Optional<Supplier<Screen>> getConfigScreen(Screen screen) {
        screenIn = screen;
        return Optional.of(Configurator::display);
    }
    
    public static Screen getRawConfigScreen(Screen screen) {
        screenIn = screen;
        return display();
    }
    
    private static void saveUserInput(SavedConfig config) {
        saveConfig();
    }
}
