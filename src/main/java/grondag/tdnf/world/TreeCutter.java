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

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Random;

import grondag.tdnf.Configurator;
import io.netty.util.internal.ThreadLocalRandom;
import it.unimi.dsi.fastutil.longs.Long2ByteMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LogBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EntityContext;
import net.minecraft.fluid.FluidState;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.world.World;

/**
 * Call when log neighbors change. Will check for a tree-like structure starting
 * at the point and if it is not resting on solid blocks will destroy the tree.<p>
 * 
 * Not thread-safe. Meant to be called from server thread. <p>
 */
public class TreeCutter {
    private Operation operation = Operation.COMPLETE;

    /** if search in progress, starting state of search */
    private BlockState startState = Blocks.AIR.getDefaultState();

    private Block startBlock;

    private final PriorityQueue<Visit> toVisit = new PriorityQueue<>(new Comparator<Visit>() {
        @Override
        public int compare(Visit o1, Visit o2) {
            return Byte.compare(o1.type, o2.type);
        }
    });

    /** packed positions that have received a valid visit */
    private final Long2ByteOpenHashMap visited = new Long2ByteOpenHashMap();

    /** packed positions of logs to be cleared - populated during pre-clearing */
    private final LongArrayFIFOQueue logs = new LongArrayFIFOQueue();

    /**
     * packed positions of logs to fall - sorted by Y - needed cuz can't sort FIFO
     * Queue
     */
    private final LongArrayList fallingLogs = new LongArrayList();

    /** Used to iterate {@link #fallingLogs} */
    private int fallingLogIndex = 0;

    /** leaves to be cleared - populated during pre-clearing */
    private final LongArrayFIFOQueue leaves = new LongArrayFIFOQueue();

    /** Used to determine which leaf block should be {@link #leafBlock} */
    private final Object2IntOpenHashMap<Block> leafCounts = new Object2IntOpenHashMap<>();

    /** general purpose mutable pos */
    private final BlockPos.Mutable searchPos = new BlockPos.Mutable();

    /** iterator traversed during pre-clearing */
    private ObjectIterator<Entry> prepIt = null;

    /** most common leaf block next to trunk */
    private Block leafBlock = null;

    private TreeJob job = null;
    
    private final DropHandler dropHandler = new DropHandler();
    
    private final FxManager fx = new FxManager();
    
    /** Counter for enforcing configured per-tick break max */
    private int breakBudget = 0;
    
    /** method reference to selected log handler (clear or drop) */
    private Operation logHandler = Operation.COMPLETE;
    
    // all below are used for center-of-mass and fall velocity handling
    private int LOG_FACTOR = 5; // logs worth this much more than leaves
    private int xStart = 0;
    private int zStart = 0;
    private int xSum = 0;
    private int zSum = 0;
    private double xVelocity = 0;
    private double zVelocity = 0;

    private Axis fallAxis = Axis.X;

    // all below are for compact representation of search space
    private static final byte POS_TYPE_LOG_FROM_ABOVE = 0;
    private static final byte POS_TYPE_LOG = 1;
    private static final byte POS_TYPE_LOG_FROM_DIAGONAL = 2;
    private static final byte POS_TYPE_LEAF = 3;
    private static final byte POS_TYPE_IGNORE = 4;

    // avoids littering code with (byte)0
    private static final byte ZERO_BYTE = 0;

    private class Visit {
        private final byte type;
        private final byte depth;
        private final long packedBlockPos;

        private Visit(long packedBlockPos, byte type, byte depth) {
            this.type = type;
            this.depth = depth;
            this.packedBlockPos = packedBlockPos;
        }
    }

    public void reset(TreeJob job) {
        this.job = job;
        dropHandler.reset(job);
        visited.clear();
        toVisit.clear();
        logs.clear();
        fallingLogs.clear();
        leaves.clear();
        leafCounts.clear();
        fx.reset();
        xSum = 0;
        zSum = 0;
        xStart = BlockPos.unpackLongX(job.startPos());
        zStart = BlockPos.unpackLongZ(job.startPos());
        prepIt = null;
        leafBlock = null;
        startState = Blocks.AIR.getDefaultState();
        startBlock = Blocks.AIR;
        operation = this::startSearch;
        fallingLogIndex = 0;
    }

    /** return true when complete */
    public boolean tick(World world) {
        fx.tick();
        breakBudget = Configurator.maxBreaksPerTick;
        
        final long maxTime = System.nanoTime() + 1000000000 / 100 * Configurator.tickBudget;
        
        while(doOp(world) && System.nanoTime() < maxTime) {} 
        
        return operation == Operation.COMPLETE;
    }
    
    /** returns false if cannot process more this tick */
    private boolean doOp(World world) {
        operation = operation.apply(world);
        
        if (operation == Operation.COMPLETE) {
            if (Configurator.stackDrops) {
                dropHandler.spawnDrops(world);
            }
            return false;
        } else {
            return breakBudget > 0;
        }
        
        //TODO: if configured to use held item, abort if stack has changed
       
    }

    private Operation startSearch(World world) {
        final long packedPos = job.startPos();
        searchPos.setFromLong(packedPos);

        BlockState state = world.getBlockState(searchPos);

        if (state.getBlock().matches(BlockTags.LOGS) && !(Configurator.protectPlayerLogs && Persistence.get(state))) {
            this.startState = state;
            this.startBlock = state.getBlock();

            this.visited.put(packedPos, POS_TYPE_LOG);

            // shoudln't really be necessary, but reflect the
            // reason we are doing this is the block below is (or was) non-supporting
            this.visited.put(BlockPos.add(packedPos, 0, -1, 0), POS_TYPE_IGNORE);

            enqueIfViable(BlockPos.add(packedPos, 0, 1, 0), POS_TYPE_LOG, ZERO_BYTE);
            enqueIfViable(BlockPos.add(packedPos, -1, 0, 0), POS_TYPE_LOG, ZERO_BYTE);
            enqueIfViable(BlockPos.add(packedPos, 1, 0, 0), POS_TYPE_LOG, ZERO_BYTE);
            enqueIfViable(BlockPos.add(packedPos, 0, 0, -1), POS_TYPE_LOG, ZERO_BYTE);
            enqueIfViable(BlockPos.add(packedPos, 0, 0, 1), POS_TYPE_LOG, ZERO_BYTE);

            enqueIfViable(BlockPos.add(packedPos, -1, 0, -1), POS_TYPE_LOG_FROM_DIAGONAL, ZERO_BYTE);
            enqueIfViable(BlockPos.add(packedPos, -1, 0, 1), POS_TYPE_LOG_FROM_DIAGONAL, ZERO_BYTE);
            enqueIfViable(BlockPos.add(packedPos, 1, 0, -1), POS_TYPE_LOG_FROM_DIAGONAL, ZERO_BYTE);
            enqueIfViable(BlockPos.add(packedPos, 1, 0, 1), POS_TYPE_LOG_FROM_DIAGONAL, ZERO_BYTE);

            enqueIfViable(BlockPos.add(packedPos, -1, 1, -1), POS_TYPE_LOG_FROM_DIAGONAL, ZERO_BYTE);
            enqueIfViable(BlockPos.add(packedPos, -1, 1, 0), POS_TYPE_LOG_FROM_DIAGONAL, ZERO_BYTE);
            enqueIfViable(BlockPos.add(packedPos, -1, 1, 1), POS_TYPE_LOG_FROM_DIAGONAL, ZERO_BYTE);
            enqueIfViable(BlockPos.add(packedPos, 0, 1, -1), POS_TYPE_LOG_FROM_DIAGONAL, ZERO_BYTE);
            enqueIfViable(BlockPos.add(packedPos, 0, 1, 1), POS_TYPE_LOG_FROM_DIAGONAL, ZERO_BYTE);
            enqueIfViable(BlockPos.add(packedPos, 1, 1, -1), POS_TYPE_LOG_FROM_DIAGONAL, ZERO_BYTE);
            enqueIfViable(BlockPos.add(packedPos, 1, 1, 0), POS_TYPE_LOG_FROM_DIAGONAL, ZERO_BYTE);
            enqueIfViable(BlockPos.add(packedPos, 1, 1, 1), POS_TYPE_LOG_FROM_DIAGONAL, ZERO_BYTE);
            return this::doSearch;
        } else
            return Operation.COMPLETE;

    }

    private Operation doSearch(World world) {
        final Visit toVisit = this.toVisit.poll();

        final long packedPos = toVisit.packedBlockPos;

        searchPos.setFromLong(packedPos);

        final byte fromType = toVisit.type;

        byte newDepth = (byte) (toVisit.depth + 1);

        if (!this.visited.containsKey(packedPos)) {
            BlockState state = world.getBlockState(searchPos);

            Block block = state.getBlock();

            if (block.matches(BlockTags.LEAVES)) {
                boolean validVisit = false;
                final LeafInfo inf = LeafInfo.get(block);

                if (fromType == POS_TYPE_LEAF) {
                    // leaf visit only valid from leaves that were one less away than this one
                    if (inf.applyAsInt(state) == Math.min(inf.maxDistance, newDepth + 1)) {
                        validVisit = true;
                        this.visited.put(packedPos, POS_TYPE_LEAF);
                    }
                } else { // assume coming from log

                    // leaf visits from logs always count as visited, even if will not be followed
                    this.visited.put(packedPos, POS_TYPE_LEAF);

                    // leaf visits coming from logs are expected to have distance = 1,
                    // otherwise they must belong to a different tree.
                    if (inf.applyAsInt(state) == 1) {
                        validVisit = true;

                        // restart depth of search when transitioning from log to leaf
                        newDepth = 0;

                        // leaves that are most common next to trunk are the ones we will break
                        leafCounts.addTo(block, 1);
                    }
                }

                if (validVisit) {
                    enqueIfViable(BlockPos.add(packedPos, 0, -1, 0), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(BlockPos.add(packedPos, 0, 1, 0), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(BlockPos.add(packedPos, -1, 0, 0), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(BlockPos.add(packedPos, 1, 0, 0), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(BlockPos.add(packedPos, 0, 0, -1), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(BlockPos.add(packedPos, 0, 0, 1), POS_TYPE_LEAF, newDepth);

                    enqueIfViable(BlockPos.add(packedPos, -1, 0, -1), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(BlockPos.add(packedPos, -1, 0, 1), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(BlockPos.add(packedPos, 1, 0, -1), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(BlockPos.add(packedPos, 1, 0, 1), POS_TYPE_LEAF, newDepth);

                    enqueIfViable(BlockPos.add(packedPos, -1, 1, -1), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(BlockPos.add(packedPos, -1, 1, 0), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(BlockPos.add(packedPos, -1, 1, 1), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(BlockPos.add(packedPos, 0, 1, -1), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(BlockPos.add(packedPos, 0, 1, 1), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(BlockPos.add(packedPos, 1, 1, -1), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(BlockPos.add(packedPos, 1, 1, 0), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(BlockPos.add(packedPos, 1, 1, 1), POS_TYPE_LEAF, newDepth);

                    enqueIfViable(BlockPos.add(packedPos, -1, -1, -1), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(BlockPos.add(packedPos, -1, -1, 0), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(BlockPos.add(packedPos, -1, -1, 1), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(BlockPos.add(packedPos, 0, -1, -1), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(BlockPos.add(packedPos, 0, -1, 1), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(BlockPos.add(packedPos, 1, -1, -1), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(BlockPos.add(packedPos, 1, -1, 0), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(BlockPos.add(packedPos, 1, -1, 1), POS_TYPE_LEAF, newDepth);
                }
            } else if (fromType != POS_TYPE_LEAF) {
                // visiting from wood (ignore type never added to queue)
                if (block == this.startState.getBlock() && !(Configurator.protectPlayerLogs && Persistence.get(state))) {
                    this.visited.put(packedPos, POS_TYPE_LOG);

                    enqueIfViable(BlockPos.add(packedPos, 0, -1, 0), POS_TYPE_LOG_FROM_ABOVE, newDepth);
                    enqueIfViable(BlockPos.add(packedPos, 0, 1, 0), POS_TYPE_LOG, newDepth);
                    enqueIfViable(BlockPos.add(packedPos, -1, 0, 0), POS_TYPE_LOG, newDepth);
                    enqueIfViable(BlockPos.add(packedPos, 1, 0, 0), POS_TYPE_LOG, newDepth);
                    enqueIfViable(BlockPos.add(packedPos, 0, 0, -1), POS_TYPE_LOG, newDepth);
                    enqueIfViable(BlockPos.add(packedPos, 0, 0, 1), POS_TYPE_LOG, newDepth);

                    enqueIfViable(BlockPos.add(packedPos, -1, 0, -1), POS_TYPE_LOG_FROM_DIAGONAL, newDepth);
                    enqueIfViable(BlockPos.add(packedPos, -1, 0, 1), POS_TYPE_LOG_FROM_DIAGONAL, newDepth);
                    enqueIfViable(BlockPos.add(packedPos, 1, 0, -1), POS_TYPE_LOG_FROM_DIAGONAL, newDepth);
                    enqueIfViable(BlockPos.add(packedPos, 1, 0, 1), POS_TYPE_LOG_FROM_DIAGONAL, newDepth);

                    enqueIfViable(BlockPos.add(packedPos, -1, 1, -1), POS_TYPE_LOG_FROM_DIAGONAL, newDepth);
                    enqueIfViable(BlockPos.add(packedPos, -1, 1, 0), POS_TYPE_LOG_FROM_DIAGONAL, newDepth);
                    enqueIfViable(BlockPos.add(packedPos, -1, 1, 1), POS_TYPE_LOG_FROM_DIAGONAL, newDepth);
                    enqueIfViable(BlockPos.add(packedPos, 0, 1, -1), POS_TYPE_LOG_FROM_DIAGONAL, newDepth);
                    enqueIfViable(BlockPos.add(packedPos, 0, 1, 1), POS_TYPE_LOG_FROM_DIAGONAL, newDepth);
                    enqueIfViable(BlockPos.add(packedPos, 1, 1, -1), POS_TYPE_LOG_FROM_DIAGONAL, newDepth);
                    enqueIfViable(BlockPos.add(packedPos, 1, 1, 0), POS_TYPE_LOG_FROM_DIAGONAL, newDepth);
                    enqueIfViable(BlockPos.add(packedPos, 1, 1, 1), POS_TYPE_LOG_FROM_DIAGONAL, newDepth);
                } else {
                    if (fromType == POS_TYPE_LOG_FROM_ABOVE) {
                        // if found a supporting block for a connected log
                        // then tree remains standing
                        if (Block.isFaceFullSquare(state.getCollisionShape(world, searchPos, EntityContext.absent()), Direction.UP)) {
                            return Operation.COMPLETE;
                        }
                    }
                    this.visited.put(packedPos, POS_TYPE_IGNORE);
                }

            }
        }

        if (this.toVisit.isEmpty()) {
            prepIt = this.visited.long2ByteEntrySet().iterator();

            // id most common leaf type next to trunk - will use it for breaking
            if (!leafCounts.isEmpty()) {
                int max = 0;
                ObjectIterator<it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<Block>> it = leafCounts.object2IntEntrySet().iterator();
                while (it.hasNext()) {
                    it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<Block> e = it.next();
                    if (e.getIntValue() > max) {
                        max = e.getIntValue();
                        leafBlock = e.getKey();
                    }
                }
            }

            return this::doPreClearing;
        } else {
            return this::doSearch;
        }
    }

    private void enqueIfViable(long packedPos, byte type, byte depth) {
        if (this.visited.containsKey(packedPos))
            return;

        if (depth == Byte.MAX_VALUE || depth < 0)
            return;

        this.toVisit.offer(new Visit(packedPos, type, depth));
    }

    private Operation doPreClearing(World world) {
        final ObjectIterator<Entry> prepIt = this.prepIt;

        if (prepIt.hasNext()) {
            Entry e = prepIt.next();
            final byte type = e.getByteValue();
            if (type != POS_TYPE_IGNORE) {
                final long pos = e.getLongKey();
                if (type == POS_TYPE_LEAF) {
                    if (Configurator.keepLogsIntact) {
                        xSum += (BlockPos.unpackLongX(pos) - xStart);
                        zSum += (BlockPos.unpackLongZ(pos) - zStart);
                    }
                    leaves.enqueue(pos);
                } else {
                    if (Configurator.keepLogsIntact) {
                        xSum += (BlockPos.unpackLongX(pos) - xStart) * LOG_FACTOR;
                        zSum += (BlockPos.unpackLongZ(pos) - zStart) * LOG_FACTOR;
                    }
                    logs.enqueue(pos);
                }
            }
            return this::doPreClearing;
        } else {
            fx.addExpected(leaves.size());
            if(!Configurator.keepLogsIntact) {
                fx.addExpected(logs.size());
            }
            return prepareLogs();
        }
    }

    private Operation doLeafClearing(World world) {
        long packedPos = leaves.dequeueLong();
        BlockPos pos = searchPos.setFromLong(packedPos);
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        if (block == leafBlock) {
            breakBlock(pos, world);
            breakBudget--;
        }
        
        if(leaves.isEmpty()) {
            // drop logs now in case player doesn't want to wait for logs
            dropHandler.spawnDrops(world);
            return this.logHandler;
        } else {
            return this::doLeafClearing;
        }
    }

    private Operation doLogClearing(World world) {
        final long packedPos = logs.dequeueLong();
        final BlockPos pos = searchPos.setFromLong(packedPos);
        final BlockState state = world.getBlockState(pos);
        final Block block = state.getBlock();

        if (block == startBlock) {
            breakBudget--;
            breakBlock(pos, world);
        }

        return this.logs.isEmpty() ? Operation.COMPLETE : this::doLogClearing;
    }

    private void breakBlock(BlockPos pos, World world) {
        BlockState blockState = world.getBlockState(pos);
        Block block = blockState.getBlock();
        if (block != startBlock && block != leafBlock) {
            // notify fx to increase chance because chance is based on totals reported earlier
            fx.request(false);
            return;
        }
        final FluidState fluidState = world.getFluidState(pos);
        final BlockEntity blockEntity = startBlock.hasBlockEntity() ? world.getBlockEntity(pos) : null;

        dropHandler.doDrops(blockState, world, pos, blockEntity);
        Dispatcher.suspend();
        world.setBlockState(pos, fluidState.getBlockState(), 3);
        Dispatcher.resume();
        
        if(fx.request(true)) {
            world.playLevelEvent(2001, pos, Block.getRawIdFromState(blockState));
        }
        
        //TODO: handle these
//        playerEntity_1.incrementStat(Stats.MINED.getOrCreateStat(this));
//        playerEntity_1.addExhaustion(0.005F);
    }

    private Operation prepareLogs() {
        if (logs.isEmpty()) {
            logHandler = Operation.COMPLETE;
        } else if (Configurator.keepLogsIntact) {
            logHandler = this::doLogDropping1;
            final double div = logs.size() * LOG_FACTOR + leaves.size();
            final double xCenterOfMass = xStart + xSum / div;
            final double zCenterOfMass = zStart + zSum / div;
            final Random r = ThreadLocalRandom.current();

            xVelocity = xCenterOfMass - xStart;
            zVelocity = zCenterOfMass - zStart;
            if (xVelocity == 0 && zVelocity == 0) {
                xVelocity = r.nextGaussian();
                zVelocity = r.nextGaussian();
            }

            // normalize
            double len = Math.sqrt(xVelocity * xVelocity + zVelocity * zVelocity);
            xVelocity /= len;
            zVelocity /= len;

            fallAxis = Math.abs(xVelocity) > Math.abs(zVelocity) ? Axis.X : Axis.Z;

            while (!logs.isEmpty()) {
                fallingLogs.add(logs.dequeueLastLong());
            }
            fallingLogs.sort((l0, l1) -> Integer.compare(BlockPos.unpackLongY(l1), BlockPos.unpackLongY(l0)));
        } else {
            logHandler = this::doLogClearing;
        }

        if (!leaves.isEmpty() && leafBlock != null) {
            return this::doLeafClearing;
        } else {
            return this.logHandler;
        }
    }

    //TODO: implement block break limits for falling logs
    private Operation doLogDropping1(World world) {
        final int limit = fallingLogs.size();
        if (limit == 0) {
            return Operation.COMPLETE;
        }

        final int i = fallingLogIndex++;
        if (i >= limit) {
            fallingLogIndex = 0;
            return this::doLogDropping2;
        }

        final long packedPos = fallingLogs.getLong(i);
        final BlockPos pos = searchPos.setFromLong(packedPos);
        world.setBlockState(pos, Blocks.AIR.getDefaultState());
        breakBudget--;
        return this::doLogDropping1;
    }

    private Operation doLogDropping2(World world) {
        final int limit = fallingLogs.size();
        if (limit == 0) {
            return Operation.COMPLETE;
        }

        final int i = fallingLogIndex++;
        if (i >= limit) {
            return Operation.COMPLETE;
        }
        final long packedPos = fallingLogs.getLong(i);
        final BlockPos pos = searchPos.setFromLong(packedPos);
        FallingLogEntity entity = new FallingLogEntity(world, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, startState.with(LogBlock.AXIS, fallAxis));
        double height = Math.sqrt(Math.max(0, pos.getY() - BlockPos.unpackLongY(job.startPos()))) * 0.2;
        entity.addVelocity(xVelocity * height, 0, zVelocity * height);
        world.spawnEntity(entity);
        return this::doLogDropping2;
    }
}
