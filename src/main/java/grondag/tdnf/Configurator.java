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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import grondag.fermion.shadow.jankson.Comment;
import grondag.fermion.shadow.jankson.Jankson;
import grondag.fermion.shadow.jankson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

public class Configurator {
    public static enum EffectLevel {
        SOME,
        NONE,
        ALL
    }
    
    @SuppressWarnings("hiding")
    public static class ConfigData {
        @Comment("Log blocks move to the ground instead of dropping as items.")
        public boolean keepLogsIntact = true;
        
        @Comment("Log blocks that move are treat as falling block vs moving directly. Can be laggy.")
        public boolean fallingBlocks = false;
        
        @Comment("Log blocks that would suffocate an entity drop as items. (If logs stay intact.)")
        public boolean preventSuffocation = true;
        
        @Comment("Consolidate item drops into stacks to prevent lag.")
        public boolean stackDrops = true;
        
        @Comment("Play particles and sounds? Choises are SOME, NONE, and ALL.")
        public EffectLevel effectLevel = EffectLevel.SOME;
        
        @Comment("Max log/leaf blocks to break per tick. 1 - 128")
        public int maxBreaksPerTick = 32;
        
        @Comment("Ticks to wait between breaking blocks. 0 - 40")
        public int breakCooldownTicks = 0;
        
        @Comment("Max blocks checked per tick when searching for logs/leaves. 1 - 512")
        public int maxSearchPosPerTick = 128;
    }
    
    public static final ConfigData DEFAULTS = new ConfigData();
    private static final Gson GSON = new GsonBuilder().create();
    private static final Jankson JANKSON = Jankson.builder().build();
    
    public static boolean keepLogsIntact = DEFAULTS.keepLogsIntact;
    public static boolean fallingBlocks = DEFAULTS.fallingBlocks;
    public static boolean preventSuffocation = DEFAULTS.preventSuffocation;
    public static boolean stackDrops = DEFAULTS.stackDrops;
    public static EffectLevel effectLevel = DEFAULTS.effectLevel;
    public static int maxBreaksPerTick = DEFAULTS.maxBreaksPerTick;
    public static int breakCooldownTicks = DEFAULTS.breakCooldownTicks;
    public static int maxSearchPosPerTick = DEFAULTS.maxSearchPosPerTick;
    
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
        keepLogsIntact = config.keepLogsIntact;
        fallingBlocks = config.fallingBlocks;
        preventSuffocation = config.preventSuffocation;
        stackDrops = config.stackDrops;
        effectLevel = config.effectLevel;
        maxBreaksPerTick = config.maxBreaksPerTick;
        breakCooldownTicks = config.breakCooldownTicks;
        maxSearchPosPerTick = config.maxSearchPosPerTick;
    }

    public static void saveConfig() {
        ConfigData config = new ConfigData();
        config.keepLogsIntact = keepLogsIntact;
        config.fallingBlocks = fallingBlocks;
        config.preventSuffocation = preventSuffocation;
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
}
