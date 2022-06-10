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

package grondag.tdnf.world;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import grondag.tdnf.Platform;
import grondag.tdnf.config.Configurator;

public class DropHandler {
	public static boolean hasAxe(Player player, ItemStack stack) {
		return player != null && stack != null && !stack.isEmpty() && Platform.isAxe(stack);
	}

	private TreeJob job = null;

	private final BlockPos.MutableBlockPos searchPos = new BlockPos.MutableBlockPos();

	/** holds consolidated drops. */
	private final ObjectArrayList<ItemStack> drops = new ObjectArrayList<>();

	private void doUnstackedDrops(ServerLevel world, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, @Nullable ServerPlayer player, @Nullable ItemStack stack) {
		if (Configurator.directDeposit && job.closeEnough()) {
			dropDirectDepositStacks(world, pos, state, blockEntity, player, stack);
		} else if (hasAxe(player, stack) && Configurator.applyFortune) {
			Block.dropResources(state, world, pos, blockEntity, player, stack);
		} else {
			Block.dropResources(state, world, pos, blockEntity);
		}
	}

	public void doDrops(BlockState blockState, ServerLevel world, BlockPos pos, BlockEntity blockEntity) {
		if (Configurator.stackDrops && !world.isClientSide) {
			if (Configurator.applyFortune && job.hasAxe()) {
				Block.getDrops(blockState, world, pos, blockEntity, job.player(), job.stack()).forEach(s -> consolidateDrops(world, s));
				// XP, etc. - probably not needed for logs but just in case
				blockState.spawnAfterBreak(world, pos, job.stack(), true);
			} else {
				Block.getDrops(blockState, world, pos, blockEntity).forEach(s -> consolidateDrops(world, s));
				// XP, etc. - probably not needed for logs but just in case
				blockState.spawnAfterBreak(world, pos, ItemStack.EMPTY, true);
			}
		} else {
			doUnstackedDrops(world, pos, blockState, blockEntity, job.player(), job.stack());
		}
	}

	public void spawnDrops(Level world) {
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
	 * Version of {@link Block#dropResources(BlockState, Level, BlockPos, BlockEntity, net.minecraft.world.entity.Entity, ItemStack)}
	 * that drops items directly to player inventory.
	 */
	private void dropDirectDepositStacks(ServerLevel world, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, @Nullable ServerPlayer player, @Nullable ItemStack stack) {
		if (hasAxe(player, stack) && Configurator.applyFortune) {
			Block.getDrops(state, world, pos, blockEntity, player, stack).forEach(s -> dropStack(world, pos, s, player));
			state.spawnAfterBreak(world, pos, stack, true);
		} else {
			Block.getDrops(state, world, pos, blockEntity).forEach(s -> dropStack(world, pos, s, player));
			state.spawnAfterBreak(world, pos, ItemStack.EMPTY, true);
		}
	}

	/**
	 * Version of {@link Block#popResource(Level, BlockPos, ItemStack)} that gives item to player directly if they have room
	 * or otherwise drops near their feet.
	 */
	private void dropStack(Level world, BlockPos pos, ItemStack stack, ServerPlayer player) {
		if (player == null || !(Configurator.directDeposit && job.closeEnough())) {
			Block.popResource(world, pos, stack);
		} else if (!world.isClientSide && !stack.isEmpty() && world.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
			if (!player.addItem(stack)) {
				final BlockPos playerPos = player.blockPosition();
				final ItemEntity itemEntity_1 = new ItemEntity(world, playerPos.getX(), playerPos.getY(), playerPos.getZ(), stack);
				itemEntity_1.setDefaultPickUpDelay();
				world.addFreshEntity(itemEntity_1);
			}
		}
	}

	private void consolidateDrops(Level world, ItemStack stack) {
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
				final int capacity = existing.getMaxStackSize() - existing.getCount();

				if (capacity > 0 && stack.getItem() == existing.getItem() && ItemStack.tagMatches(stack, existing)) {
					final int amt = Math.min(stack.getCount(), capacity);

					if (amt > 0) {
						stack.shrink(amt);
						existing.grow(amt);
					}

					if (existing.getCount() == existing.getMaxStackSize()) {
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
