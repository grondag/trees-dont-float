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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import grondag.tdnf.Configurator;
import grondag.tdnf.Configurator.FallCondition;
import grondag.tdnf.world.Dispatcher;
import grondag.tdnf.world.DropHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

@Mixin(Block.class)
public abstract class MixinBlock {
    private static final int LOG = 0;
    private static final int UNKNOWN = 1;
    private static final int OTHER = 2;
    
    int blockType = UNKNOWN;
    
    boolean isLog() {
        int result = blockType;
        if(result == UNKNOWN) {
            if(BlockTags.LOGS.contains((Block)(Object)this)) {
                result = LOG;
            } else {
                result = OTHER;
            }
            this.blockType = result;
        }
        return result == LOG;
    }
    
    @Inject(at = @At("HEAD"), method = "neighborUpdate")
    private void hookNeighborUpdate(BlockState blockState, World world, BlockPos blockPos, Block otherBlock, BlockPos otherPos, boolean notify,  CallbackInfo ci) {
        if (!world.isClient && isLog() && Configurator.fallCondition == FallCondition.NO_SUPPORT && otherPos.getY() == blockPos.getY() - 1) {
//            System.out.println("neighborUpdate notify = " + notify);
            BlockState otherState = world.getBlockState(otherPos);
            if (!Block.isFaceFullSquare(otherState.getCollisionShape(world, otherPos, EntityContext.absent()), Direction.UP)) {
                Dispatcher.enqueCheck((World) world, otherPos, null);
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "onBreak")
    private void hookOnBreak(World world, BlockPos blockPos, BlockState blockState, PlayerEntity playerEntity, CallbackInfo ci) {
        if (!world.isClient && isLog() && playerEntity != null) {
            final ServerPlayerEntity player = (ServerPlayerEntity) playerEntity;
            if (Configurator.fallCondition == FallCondition.USE_TOOL) {
                if(DropHandler.hasAxe(player, player.getMainHandStack())) {
                    Dispatcher.enqueCheck(world, blockPos, player);
                }
            } else {
//            System.out.println("onBreak playerEntity = " + (playerEntity == null ? "NULL" : playerEntity.toString()));
                Dispatcher.enqueCheck(world, blockPos, player);
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "onBlockRemoved")
    private void hookOnBlockRemoved(BlockState oldState, World world, BlockPos blockPos, BlockState newState, boolean notify, CallbackInfo ci) {
        if (isLog() && oldState.getBlock() != newState.getBlock() && Configurator.fallCondition != FallCondition.USE_TOOL) {
//            System.out.println("onBlockRemoved notify = " + notify);
            Dispatcher.enqueCheck(world, blockPos, null);
        }
    }
}
