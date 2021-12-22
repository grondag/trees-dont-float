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

import io.netty.buffer.Unpooled;
import net.minecraftforge.fmllegacy.common.registry.IEntityAdditionalSpawnData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

import grondag.tdnf.Platform;
import grondag.tdnf.TreesDoNotFloat;
import grondag.tdnf.config.Configurator;
import grondag.tdnf.world.BaseFallingLogEntity;

public class FallingLogEntity extends BaseFallingLogEntity implements IEntityAdditionalSpawnData {
	public FallingLogEntity(EntityType<? extends FallingLogEntity> entityType, Level world) {
		super(entityType, world);
	}

	public FallingLogEntity(Level world, double x, double y, double z, BlockState state) {
		super(world, x, y, z, state);
	}

	@Override
	public void writeSpawnData(FriendlyByteBuf arg) {
		toBuffer(arg);
	}

	@Override
	public void readSpawnData(FriendlyByteBuf arg) {
		fromBuffer(arg);
	}
}
