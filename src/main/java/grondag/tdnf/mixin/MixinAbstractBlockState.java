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
import grondag.tdnf.world.Dispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(BlockStateBase.class)
public class MixinAbstractBlockState {

	@Inject(at = @At("HEAD"), method = "getStateForNeighborUpdate", cancellable = true)
	private void hookGetStateForNeighborUpdate(Direction face, BlockState otherState, LevelAccessor world,
			BlockPos myPos, BlockPos otherPos, CallbackInfoReturnable<BlockState> ci) {
		if(!world.isClientSide()) {
			final BlockState me = (BlockState)(Object)this;
			if(!me.isAir() && Dispatcher.isDoomed(myPos)) {
				ci.setReturnValue((BlockState)(Object)this);
			}
		}
	}
}
