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

import java.util.concurrent.ArrayBlockingQueue;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import grondag.tdnf.config.Configurator;
import grondag.tdnf.config.Configurator.FallCondition;

public class TreeJob {
	private TreeJob() { }

	final TreeCutter cutter = new TreeCutter(this);
	private long startPos;
	private ServerPlayer player;
	private ItemStack stack;
	private boolean hasAxe;
	private boolean canCancel = true;
	private int ticks = 0;

	private void reset(ProtectionTracker protectionTracker) {
		ticks = 0;
		cutter.reset(protectionTracker);
	}

	public void prepareForTick(ServerLevel world) {
		cutter.prepareForTick(world);
		++ticks;
	}

	public boolean canRun() {
		return cutter.canRun();
	}

	public void tick(ServerLevel world) {
		cutter.tick(world);
	}

	/** packed staring pos. */
	public long startPos() {
		return startPos;
	}

	/** player who initiated the break, if known. */
	public ServerPlayer player() {
		return player;
	}

	/** stack used by player who initiated the break, if known. */
	public ItemStack stack() {
		return stack;
	}

	public boolean hasAxe() {
		return hasAxe && player.getMainHandItem() == stack && !stack.isEmpty();
	}

	public boolean isComplete() {
		return cutter.isComplete();
	}

	public boolean isTimedOut() {
		return ticks > Configurator.jobTimeoutTicks;
	}

	/** Call when when changing tool or player status can no longer affect the outcome. */
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
	public boolean isCancelled(Level world) {
		return canCancel
			& (player == null
			|| player.getMainHandItem() != stack
			|| player.wonGame
			|| player.level != world
			|| !closeEnough());
	}

	public final boolean closeEnough() {
		if (player == null) {
			return false;
		}

		final BlockPos pos = player.blockPosition();
		final int dx = pos.getX() - BlockPos.getX(startPos);
		final int dy = pos.getY() - BlockPos.getY(startPos);
		final int dz = pos.getZ() - BlockPos.getZ(startPos);
		return dx * dx + dy * dy + dz * dz < 32 * 32;
	}

	private static final ArrayBlockingQueue<TreeJob> POOL = new ArrayBlockingQueue<>(512);

	public static TreeJob claim(long startPos, ServerPlayer player, ItemStack stack, ProtectionTracker protectionTracker) {
		TreeJob result = POOL.poll();

		if (result == null) {
			result = new TreeJob();
		}

		result.startPos = startPos;
		result.player = player;
		result.stack = stack;
		// TODO: could be better/more reliable - on forge this tests that the tool can mine an Acacia log
		// Overall the assumptions here are sloppy for modded - may need to rethink how tools work
		result.hasAxe = DropHandler.hasAxe(player, stack);
		result.canCancel = result.hasAxe && Configurator.fallCondition == FallCondition.USE_TOOL;
		result.reset(protectionTracker);
		return result;
	}
}
