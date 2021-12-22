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

import net.minecraft.core.Registry;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.tool.attribute.v1.FabricToolTags;
import net.fabricmc.loader.api.FabricLoader;

public class Platform {
	public static boolean isAxe(ItemStack stack) {
		return FabricToolTags.AXES.contains(stack.getItem());
	}

	public static Path configDirectory() {
		return FabricLoader.getInstance().getConfigDir();
	}

	public static String getBlockName(Block block) {
		return Registry.BLOCK.getKey(block).toString();
	}

	private static final EntityType<FallingLogEntity> FALLING_LOG;

	static {
		final var type = FabricEntityTypeBuilder.<FallingLogEntity>create(MobCategory.MISC, FallingLogEntity::new).dimensions(EntityDimensions.fixed(0.9f, 0.9f)).build();
		FALLING_LOG = Registry.register(Registry.ENTITY_TYPE, FallingLogEntity.IDENTIFIER, type);
	}

	public static EntityType<FallingLogEntity> fallingLogEntityType() {
		return FALLING_LOG;
	}
}
