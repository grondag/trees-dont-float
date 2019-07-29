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
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LogBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EntityContext;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
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

    private final PriorityQueue<Visit> toVisit = new PriorityQueue<>(new Comparator<Visit>() {
        @Override
        public int compare(Visit o1, Visit o2) {
            return Byte.compare(o1.type, o2.type);
        }
    });

    /** packed positions that have received a valid visit */
    private final Long2ByteOpenHashMap visited = new Long2ByteOpenHashMap();

    private final LongOpenHashSet doomed = new LongOpenHashSet();
    
    /** packed positions of logs to be cleared - populated during pre-clearing */
    private final LongArrayFIFOQueue logs = new LongArrayFIFOQueue();

    /**
     * packed positions of logs to fall - sorted by Y - needed cuz can't sort FIFO
     * Queue
     */
    private final LongArrayList fallingLogs = new LongArrayList();

    private final ObjectArrayList<BlockState> fallingLogStates = new ObjectArrayList<>(); 
    
    /** Used to iterate {@link #fallingLogs} */
    private int fallingLogIndex = 0;

    /** leaves to be cleared - populated during pre-clearing */
    private final LongArrayFIFOQueue leaves = new LongArrayFIFOQueue();

    /** general purpose mutable pos */
    private final BlockPos.Mutable searchPos = new BlockPos.Mutable();

    /** iterator traversed during pre-clearing */
    private ObjectIterator<Entry> prepIt = null;

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
        doomed.clear();
        toVisit.clear();
        logs.clear();
        fallingLogs.clear();
        fallingLogStates.clear();
        leaves.clear();
        fx.reset();
        xSum = 0;
        zSum = 0;
        xStart = BlockPos.unpackLongX(job.startPos());
        zStart = BlockPos.unpackLongZ(job.startPos());
        prepIt = null;
        operation = this::startSearch;
        fallingLogIndex = 0;
    }

    /** return true when complete */
    public boolean tick(World world) {
        if(job.isCancelled(world)) {
            if (Configurator.stackDrops) {
                dropHandler.spawnDrops(world);
            }
            return true;
        }

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
    }

    private Operation startSearch(World world) {
        final long packedPos = job.startPos();
        searchPos.setFromLong(packedPos);

        BlockState state = world.getBlockState(searchPos);

        if (state.getBlock().matches(BlockTags.LOGS) && !(Configurator.protectPlayerLogs && Persistence.get(state))) {
//            this.startState = state;
//            this.startBlock = state.getBlock();

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
                if (BlockTags.LOGS.contains(block) && !(Configurator.protectPlayerLogs && Persistence.get(state))) {
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
                doomed.add(pos);
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
            // Confirm tool has adequate durability
            // This only matters when we have to protect the tool or when we are keeping logs intact.
            // It matters when logs are intact because we remove all the logs first and then spawn them
            // incrementally. If we run out of durability mid-removal it gets weird due to lack of fancy physics.
            if(job.hasAxe() && Configurator.consumeDurability && (Configurator.protectTools || Configurator.keepLogsIntact)) {
                final ItemStack stack = job.stack();
                final int capacity = stack.isEmpty() ? 0 : stack.getMaxDamage() - stack.getDamage();
                final int needed = logs.size() + (Configurator.leafDurability ? leaves.size() : 0);
                if(needed >= capacity) {
                    return Operation.COMPLETE;
                }
            }

            fx.addExpected(leaves.size());
            if(!Configurator.keepLogsIntact) {
                fx.addExpected(logs.size());
            }
            return prepareLogs();
        }
    }

    private Operation doLeafClearing(World world) {
        if(leaves.isEmpty()) {
            return Operation.COMPLETE;
        }
        
        long packedPos = leaves.dequeueLong();
        BlockPos pos = searchPos.setFromLong(packedPos);
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        if (BlockTags.LEAVES.contains(block)) {
            if(!Configurator.leafDurability || checkDurability(world, state, pos)) {
                breakBlock(pos, world);
                breakBudget--;
            } else {
                return Operation.COMPLETE;
            }
        }

        return this::doLeafClearing;
    }

    private Operation doLogClearing(World world) {
        final long packedPos = fallingLogs.popLong();
        final BlockPos pos = searchPos.setFromLong(packedPos);
        final BlockState state = world.getBlockState(pos);
        final Block block = state.getBlock();

        if (BlockTags.LOGS.contains(block)) {
            if(checkDurability(world, state, pos)) {
                breakBudget--;
                breakBlock(pos, world);
            } else {
                return Operation.COMPLETE;
            }
        }

        if(fallingLogs.isEmpty()) {
            // drop leaves now in case player doesn't want to wait for logs
            dropHandler.spawnDrops(world);
            return this::doLeafClearing;
        } else {
            return this::doLogClearing;
        }
    }

    private void breakBlock(BlockPos pos, World world) {
        BlockState blockState = world.getBlockState(pos);
        Block block = blockState.getBlock();
        final boolean isLeaf = BlockTags.LEAVES.contains(block);
        if (!BlockTags.LOGS.contains(block) && !isLeaf) {
            // notify fx to increase chance because chance is based on totals reported earlier
            fx.request(false);
            return;
        }
        final FluidState fluidState = world.getFluidState(pos);
        final BlockEntity blockEntity = block.hasBlockEntity() ? world.getBlockEntity(pos) : null;

        dropHandler.doDrops(blockState, world, pos, blockEntity);
        Dispatcher.suspend(p -> doomed.contains(p.asLong()));
        world.setBlockState(pos, fluidState.getBlockState(), 3);
        Dispatcher.resume();

        if(fx.request(true)) {
            world.playLevelEvent(2001, pos, Block.getRawIdFromState(blockState));
        }

        applyHunger(isLeaf, block);
    }

    private boolean checkDurability(World world, BlockState state, BlockPos pos) {
        if(Configurator.consumeDurability && job.hasAxe() && !job.player().isCreative()) {
            final ItemStack stack = job.stack();
            if(Configurator.protectTools && stack.getDamage() >= stack.getMaxDamage() - 2) {
                return false;
            }
            stack.getItem().postMine(stack, world, state, pos, job.player());
            return true;
        } else {
            return true;
        }
    }

    private void applyHunger(boolean isLeaf, Block block) {
        if(Configurator.applyHunger && (!isLeaf || Configurator.leafHunger)) {
            final ServerPlayerEntity player = job.player();
            if(player != null && !player.isCreative()) {
                player.addExhaustion(0.005F);
                player.incrementStat(Stats.MINED.getOrCreateStat(block));
            }
        }
    }

    private Operation prepareLogs() {
        if (logs.isEmpty()) {
            logHandler = Operation.COMPLETE;
        } else {
            while (!logs.isEmpty()) {
                fallingLogs.add(logs.dequeueLastLong());
            }
            fallingLogs.sort((l0, l1) -> Integer.compare(BlockPos.unpackLongY(l1), BlockPos.unpackLongY(l0)));
            
            if (Configurator.keepLogsIntact) {
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
                
            } else {
                logHandler = this::doLogClearing;
            }
        }
        return this.logHandler;

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
            //FIXME: not in right place - what about logs partially completed - can abort then? See below also
            // logs are all removed so should not stop after this
            job.forceCompletion();
            return this::doLogDropping2;
        }

        final long packedPos = fallingLogs.getLong(i);
        final BlockPos pos = searchPos.setFromLong(packedPos);
        final BlockState state = world.getBlockState(pos);
        fallingLogStates.add(state);
        
        if(checkDurability(world, state, pos)) {
            applyHunger(false, state.getBlock());
            world.setBlockState(pos, Blocks.AIR.getDefaultState());

            breakBudget--;
            return this::doLogDropping1;
        } else {
            return Operation.COMPLETE;
        }
    }

    private Operation doLogDropping2(World world) {
        final int limit = fallingLogs.size();
        if (limit == 0 || fallingLogIndex >= limit - 1) {
            // drop leaves now in case player doesn't want to wait for logs
            dropHandler.spawnDrops(world);
            return this::doLeafClearing;
        }
        
        if(FallingLogEntity.canSpawn()) {
            final int i = fallingLogIndex++;
            final long packedPos = fallingLogs.getLong(i);
            final BlockPos pos = searchPos.setFromLong(packedPos);
            BlockState state = fallingLogStates.get(i);
            if(state.contains(LogBlock.AXIS)) {
                state = state.with(LogBlock.AXIS, fallAxis);
            }
            FallingLogEntity entity = new FallingLogEntity(world, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, state);
            double height = Math.sqrt(Math.max(0, pos.getY() - BlockPos.unpackLongY(job.startPos()))) * 0.2;
            entity.addVelocity(xVelocity * height, 0, zVelocity * height);
            world.spawnEntity(entity);
        } else {
            // force exit till next tick
            breakBudget = 0;
        }
        return this::doLogDropping2;
    }
}
