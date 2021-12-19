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

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import grondag.tdnf.Configurator;

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
		if (queuedPositions.add(packedPosition)) {
			//                System.out.println("Enqueing: " + BlockPos.fromLong(packedPosition).toString() + " player = " +
			//                        (player == null ? "NULL" : player.toString()));
			waitingJobs.enqueue(TreeJob.claim(packedPosition, player, player == null ? null : player.getMainHandItem()));
		}
	}
}
