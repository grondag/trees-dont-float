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
package grondag.tdnf.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.AbstractBlock.AbstractBlockState;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;

import grondag.tdnf.world.Dispatcher;

@Mixin(AbstractBlockState.class)
public class MixinAbstractBlockState {

	@Inject(at = @At("HEAD"), method = "getStateForNeighborUpdate", cancellable = true)
	private void hookGetStateForNeighborUpdate(Direction face, BlockState otherState, WorldAccess world,
			BlockPos myPos, BlockPos otherPos, CallbackInfoReturnable<BlockState> ci) {
		if(!world.isClient()) {
			final BlockState me = (BlockState)(Object)this;
			if(!me.isAir() && Dispatcher.isDoomed(myPos)) {
				ci.setReturnValue((BlockState)(Object)this);
			}
		}
	}
}
