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

package grondag.tdnf.world;

import grondag.tdnf.Configurator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

import net.fabricmc.fabric.api.tool.attribute.v1.FabricToolTags;


public class DropHandler {
	public static boolean hasAxe(PlayerEntity player, ItemStack stack) {
		return player != null && stack != null && !stack.isEmpty() && FabricToolTags.AXES.contains(stack.getItem());
	}

	private TreeJob job = null;

	private final BlockPos.Mutable searchPos = new BlockPos.Mutable();

	/** holds consolidated drops */
	private final ObjectArrayList<ItemStack> drops = new ObjectArrayList<>();

	private void doUnstackedDrops(ServerWorld world, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, @Nullable ServerPlayerEntity player, @Nullable ItemStack stack) {
		if(Configurator.directDeposit && job.closeEnough()) {
			dropDirectDepositStacks(world, pos, state, blockEntity, player, stack);
		} else if (hasAxe(player, stack) && Configurator.applyFortune) {
			Block.dropStacks(state, world, pos, blockEntity, player, stack);
		} else {
			Block.dropStacks(state, world, pos, blockEntity);
		}
	}

	public void doDrops(BlockState blockState, ServerWorld world, BlockPos pos, BlockEntity blockEntity) {
		if (Configurator.stackDrops && !world.isClient) {
			if (Configurator.applyFortune && job.hasAxe()) {
				Block.getDroppedStacks(blockState, world, pos, blockEntity, job.player(), job.stack()).forEach(s -> consolidateDrops(world, s));
				// XP, etc. - probably not needed for logs but just in case
				blockState.onStacksDropped(world, pos, job.stack());
			} else {
				Block.getDroppedStacks(blockState, world, pos, blockEntity).forEach(s -> consolidateDrops(world, s));
				// XP, etc. - probably not needed for logs but just in case
				blockState.onStacksDropped(world, pos, ItemStack.EMPTY);
			}
		} else {
			doUnstackedDrops(world, pos, blockState, blockEntity, job.player(), job.stack());
		}
	}

	public void spawnDrops(World world) {
		if (!drops.isEmpty()) {
			final BlockPos pos = searchPos.set(job.startPos());
			final int limit = drops.size();

			for (int i = 0; i < limit; i++) {
				dropStack(world, pos, drops.get(i), job.player());
			}

			drops.clear();
		}
	}

	public void reset(TreeJob job) {
		this.job = job;
		drops.clear();
	}

	/**
	 * Version of {@link Block#dropStacks(BlockState, World, BlockPos, BlockEntity, net.minecraft.entity.Entity, ItemStack)}
	 * that drops items directly to player inventory.
	 */
	private void dropDirectDepositStacks(ServerWorld world, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, @Nullable ServerPlayerEntity player, @Nullable ItemStack stack) {
		if (hasAxe(player, stack) && Configurator.applyFortune) {
			Block.getDroppedStacks(state, world, pos, blockEntity, player, stack).forEach(s -> dropStack(world, pos, s, player));
			state.onStacksDropped(world, pos, stack);
		} else {
			Block.getDroppedStacks(state, world, pos, blockEntity).forEach(s -> dropStack(world, pos, s, player));
			state.onStacksDropped(world, pos, ItemStack.EMPTY);
		}
	}

	/**
	 * Version of {@link Block#dropStack(World, BlockPos, ItemStack)} that gives item to player directly if they have room
	 * or otherwise drops near their feet.
	 */
	private void dropStack(World world, BlockPos pos, ItemStack stack, ServerPlayerEntity player) {
		if(player == null || !(Configurator.directDeposit && job.closeEnough())) {
			Block.dropStack(world, pos, stack);
		} else if(!world.isClient && !stack.isEmpty() && world.getGameRules().getBoolean(GameRules.DO_TILE_DROPS)) {
			if(!player.giveItemStack(stack)) {
				final BlockPos playerPos = player.getBlockPos();
				final ItemEntity itemEntity_1 = new ItemEntity(world, playerPos.getX(), playerPos.getY(), playerPos.getZ(), stack);
				itemEntity_1.setToDefaultPickupDelay();
				world.spawnEntity(itemEntity_1);
			}
		}
	}

	private void consolidateDrops(World world, ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return;
		}
		stack = stack.copy();

		if (!stack.isStackable()) {
			dropStack(world, searchPos.set(job.startPos()), stack, job.player());
			return;
		}

		final ObjectArrayList<ItemStack> drops = this.drops;

		if (drops.isEmpty() || !stack.isStackable()) {
			drops.add(stack);
		} else {
			final int limit = drops.size();
			for (int i = 0; i < limit; i++) {
				final ItemStack existing = drops.get(i);
				final int capacity = existing.getMaxCount() - existing.getCount();
				if (capacity > 0 && stack.getItem() == existing.getItem() && ItemStack.areTagsEqual(stack, existing)) {
					final int amt = Math.min(stack.getCount(), capacity);
					if (amt > 0) {
						stack.decrement(amt);
						existing.increment(amt);
					}

					if (existing.getCount() == existing.getMaxCount()) {
						dropStack(world, searchPos.set(job.startPos()), existing, job.player());
						drops.remove(i);
						break;
					}

					if (stack.isEmpty()) {
						return;
					}
				}
			}
			if (!stack.isEmpty()) {
				drops.add(stack);
			}
		}
	}

	public final Operation opDoDrops = w -> {
		if (Configurator.stackDrops) {
			spawnDrops(w);
		}

		return Operation.COMPLETE;
	};
}
