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

import net.minecraftforge.fmllegacy.common.registry.IEntityAdditionalSpawnData;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

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
