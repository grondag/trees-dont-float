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

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

class WorldJobs {
	private final ObjectArrayFIFOQueue<TreeJob> jobList = new ObjectArrayFIFOQueue<>();
	private final LongOpenHashSet queuedPositions = new LongOpenHashSet();
	private TreeJob currentJob = null;

	public void run(ServerWorld world) {
		TreeJob result = currentJob;

		if (result == null & !jobList.isEmpty()) {
			result = jobList.dequeue();
			currentJob = result;
		}

		if (result != null) {
			if (result.tick(world)) {
				if (result.isTimedOut()) {
					queuedPositions.remove(result.startPos());
					result.release();
					currentJob = null;
				} else {
					// give job one more tick to clean up
					result.timeout();
				}
			}
		}

		assert queuedPositions.isEmpty() == (jobList.isEmpty() && currentJob == null);
	}

	// only add the first report - earlier reports are more reliable/valuable
	// in particular, break block comes first and includes player
	public void enqueue(long packedPosition, ServerPlayerEntity player) {
		if(queuedPositions.add(packedPosition)) {
			//                System.out.println("Enqueing: " + BlockPos.fromLong(packedPosition).toString() + " player = " +
			//                        (player == null ? "NULL" : player.toString()));
			jobList.enqueue(TreeJob.claim(packedPosition, player, player == null ? null : player.getMainHandStack()));
		}
	}
}