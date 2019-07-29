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

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PillarBlock;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.property.Properties;

@Mixin(PillarBlock.class)
public abstract class MixinPillarBlock extends MixinBlock {

    @Inject(at = @At("RETURN"), method = "getPlacementState")
    void getPlacementState(ItemPlacementContext context, CallbackInfoReturnable<BlockState> ci) {
        if (isLog()) {
            Block me = (Block)(Object)this;
            BlockState state = ci.getReturnValue();
            if (context.getPlayer() != null && state.getBlock() == me && me.getStateFactory().getProperties().contains(Properties.PERSISTENT)) {
                ci.setReturnValue(state.with(Properties.PERSISTENT, true));
            }
        }
    }
}
