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
import net.fabricmc.fabric.api.tools.FabricToolTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LogBlock;
import net.minecraft.block.MaterialColor;
import net.minecraft.block.PillarBlock;
import net.minecraft.entity.EntityContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateFactory;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

@Mixin(LogBlock.class)
public abstract class MixinLogBlock extends PillarBlock {
    public MixinLogBlock(MaterialColor materialColor_1, Block.Settings block$Settings_1) {
        super(block$Settings_1);
    }

    @Override
    public void neighborUpdate(BlockState blockState, World world, BlockPos blockPos, Block otherBlock, BlockPos otherPos, boolean notify) {
        super.neighborUpdate(blockState, world, blockPos, otherBlock, otherPos, notify);

        if ((Configurator.fallCondition == FallCondition.NO_SUPPORT) && otherPos.getY() == blockPos.getY() - 1 && world instanceof ServerWorld) {
//            System.out.println("neighborUpdate notify = " + notify);
            BlockState otherState = world.getBlockState(otherPos);
            if (!Block.isFaceFullSquare(otherState.getCollisionShape(world, otherPos, EntityContext.absent()), Direction.UP)) {
                Dispatcher.enqueCheck((World) world, otherPos, null);
            }
        }
    }

    @Override
    public void onBreak(World world, BlockPos blockPos, BlockState blockState, PlayerEntity playerEntity) {
        if (!world.isClient && playerEntity != null) {
            final ServerPlayerEntity player = (ServerPlayerEntity) playerEntity;
            if (Configurator.fallCondition == FallCondition.USE_TOOL) {
                final ItemStack stack = player.getMainHandStack();
                if (FabricToolTags.AXES.contains(stack.getItem())) {
                    Dispatcher.enqueCheck(world, blockPos, player);
                }
            } else {
//            System.out.println("onBreak playerEntity = " + (playerEntity == null ? "NULL" : playerEntity.toString()));
                Dispatcher.enqueCheck(world, blockPos, player);
            }
        }
        super.onBreak(world, blockPos, blockState, playerEntity);
    }

    @Override
    public void onBlockRemoved(BlockState oldState, World world, BlockPos blockPos, BlockState newState, boolean notify) {
        if (oldState.getBlock() != newState.getBlock() && Configurator.fallCondition != FallCondition.USE_TOOL) {
//            System.out.println("onBlockRemoved notify = " + notify);
            Dispatcher.enqueCheck(world, blockPos, null);
        }
        super.onBlockRemoved(oldState, world, blockPos, newState, notify);
    }

    @Inject(at = @At("RETURN"), method = "<init>")
    private void onInit(CallbackInfo ci) {
        this.setDefaultState((BlockState) this.getDefaultState().with(Properties.PERSISTENT, false));
    }

    @Override
    protected void appendProperties(StateFactory.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(Properties.PERSISTENT);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        BlockState result = super.getPlacementState(context);
        if (context.getPlayer() != null && result.getBlock() == (Block) (Object) (this)) {
            result = result.with(Properties.PERSISTENT, true);
        }
        return result;
    }
}
