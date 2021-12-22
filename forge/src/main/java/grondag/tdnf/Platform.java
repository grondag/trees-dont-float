/*
 * This file is part of True Darkness and is licensed to the project under
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

package grondag.tdnf;

import java.nio.file.Path;

import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

@Mod.EventBusSubscriber(modid = TreesDoNotFloat.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Platform {
	public static Path configDirectory() {
		return FMLPaths.CONFIGDIR.get();
	}

	public static boolean isAxe(ItemStack stack) {
		return stack.getItem().isCorrectToolForDrops(Blocks.ACACIA_LOG.defaultBlockState());
	}

	public static String getBlockName(Block block) {
		return block.getRegistryName().toString();
	}

	private static EntityType<FallingLogEntity> FALLING_LOG;

	public static EntityType<FallingLogEntity> fallingLogEntityType() {
		return FALLING_LOG;
	}

	@SubscribeEvent
	public static void registerTE(RegistryEvent.Register<EntityType<?>> evt) {
		FALLING_LOG = EntityType.Builder.<FallingLogEntity>of(FallingLogEntity::new, MobCategory.MISC).sized(0.9f, 0.9f).build("tdnf_falling_log");
		FALLING_LOG.setRegistryName(FallingLogEntity.IDENTIFIER);
		evt.getRegistry().register(FALLING_LOG);
	}
}
