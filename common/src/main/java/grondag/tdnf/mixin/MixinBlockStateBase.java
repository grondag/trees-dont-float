/*
 * This file is part of Trees Do Not Float and is licensed to the project under
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

package grondag.tdnf.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import net.minecraft.world.level.block.state.BlockState;

import grondag.tdnf.world.Dispatcher;

@Mixin(BlockStateBase.class)
public class MixinBlockStateBase {
	@Inject(at = @At("HEAD"), method = "updateShape", cancellable = true)
	private void hookUpdateShape(Direction face, BlockState otherState, LevelAccessor levelAccessor, BlockPos myPos, BlockPos otherPos, CallbackInfoReturnable<BlockState> ci) {
		if (!levelAccessor.isClientSide()) {
			final BlockState me = (BlockState) (Object) this;

			if (!me.isAir() && Dispatcher.isDoomed(myPos)) {
				ci.setReturnValue(me);
			}
		}
	}
}
