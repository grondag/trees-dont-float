# Trees Do Not Float

# Annoyed by trees that don't respect the fundamental laws of physics? Put those trees in their place with **Trees Do Not Float!**

When this mod is installed and the last log supporting a tree is broken, the whole tree comes down. Designed to be server-friendly.

[https://streamable.com/cya03](https://streamable.com/cya03 "")

[https://streamable.com/svbis](https://streamable.com/svbis "")

[Discord](https://discord.gg/7NaqR2e)

## **NEWS**: Version 3.0

Version 3.0 restores the `protectPlayerLogs` config option.  This does not break world saves. Unlike the version 1.0 feature, this protection scheme does not change block states and the mod can still run server-side only. The mod only needs to be installed on the client if the "keep logs intact" feature is enabled. Players who are building with logs can temporarily disable the block breaking effect by sneaking (or not sneaking, configurable).

## Configuration

_Note that the Fabric version requires Mod Menu to access the in-game configuration UI._

### Configuration Presets
TDNF has many configuration options.  To make it easier to get what you want and start playing right away, the following presets are provided:

* **Deforestation**: (Default) Overpowered? Yes. Fun? Also yes. 
* **Physics**: Trees fall easily but logs are kept intact and may damage nearby objects.
* **Lumberjack**: Axe is required. Better axes and efficiency enchantment increase breaking range. No automated farms.
* **Progression**: Axes help and better axes help more. Automated farms have a modest range.
* **Hardcore**: Axes are required, logs remain intact, ranges are reduced, leaves cost durability and automation doesn't work.
* **Skyblock**: Items are directly deposited. Ranges are modest and axes help. Automated farms work.

For the Physics and Hardcore presets, you can enable the Render Falling Logs option to see the logs falling.  This only affects your game client. 

### Detailed Configuration Options

* **fallCondition**: When do trees break? (`NO_SUPPORT`, `LOG_BREAK`, or `USE_TOOL`)
* **fastLeafDecay**: Leaves decay instantly. Ignored (leaves decay) when keepLogsIntact is true.
* **keepLogsIntact**: Log blocks move to the ground instead of dropping as items. Can be laggy. Leaves alwasy break when true.
* **renderFallingLogs**: Render falling logs? (Affects client side only.) Can be laggy.
* **fallingLogsBreakPlants**: Falling logs break leaves and other plants on the way down.
* **fallingLogsBreakFragile**: Falling logs break glass and other fragile blocks.
* **activeWhen**: Players can sneak (or not sneak) to disable mod for building. (SNEAKING, NOT_SNEAKING, or ALWAYS)
* **breakFungalLeaves**: Break large fungal wart blocks and Shroomlamps
* **directDeposit**: Place dropped items directly into player inventory. (Good for skyblock)
* **applyFortune**: Apply fortune from axe used to fell a tree. (If an axe was used.)
* **consumeDurability**: Remove durability from an axe used to fell a tree. (If an axe was used.)
* **leafDurability**: Tools take durability loss from leaves as well as logs.
* **protectTools**: Don't break tools when using durability.
* **protectPlacedBlocks**: Don't break blocks placed by players.
* **applyHunger**: Players gain hunger from felling trees, as if they had broken each log.
* **leafHunger**: Players gain hunger from leaves in addition to logs.
* **nonPlayerLogLimit**: Max logs that can be broken by non-player mechanics like pistons, fire and TNT.
* **playerBaseLogLimit**: Logs that can be broken by a player without using a tool.
* **toolTierLogBonus**: Additional logs that can be broken when player uses a tool, per tier of tool. Set zero to disable.
* **enableEfficiencyLogMultiplier**: When true, log breaking limit is multiplied by Efficiency enchantment level.
* **stackDrops**: Consolidate item drops into stacks to prevent lag.
* **effectsPerSecond**: Play particles and sounds? Number is max effects per second. 0-20
* maxJobsPerWorld: Maximum number of concurrent breaking tasks in each world. 1-256
* maxBreaksPerSecond: Max log/leaf blocks to break per second, per tree. 1 - 2560
* tickBudget: Max percentage of each server tick that can be used by TDNF in each world. 1 - 5
* maxFallingBlocks: Max number of active falling block entities. 1 - 64
* jobTimeoutSeconds: Tree cutting jobs will be abandoned if they take longer than this number of seconds. Use larger values if breaking speed is slow. 20-1800

## Caution

If the mod is configured to combine item stacks and the server saves the world and exits before breaking is complete, you may not get all of your drops from a tree that is in the process of breaking. You can turn this feature off so that items always drop immediately, but that will generate more lag. Fortunately, wood is cheap.
 
