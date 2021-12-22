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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

import grondag.tdnf.Platform;
import grondag.tdnf.PlayerBreakHandler;
import grondag.tdnf.config.Configurator;
import grondag.tdnf.config.Configurator.FallCondition;
import grondag.tdnf.world.Dispatcher;
import grondag.tdnf.world.TreeBlock;

@Mixin(BlockBehaviour.class)
public abstract class MixinBlockBehaviour implements TreeBlock {
	private int blockType = UNKNOWN;

	@Shadow protected Material material;

	@Override
	public int treeBlockType() {
		int result = blockType;

		if (result == UNKNOWN) {
			final Block self = (Block) (Object) this;
			final String myName = Platform.getBlockName(self);

			if (BlockTags.LOGS.contains(self) && (material == Material.WOOD || material == Material.NETHER_WOOD)) {
				if (self == Blocks.CRIMSON_STEM || self == Blocks.WARPED_STEM || Configurator.moddedFungusLogs.contains(myName)) {
					result = FUNGUS_LOG;
				} else {
					result = LOG;
				}
			} else if (HugeMushroomBlock.class.isInstance(self) || Configurator.moddedMushroomBlocks.contains(myName)) {
				result = FUNGUS_LOG;
			} else if (self == Blocks.NETHER_WART_BLOCK || self == Blocks.WARPED_WART_BLOCK || self == Blocks.SHROOMLIGHT
					|| Configurator.moddedFungusLeaves.contains(myName)) {
				result = FUNGUS_LEAF;
			} else {
				result = OTHER;
			}

			blockType = result;
		}

		return result;
	}

	@Inject(at = @At("HEAD"), method = "neighborChanged")
	private void hookNeighborChanged(BlockState blockState, Level level, BlockPos blockPos, Block otherBlock, BlockPos otherPos, boolean notify, CallbackInfo ci) {
		if (!level.isClientSide
			&& PlayerBreakHandler.shouldCheckBreakEvents()
			&& isLog() && Configurator.fallCondition == FallCondition.NO_SUPPORT
			&& otherPos.getY() == blockPos.getY() - 1
		) {
			//			System.out.println("neighborUpdate notify = " + notify);
			final BlockState otherState = level.getBlockState(otherPos);

			if (!Block.isFaceFull(otherState.getCollisionShape(level, otherPos, CollisionContext.empty()), Direction.UP)) {
				Dispatcher.enqueBreak((ServerLevel) level, otherPos, null);
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
			Dispatcher.enqueBreak((ServerLevel) level, blockPos, null);
		}
	}
}
