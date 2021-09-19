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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.shapes.CollisionContext;

import grondag.tdnf.Configurator;
import grondag.tdnf.Configurator.FallCondition;
import grondag.tdnf.PlayerBreakHandler;
import grondag.tdnf.world.Dispatcher;
import grondag.tdnf.world.TreeBlock;

@Mixin(BlockBehaviour.class)
public abstract class MixinBlockBehaviour implements TreeBlock {
	private int blockType = UNKNOWN;

	@Shadow protected Material material;

	@Override
	public int treeBlockType() {
		int result = blockType;

		if(result == UNKNOWN) {
			final Block self = (Block)(Object) this;

			if(BlockTags.LOGS.contains(self) && (material == Material.WOOD || material == Material.NETHER_WOOD)) {
				if (self == Blocks.CRIMSON_STEM || self == Blocks.WARPED_STEM || Configurator.moddedFungusLogs.contains(Registry.BLOCK.getKey(self).toString())) {
					result = FUNGUS_LOG;
				} else {
					result = LOG;
				}
			} else if (HugeMushroomBlock.class.isInstance(this) || Configurator.moddedMushroomBlocks.contains(Registry.BLOCK.getKey(self).toString())) {
				result = FUNGUS_LOG;
			} else if (self == Blocks.NETHER_WART_BLOCK || self == Blocks.WARPED_WART_BLOCK || self == Blocks.SHROOMLIGHT
			|| Configurator.moddedFungusLeaves.contains(Registry.BLOCK.getKey(self).toString())) {
				result = FUNGUS_LEAF;
			} else {
				result = OTHER;
			}

			blockType = result;
		}

		return result;
	}

	@Inject(at = @At("HEAD"), method = "neighborChanged")
	private void hookNeighborChanged(BlockState blockState, Level level, BlockPos blockPos, Block otherBlock, BlockPos otherPos, boolean notify,  CallbackInfo ci) {
		if (!level.isClientSide
			&& PlayerBreakHandler.shouldCheckBreakEvents()
			&& isLog() && Configurator.fallCondition == FallCondition.NO_SUPPORT
			&& otherPos.getY() == blockPos.getY() - 1
		) {
			//			System.out.println("neighborUpdate notify = " + notify);
			final BlockState otherState = level.getBlockState(otherPos);

			if (!Block.isFaceFull(otherState.getCollisionShape(level, otherPos, CollisionContext.empty()), Direction.UP)) {
				Dispatcher.enqueCheck((ServerLevel) level, otherPos, null);
			}
		}
	}

	@Inject(at = @At("HEAD"), method = "onRemove")
	private void hookOnRemove(BlockState oldState, Level level, BlockPos blockPos, BlockState newState, boolean notify, CallbackInfo ci) {
		if (isLog() && oldState.getBlock() != newState.getBlock()
			&& !level.isClientSide
			&& PlayerBreakHandler.shouldCheckBreakEvents()
			&& Configurator.fallCondition != FallCondition.USE_TOOL
			&& !Block.isFaceFull(newState.getCollisionShape(level, blockPos, CollisionContext.empty()), Direction.UP)
		) {
			//			System.out.println("onBlockRemoved notify = " + notify);
			Dispatcher.enqueCheck((ServerLevel) level, blockPos, null);
		}
	}
}
