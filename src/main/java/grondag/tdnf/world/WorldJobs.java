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
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

class WorldJobs {
	private final ObjectArrayFIFOQueue<TreeJob> waitingJobs = new ObjectArrayFIFOQueue<>();
	private final LongOpenHashSet queuedPositions = new LongOpenHashSet();
	private final ObjectArrayList<TreeJob> runningJobs = new ObjectArrayList<>();

	public void run(ServerLevel world) {
		final ObjectArrayList<TreeJob> jobs = runningJobs;
		final int jobLimit = Configurator.maxJobsPerWorld;

		while (jobs.size() < jobLimit && !waitingJobs.isEmpty()) {
			jobs.add(waitingJobs.dequeue());
		}

		final int limit = jobs.size();

		for (int j = 0; j < limit; ++j) {
			jobs.get(j).prepareForTick(world);
		}

		int i = 0;
		boolean didRun = false;

		while (true) {
			if (jobs.isEmpty()) { // || !TickTimeLimiter.canRun()) {
				break;
			}

			final TreeJob job = jobs.get(i);

			if (job.canRun()) {
				didRun = true;
				job.tick(world);

				if (job.isComplete()) {
					queuedPositions.remove(job.startPos());
					job.release();
					jobs.remove(i);
				} else {
					++i;
				}
			} else {
				// skip jobs that have hit limits
				++i;
			}

			// exit if all jobs have hit limits
			if (i >= jobs.size()) {
				if (didRun) {
					didRun = false;
					i = 0;
				} else {
					break;
				}
			}
		}

		assert queuedPositions.isEmpty() == (waitingJobs.isEmpty() && runningJobs.isEmpty());
	}

	// only add the first report - earlier reports are more reliable/valuable
	// in particular, break block comes first and includes player
	public void enqueue(long packedPosition, ServerPlayer player) {
		if(queuedPositions.add(packedPosition)) {
			//                System.out.println("Enqueing: " + BlockPos.fromLong(packedPosition).toString() + " player = " +
			//                        (player == null ? "NULL" : player.toString()));
			waitingJobs.enqueue(TreeJob.claim(packedPosition, player, player == null ? null : player.getMainHandItem()));
		}
	}
}