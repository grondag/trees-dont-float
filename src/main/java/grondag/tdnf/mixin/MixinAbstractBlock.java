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

import grondag.tdnf.Configurator;
import grondag.tdnf.Configurator.FallCondition;
import grondag.tdnf.PlayerBreakHandler;
import grondag.tdnf.world.Dispatcher;
import grondag.tdnf.world.TreeBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.block.MushroomBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

@Mixin(AbstractBlock.class)
public abstract class MixinAbstractBlock implements TreeBlock {
	private int blockType = UNKNOWN;

	@Shadow protected Material material;

	@Override
	public int treeBlockType() {
		int result = blockType;

		if(result == UNKNOWN) {
			final Block self = (Block)(Object) this;

			if(BlockTags.LOGS.contains(self) && (material == Material.WOOD || material == Material.NETHER_WOOD)) {
				if (self == Blocks.CRIMSON_STEM || self == Blocks.WARPED_STEM || Configurator.moddedFungusLogs.contains(Registry.BLOCK.getId(self).toString())) {
					result = FUNGUS_LOG;
				} else {
					result = LOG;
				}
			} else if (MushroomBlock.class.isInstance(this) || Configurator.moddedMushroomBlocks.contains(Registry.BLOCK.getId(self).toString())) {
				result = FUNGUS_LOG;
			} else if (self == Blocks.NETHER_WART_BLOCK || self == Blocks.WARPED_WART_BLOCK || self == Blocks.SHROOMLIGHT
			|| Configurator.moddedFungusLeaves.contains(Registry.BLOCK.getId(self).toString())) {
				result = FUNGUS_LEAF;
			} else {
				result = OTHER;
			}

			blockType = result;
		}

		return result;
	}

	@Inject(at = @At("HEAD"), method = "neighborUpdate")
	private void hookNeighborUpdate(BlockState blockState, World world, BlockPos blockPos, Block otherBlock, BlockPos otherPos, boolean notify,  CallbackInfo ci) {
		if (!world.isClient
		&& PlayerBreakHandler.shouldCheckBreakEvents()
		&& isLog() && Configurator.fallCondition == FallCondition.NO_SUPPORT
		&& otherPos.getY() == blockPos.getY() - 1) {
			//			System.out.println("neighborUpdate notify = " + notify);
			final BlockState otherState = world.getBlockState(otherPos);

			if (!Block.isFaceFullSquare(otherState.getCollisionShape(world, otherPos, ShapeContext.absent()), Direction.UP)) {
				Dispatcher.enqueCheck((ServerWorld) world, otherPos, null);
			}
		}
	}

	@Inject(at = @At("HEAD"), method = "onStateReplaced")
	private void hookOnStateReplaced(BlockState oldState, World world, BlockPos blockPos, BlockState newState, boolean notify, CallbackInfo ci) {
		if (isLog() && oldState.getBlock() != newState.getBlock()
		&& !world.isClient
		&& PlayerBreakHandler.shouldCheckBreakEvents()
		&& Configurator.fallCondition != FallCondition.USE_TOOL
		&& !Block.isFaceFullSquare(newState.getCollisionShape(world, blockPos, ShapeContext.absent()), Direction.UP)) {
			//			System.out.println("onBlockRemoved notify = " + notify);
			Dispatcher.enqueCheck((ServerWorld) world, blockPos, null);
		}
	}
}
