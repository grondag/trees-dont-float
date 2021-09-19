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

import java.util.IdentityHashMap;
import java.util.function.Predicate;

import com.google.common.base.Predicates;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class Dispatcher {

	private static boolean suspended = false;

	private static Predicate<BlockPos> doomTest = Predicates.alwaysFalse();

	public static void init() {
		ServerTickEvents.END_WORLD_TICK.register(Dispatcher::routeTick);
	}

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
