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

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import net.fabricmc.fabric.api.event.world.WorldTickCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class Dispatcher {

    private static boolean suspended = false;

    public static void init() {
        WorldTickCallback.EVENT.register(Dispatcher::routeTick);
    }

    private static class WorldJobs {
        private final ObjectArrayFIFOQueue<TreeJob> jobList = new ObjectArrayFIFOQueue<>();
        private final LongOpenHashSet queuedPositions = new LongOpenHashSet();
        
        private TreeCutter cutter = null;
        private TreeJob currentJob = null;

        TreeCutter cutter() {
            TreeCutter result = cutter;
            if (result == null) {
                result = new TreeCutter();
                cutter = result;
            }
            return result;
        }

        public void run(World world) {
            TreeJob result = currentJob;
            if (result == null & !jobList.isEmpty()) {
                result = jobList.dequeue();
                currentJob = result;
                cutter().reset(result);
            }

            if (result != null) {
                if (cutter.tick(world)) {
                    queuedPositions.remove(result.startPos());
                    result.release();
                    currentJob = null;
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

    private static final IdentityHashMap<World, WorldJobs> worldJobs = new IdentityHashMap<>();

    public static void routeTick(World world) {
        if (world.isClient) {
            return;
        }
        ;

        WorldJobs jobs = worldJobs.get(world);
        if (jobs == null) {
            return;
        }
        jobs.run(world);
    }

    public static void enqueCheck(World world, BlockPos pos, ServerPlayerEntity player) {
        if (world.isClient || suspended) {
            return;
        }

        WorldJobs jobs = worldJobs.get(world);
        if (jobs == null) {
            jobs = new WorldJobs();
            worldJobs.put(world, jobs);
        }

        jobs.enqueue(BlockPos.asLong(pos.getX(), pos.getY() + 1, pos.getZ()), player);
    }

    public static void suspend() {
        suspended = true;
    }

    public static void resume() {
        suspended = false;
    }
}