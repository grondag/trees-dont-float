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

import java.util.concurrent.ArrayBlockingQueue;

import grondag.tdnf.Configurator;
import grondag.tdnf.Configurator.FallCondition;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TreeJob {
	private TreeJob() {}
	final TreeCutter cutter = new TreeCutter(this);
	private long startPos;
	private ServerPlayerEntity player;
	private ItemStack stack;
	private boolean hasAxe;
	private boolean canCancel = true;
	private int ticks = 0;

	private void reset() {
		ticks = 0;
		cutter.reset();
	}

	public void prepareForTick(ServerWorld world) {
		cutter.prepareForTick(world);
		++ticks;
	}

	public boolean canRun() {
		return cutter.canRun();
	}

	public void tick(ServerWorld world) {
		cutter.tick(world);
	}

	/** packed staring pos */
	public long startPos() {
		return startPos;
	}

	/** player who initiated the break, if known */
	public ServerPlayerEntity player() {
		return player;
	}

	/** stack used by player who initiated the break, if known */
	public ItemStack stack() {
		return stack;
	}

	public boolean hasAxe() {
		return hasAxe && player.getMainHandStack() == stack && !stack.isEmpty();
	}

	public boolean isComplete() {
		return cutter.isComplete();
	}

	public boolean isTimedOut() {
		return ticks > Configurator.jobTimeoutTicks;
	}

	/** Call when when changing tool or player status can no longer affect the outcome */
	public void disableCancel() {
		canCancel = false;
	}

	public void release() {
		player = null;
		stack = null;
		POOL.offer(this);
	}

	/**
	 * True when: <br>
	 * <li>Player starts with an axe
	 * <li>Tool-dependent effects are in play
	 * <li>Player switches away from the tool they had at start.
	 */
	public boolean isCancelled(World world) {
		return canCancel
		& (player == null
		|| player.getMainHandStack() != stack
		|| player.notInAnyWorld
		|| player.world != world
		|| !closeEnough());
	}

	public final boolean closeEnough() {
		if (player == null) {
			return false;
		}

		final BlockPos pos = player.getBlockPos();
		final int dx = pos.getX() - BlockPos.unpackLongX(startPos);
		final int dy = pos.getY() - BlockPos.unpackLongY(startPos);
		final int dz = pos.getZ() - BlockPos.unpackLongZ(startPos);
		return dx * dx + dy * dy + dz * dz < 32 * 32;
	}

	private static final ArrayBlockingQueue<TreeJob> POOL = new ArrayBlockingQueue<>(512);

	public static TreeJob claim(long startPos, ServerPlayerEntity player, ItemStack stack) {
		TreeJob result = POOL.poll();
		if (result == null) {
			result = new TreeJob();
		}
		result.startPos = startPos;
		result.player = player;
		result.stack = stack;
		result.hasAxe = DropHandler.hasAxe(player, stack);
		result.canCancel = result.hasAxe && Configurator.fallCondition == FallCondition.USE_TOOL;
		result.reset();
		return result;
	}
}
