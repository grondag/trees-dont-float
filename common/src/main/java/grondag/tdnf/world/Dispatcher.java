/*
 * This file is part of True Darkness and is licensed to the project under
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

import java.util.IdentityHashMap;
import java.util.function.Predicate;

import com.google.common.base.Predicates;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class Dispatcher {
	private static boolean suspended = false;

	private static Predicate<BlockPos> doomTest = Predicates.alwaysFalse();

	private static final IdentityHashMap<ServerLevel, WorldJobs> worldJobs = new IdentityHashMap<>();

	public static void routeTick(ServerLevel world) {
		if (world.isClientSide) {
			return;
		}

		final WorldJobs jobs = worldJobs.get(world);

		if (jobs != null) {
			TickTimeLimiter.reset();
			jobs.run(world);
		}

		// make sure we aren't left in an odd state
		resume();
	}

	public static void enqueCheck(ServerLevel world, BlockPos pos, ServerPlayer player) {
		if (world.isClientSide || suspended) {
			return;
		}

		WorldJobs jobs = worldJobs.get(world);

		if (jobs == null) {
			jobs = new WorldJobs();
			worldJobs.put(world, jobs);
		}

		jobs.enqueue(BlockPos.asLong(pos.getX(), pos.getY() + 1, pos.getZ()), player);
	}

	public static void suspend(Predicate<BlockPos> theDoomed) {
		doomTest = theDoomed;
		suspended = true;
	}

	public static void resume() {
		doomTest = Predicates.alwaysFalse();
		suspended = false;
	}

	public static boolean isDoomed(BlockPos pos) {
		return doomTest.test(pos);
	}
}
