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

package grondag.tdnf;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Random;

import grondag.fermion.world.PackedBlockPos;
import grondag.tdnf.Configurator.EffectLevel;
import io.netty.util.internal.ThreadLocalRandom;
import it.unimi.dsi.fastutil.longs.Long2ByteMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.LogBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EntityContext;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Call when lava is placed beneath a log.  Will check for a tree-like structure
 * starting at the point and if it is not resting on other non-volcanic blocks
 * will destroy the tree. <p>
 * 
 * Not thread-safe.  Meant to be called from server thread.<p>
 * 
 * Current implementation is a bit sloppy and seems to miss some leaves/logs
 * and leaf decay isn't immediate as desired / expected.  
 *
 */
public class TreeCutter
{
    private Operation operation = Operation.STARTING;

    /** if search in progress, starting state of search */
    private BlockState startState = Blocks.AIR.getDefaultState(); 

    private Block startBlock;

    private final PriorityQueue<Visit> toVisit = new PriorityQueue<>(
            new Comparator<Visit>() 
            {
                @Override
                public int compare(Visit o1, Visit o2)
                {
                    return Byte.compare(o1.type, o2.type);
                }
            });

    /** packed positions that have received a valid visit */
    private final Long2ByteOpenHashMap visited = new Long2ByteOpenHashMap();

    /** packed positions of logs to be cleared - populated during pre-clearing */
    private final LongArrayFIFOQueue logs = new LongArrayFIFOQueue();

    /** packed positions of logs to fall - sorted by Y - needed cuz can't sort FIFO Queue */
    private final LongArrayList fallingLogs = new LongArrayList();

    /** leaves to be cleared - populated during pre-clearing */
    private final LongArrayFIFOQueue leaves = new LongArrayFIFOQueue();

    /** Used to determine which leaf block should be {@link #leafBlock} */
    private final Object2IntOpenHashMap<Block> leafCounts = new Object2IntOpenHashMap<>();

    /** general purpose mutable pos */
    private final BlockPos.Mutable searchPos = new BlockPos.Mutable();

    /** holds consolidated drops */
    private final ObjectArrayList<ItemStack> drops = new ObjectArrayList<>();

    /** iterator traversed durnig pre-clearing */
    private ObjectIterator<Entry> prepIt = null;

    /** most common leaf block next to trunk */
    private Block leafBlock = null;

    /** packed staring pos */
    private long startPos;

    /** true when limiting particles, etc. - ensures we do at least one */
    private boolean needsFirstEffect = true;

    /** cooldown timer if configured */
    private int cooldownTicks = 0;
    
    // all below are used for center-of-mass and fall velocity handling
    private int xStart = 0;
    private int zStart = 0;
    private int xSum = 0;
    private int zSum = 0;
    private double xVelocity = 0;
    private double zVelocity = 0;

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

    public void reset(long startPos) {
        visited.clear();
        toVisit.clear();
        logs.clear();
        fallingLogs.clear();
        leaves.clear();
        drops.clear();
        leafCounts.clear();
        xSum = 0;
        zSum = 0;
        xStart = PackedBlockPos.getX(startPos);
        zStart = PackedBlockPos.getZ(startPos);
        prepIt = null;
        leafBlock = null;
        startState = Blocks.AIR.getDefaultState();
        startBlock = Blocks.AIR;
        this.startPos = startPos;
        operation = Operation.STARTING;
        needsFirstEffect = true;
    }

    public boolean tick(World world) {
        if(cooldownTicks > 0) {
            cooldownTicks--;
            return false;
        }

        switch(this.operation) {
        case STARTING:
            this.operation = startSearch(world);
            break;

        case SEARCHING: {
            Operation op = Operation.SEARCHING;
            int budget = Configurator.maxSearchPosPerTick;
            while(budget-- > 0 && op == Operation.SEARCHING) {
                op = doSearch(world);
            }
            this.operation = op;
            break;
        }

        case PRECLEARING: {
            Operation op = Operation.PRECLEARING;
            int budget = Configurator.maxSearchPosPerTick;
            while(budget-- > 0 && op == Operation.PRECLEARING) {
                op = doPreClearing(world);
            }
            this.operation = op;
            break;
        }

        case CLEARING_LEAVES: {
            Operation op = Operation.CLEARING_LEAVES;
            int budget = Configurator.maxBreaksPerTick;
            while(budget-- > 0 && op == Operation.CLEARING_LEAVES) {
                op = doLeafClearing(world);
            }
            this.cooldownTicks = Configurator.breakCooldownTicks;
            this.operation = op;
            // drop logs now in case player doesn't want to wait for logs
            if(op != Operation.CLEARING_LEAVES && Configurator.stackDrops) {
                spawnDrops(world);
            }
            break;
        }

        case CLEARING_LOGS: {
            Operation op = Operation.CLEARING_LOGS;
            int budget = Configurator.maxBreaksPerTick;
            while(budget-- > 0 && op == Operation.CLEARING_LOGS) {
                op = doLogClearing(world);
            }
            this.cooldownTicks = Configurator.breakCooldownTicks;
            this.operation = op;
            break;
        }

        case DROPPING_LOGS: {
            Operation op = Operation.DROPPING_LOGS;
            int budget = Configurator.maxBreaksPerTick;
            while(budget-- > 0 && op == Operation.DROPPING_LOGS) {
                op = doLogDropping(world);
            }
            this.cooldownTicks = Configurator.breakCooldownTicks;
            this.operation = op;
            break;
        }
        
        default:
            operation = Operation.COMPLETE; // handle WTF
        case COMPLETE:
            break;
        }

        if(operation == Operation.COMPLETE) {
            if(Configurator.stackDrops) {
                spawnDrops(world);
            }
            return true;
        } else {
            return false;
        }
    }

    private Operation startSearch(World world) {
        final long packedPos = this.startPos;
        PackedBlockPos.unpackTo(packedPos, searchPos);

        BlockState state = world.getBlockState(searchPos);

        if(state.getBlock().matches(BlockTags.LOGS)) {

            this.startState = state;
            this.startBlock = state.getBlock();

            this.visited.put(packedPos, POS_TYPE_LOG);

            // shoudln't really be necessary, but reflect the
            // reason we are doing this is the block below is (or was) hot lava
            this.visited.put(PackedBlockPos.down(packedPos), POS_TYPE_IGNORE);


            enqueIfViable(PackedBlockPos.east(packedPos), POS_TYPE_LOG, ZERO_BYTE);
            enqueIfViable(PackedBlockPos.west(packedPos), POS_TYPE_LOG, ZERO_BYTE);
            enqueIfViable(PackedBlockPos.north(packedPos), POS_TYPE_LOG, ZERO_BYTE);
            enqueIfViable(PackedBlockPos.south(packedPos), POS_TYPE_LOG, ZERO_BYTE);
            enqueIfViable(PackedBlockPos.up(packedPos), POS_TYPE_LOG, ZERO_BYTE);

            enqueIfViable(PackedBlockPos.add(packedPos, -1, 0, -1), POS_TYPE_LOG_FROM_DIAGONAL, ZERO_BYTE);
            enqueIfViable(PackedBlockPos.add(packedPos, -1, 0, 1), POS_TYPE_LOG_FROM_DIAGONAL, ZERO_BYTE);
            enqueIfViable(PackedBlockPos.add(packedPos, 1, 0, -1), POS_TYPE_LOG_FROM_DIAGONAL, ZERO_BYTE);
            enqueIfViable(PackedBlockPos.add(packedPos, 1, 0, 1), POS_TYPE_LOG_FROM_DIAGONAL, ZERO_BYTE);

            enqueIfViable(PackedBlockPos.add(packedPos, -1, 1, -1), POS_TYPE_LOG_FROM_DIAGONAL, ZERO_BYTE);
            enqueIfViable(PackedBlockPos.add(packedPos, -1, 1, 0), POS_TYPE_LOG_FROM_DIAGONAL, ZERO_BYTE);
            enqueIfViable(PackedBlockPos.add(packedPos, -1, 1, 1), POS_TYPE_LOG_FROM_DIAGONAL, ZERO_BYTE);
            enqueIfViable(PackedBlockPos.add(packedPos, 0, 1, -1), POS_TYPE_LOG_FROM_DIAGONAL, ZERO_BYTE);
            enqueIfViable(PackedBlockPos.add(packedPos, 0, 1, 1), POS_TYPE_LOG_FROM_DIAGONAL, ZERO_BYTE);
            enqueIfViable(PackedBlockPos.add(packedPos, 1, 1, -1), POS_TYPE_LOG_FROM_DIAGONAL, ZERO_BYTE);
            enqueIfViable(PackedBlockPos.add(packedPos, 1, 1, 0), POS_TYPE_LOG_FROM_DIAGONAL, ZERO_BYTE);
            enqueIfViable(PackedBlockPos.add(packedPos, 1, 1, 1), POS_TYPE_LOG_FROM_DIAGONAL, ZERO_BYTE);
            return Operation.SEARCHING;
        }
        else return Operation.COMPLETE;

    }

    private Operation doSearch(World world) {
        final Visit toVisit = this.toVisit.poll();

        final long packedPos = toVisit.packedBlockPos;

        PackedBlockPos.unpackTo(packedPos, searchPos);

        final byte fromType = toVisit.type;

        byte newDepth = (byte) (toVisit.depth + 1);

        if(!this.visited.containsKey(packedPos)) {
            BlockState state = world.getBlockState(searchPos);

            Block block = state.getBlock();

            if(block.matches(BlockTags.LEAVES)) {
                boolean validVisit = false;

                if(fromType == POS_TYPE_LEAF) {
                    // leaf visit only valid from leaves that were one less away than this one
                    if(state.get(LeavesBlock.DISTANCE) == Math.min(7, newDepth + 1)) {
                        validVisit = true;
                        this.visited.put(packedPos, POS_TYPE_LEAF);
                    }
                } else { // assume coming from log

                    // leaf visits from logs always count as visited, even if will not be followed
                    this.visited.put(packedPos, POS_TYPE_LEAF);

                    // leaf visits coming from logs are expected to have distance = 1,
                    // otherwise they must belong to a different tree.
                    if(state.get(LeavesBlock.DISTANCE) == 1) {
                        validVisit = true;

                        // restart depth of search when transitioning from log to leaf
                        newDepth = 0;

                        // leaves that are most common next to trunk are the ones we will break
                        leafCounts.addTo(block, 1);
                    }
                }

                if(validVisit) {
                    enqueIfViable(PackedBlockPos.up(packedPos), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(PackedBlockPos.east(packedPos), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(PackedBlockPos.west(packedPos), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(PackedBlockPos.north(packedPos), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(PackedBlockPos.south(packedPos), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(PackedBlockPos.down(packedPos), POS_TYPE_LEAF, newDepth);

                    enqueIfViable(PackedBlockPos.add(packedPos, -1, 0, -1), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(PackedBlockPos.add(packedPos, -1, 0, 1), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(PackedBlockPos.add(packedPos, 1, 0, -1), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(PackedBlockPos.add(packedPos, 1, 0, 1), POS_TYPE_LEAF, newDepth);

                    enqueIfViable(PackedBlockPos.add(packedPos, -1, 1, -1), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(PackedBlockPos.add(packedPos, -1, 1, 0), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(PackedBlockPos.add(packedPos, -1, 1, 1), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(PackedBlockPos.add(packedPos, 0, 1, -1), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(PackedBlockPos.add(packedPos, 0, 1, 1), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(PackedBlockPos.add(packedPos, 1, 1, -1), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(PackedBlockPos.add(packedPos, 1, 1, 0), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(PackedBlockPos.add(packedPos, 1, 1, 1), POS_TYPE_LEAF, newDepth);

                    enqueIfViable(PackedBlockPos.add(packedPos, -1, -1, -1), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(PackedBlockPos.add(packedPos, -1, -1, 0), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(PackedBlockPos.add(packedPos, -1, -1, 1), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(PackedBlockPos.add(packedPos, 0, -1, -1), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(PackedBlockPos.add(packedPos, 0, -1, 1), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(PackedBlockPos.add(packedPos, 1, -1, -1), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(PackedBlockPos.add(packedPos, 1, -1, 0), POS_TYPE_LEAF, newDepth);
                    enqueIfViable(PackedBlockPos.add(packedPos, 1, -1, 1), POS_TYPE_LEAF, newDepth);
                }
            }
            else if(fromType != POS_TYPE_LEAF) {
                // visiting from wood (ignore type never added to queue)
                if(block == this.startState.getBlock()) {
                    this.visited.put(packedPos, POS_TYPE_LOG);

                    enqueIfViable(PackedBlockPos.down(packedPos), POS_TYPE_LOG_FROM_ABOVE, newDepth);
                    enqueIfViable(PackedBlockPos.east(packedPos), POS_TYPE_LOG, newDepth);
                    enqueIfViable(PackedBlockPos.west(packedPos), POS_TYPE_LOG, newDepth);
                    enqueIfViable(PackedBlockPos.north(packedPos), POS_TYPE_LOG, newDepth);
                    enqueIfViable(PackedBlockPos.south(packedPos), POS_TYPE_LOG, newDepth);
                    enqueIfViable(PackedBlockPos.up(packedPos), POS_TYPE_LOG, newDepth);

                    enqueIfViable(PackedBlockPos.add(packedPos, -1, 0, -1), POS_TYPE_LOG_FROM_DIAGONAL, newDepth);
                    enqueIfViable(PackedBlockPos.add(packedPos, -1, 0, 1), POS_TYPE_LOG_FROM_DIAGONAL, newDepth);
                    enqueIfViable(PackedBlockPos.add(packedPos, 1, 0, -1), POS_TYPE_LOG_FROM_DIAGONAL, newDepth);
                    enqueIfViable(PackedBlockPos.add(packedPos, 1, 0, 1), POS_TYPE_LOG_FROM_DIAGONAL, newDepth);

                    enqueIfViable(PackedBlockPos.add(packedPos, -1, 1, -1), POS_TYPE_LOG_FROM_DIAGONAL, newDepth);
                    enqueIfViable(PackedBlockPos.add(packedPos, -1, 1, 0), POS_TYPE_LOG_FROM_DIAGONAL, newDepth);
                    enqueIfViable(PackedBlockPos.add(packedPos, -1, 1, 1), POS_TYPE_LOG_FROM_DIAGONAL, newDepth);
                    enqueIfViable(PackedBlockPos.add(packedPos, 0, 1, -1), POS_TYPE_LOG_FROM_DIAGONAL, newDepth);
                    enqueIfViable(PackedBlockPos.add(packedPos, 0, 1, 1), POS_TYPE_LOG_FROM_DIAGONAL, newDepth);
                    enqueIfViable(PackedBlockPos.add(packedPos, 1, 1, -1), POS_TYPE_LOG_FROM_DIAGONAL, newDepth);
                    enqueIfViable(PackedBlockPos.add(packedPos, 1, 1, 0), POS_TYPE_LOG_FROM_DIAGONAL, newDepth);
                    enqueIfViable(PackedBlockPos.add(packedPos, 1, 1, 1), POS_TYPE_LOG_FROM_DIAGONAL, newDepth);
                }
                else {
                    if(fromType == POS_TYPE_LOG_FROM_ABOVE) {
                        // if found a supporting block for a connected log
                        // then tree remains stanging
                        if(Block.isFaceFullSquare(state.getCollisionShape(world, searchPos, EntityContext.absent()), Direction.UP)) {
                            return Operation.COMPLETE;
                        }
                    }
                    this.visited.put(packedPos, POS_TYPE_IGNORE);
                }

            }
        }

        if(this.toVisit.isEmpty()) {
            prepIt = this.visited.long2ByteEntrySet().iterator();

            // id most common leaf type next to trunk - will use it for breaking
            if(!leafCounts.isEmpty()) {
                int max = 0;
                ObjectIterator<it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<Block>> it = leafCounts.object2IntEntrySet().iterator();
                while(it.hasNext()) {
                    it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<Block> e = it.next();
                    if(e.getIntValue() > max) {
                        max = e.getIntValue();
                        leafBlock = e.getKey();
                    }
                }
            }

            return Operation.PRECLEARING;
        } else {
            return Operation.SEARCHING;
        }
    }

    private void enqueIfViable(long packedPos, byte type, byte depth) {
        if(this.visited.containsKey(packedPos)) return;

        if(depth == Byte.MAX_VALUE || depth < 0) return;

        this.toVisit.offer(new Visit(packedPos, type, depth));
    }

    private Operation doPreClearing(World world) {
        final ObjectIterator<Entry> prepIt = this.prepIt;

        if(prepIt.hasNext()) {
            Entry e = prepIt.next();
            final byte type = e.getByteValue();
            if(type != POS_TYPE_IGNORE) {
                if(type == POS_TYPE_LEAF) {
                    leaves.enqueue(e.getLongKey());
                } else {
                    long pos = e.getLongKey();
                    if(Configurator.fallingBlocks) {
                        xSum += PackedBlockPos.getX(pos) - xStart;
                        zSum += PackedBlockPos.getZ(pos) - zStart;
                    }
                    logs.enqueue(pos);
                }
            }
            return Operation.PRECLEARING;
        } else {
            if(!leaves.isEmpty() && leafBlock != null) {
                return Operation.CLEARING_LEAVES;
            } else if(!logs.isEmpty()) {
                return Operation.CLEARING_LOGS;
            } else {
                return Operation.COMPLETE;
            }
        }
    }

    private Operation doLeafClearing(World world) {
        long packedPos = leaves.dequeueLong();
        BlockPos pos = PackedBlockPos.unpackTo(packedPos, searchPos);
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        if(block == leafBlock) {
            breakBlock(pos, world);
        }

        return leaves.isEmpty() ? prepareLogs():  Operation.CLEARING_LEAVES;
    }

    private Operation doLogClearing(World world) {
        final long packedPos = logs.dequeueLong();
        final BlockPos pos = PackedBlockPos.unpackTo(packedPos, searchPos);
        final BlockState state = world.getBlockState(pos);
        final Block block = state.getBlock();

        if(block == startBlock) {
            breakBlock(pos, world);
        }

        return this.logs.isEmpty() ? Operation.COMPLETE :  Operation.CLEARING_LOGS;
    }

    private void breakBlock(BlockPos pos, World world) {
        BlockState blockState = world.getBlockState(pos);
        Block block = blockState.getBlock();
        if (block != startBlock && block != leafBlock) {
            return;
        }
        final FluidState fluidState = world.getFluidState(pos);
        final BlockEntity blockEntity = startBlock.hasBlockEntity() ? world.getBlockEntity(pos) : null;
                
        doDrops(blockState, world, pos, blockEntity);
        world.setBlockState(pos, fluidState.getBlockState(), 3);

        final EffectLevel fxLevel = Configurator.effectLevel;
        if(fxLevel == EffectLevel.ALL || (fxLevel == EffectLevel.SOME && (needsFirstEffect || ThreadLocalRandom.current().nextInt(4) == 0))) {
            world.playLevelEvent(2001, pos, Block.getRawIdFromState(blockState));
            needsFirstEffect = false;
        }
    }

    private Operation prepareLogs() {
        if(logs.isEmpty()) {
            return Operation.COMPLETE;
        }
        if(Configurator.fallingBlocks) {
            final double div = logs.size();
            final double xCenterOfMass = xStart + xSum / div;
            final double zCenterOfMass = zStart + zSum / div;
            final Random r = ThreadLocalRandom.current();
            
            xVelocity = xCenterOfMass - xStart; 
            if(xVelocity == 0) {
                xVelocity = r.nextGaussian();
            } else 
            
            zVelocity = zCenterOfMass - zStart; 
            if(zVelocity == 0) {
                zVelocity = r.nextGaussian();
            }
            
            // normalize
            double len = Math.sqrt(xVelocity * xVelocity + zVelocity * zVelocity);
            xVelocity /= len;
            zVelocity /= len;
                    
            while(!logs.isEmpty()) {
                fallingLogs.add(logs.dequeueLastLong());
            }
            // note inverse order so we can peel off the tail of the list vs the head
            fallingLogs.sort((l0, l1) -> Integer.compare(PackedBlockPos.getY(l1), PackedBlockPos.getY(l0)));
            
            return fallingLogs.isEmpty() ? Operation.COMPLETE : Operation.DROPPING_LOGS;
        } else {
            return Operation.CLEARING_LOGS;
        }
    }

    private Operation doLogDropping(World world) {
        if(fallingLogs.isEmpty()) {
            return Operation.COMPLETE;
        }
        final long packedPos = fallingLogs.popLong();
        final BlockPos pos = PackedBlockPos.unpackTo(packedPos, searchPos);
        FallingLogEntity entity = new FallingLogEntity(world, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, startState.cycle(LogBlock.AXIS));
        double height = Math.sqrt(Math.max(0, pos.getY() - PackedBlockPos.getY(startPos))) * 0.2;
        entity.addVelocity(xVelocity * height, 0, zVelocity * height);
        world.spawnEntity(entity);
        
        return fallingLogs.isEmpty() ? Operation.COMPLETE :  Operation.DROPPING_LOGS;
    }

    private void doDrops(BlockState blockState, World world, BlockPos pos, BlockEntity blockEntity) {
        if(Configurator.stackDrops) {
            Block.getDroppedStacks(blockState, (ServerWorld)world, pos, blockEntity).forEach(s -> consolidateDrops(world, s));
            //XP, etc. - probably not needed for logs but just in case
            blockState.onStacksDropped(world, pos, ItemStack.EMPTY);
        } else {
            Block.dropStacks(blockState, world, pos, blockEntity);
        }
    }

    private void consolidateDrops(World world, ItemStack stack) {
        if(stack == null || stack.isEmpty()) {
            return;
        }
        stack = stack.copy();

        if(!stack.canStack()) {
            Block.dropStack(world, PackedBlockPos.unpackTo(startPos, searchPos), stack);
            return;
        }

        final ObjectArrayList<ItemStack> drops = this.drops;

        if(drops.isEmpty() || !stack.canStack()) {
            drops.add(stack);
        } else {
            final int limit = drops.size();
            for(int i = 0; i < limit; i++) {
                ItemStack existing = drops.get(i);
                final int capacity = existing.getMaxAmount() - existing.getAmount();
                if(capacity > 0 && stack.getItem() == existing.getItem() 
                        && ItemStack.areTagsEqual(stack, existing)) {
                    int amt = Math.min(stack.getAmount(), capacity);
                    if(amt > 0) {
                        stack.subtractAmount(amt);
                        existing.addAmount(amt);
                    }

                    if(existing.getAmount() == existing.getMaxAmount()) {
                        Block.dropStack(world, PackedBlockPos.unpackTo(startPos, searchPos), existing);
                        drops.remove(i);
                        break;
                    }

                    if(stack.isEmpty()) {
                        return;
                    }
                }
            }
            if(!stack.isEmpty()) {
                drops.add(stack);
            }
        }
    }

    private void spawnDrops(World world) {
        if(!drops.isEmpty()) {
            BlockPos pos = PackedBlockPos.unpackTo(startPos, searchPos);
            final int limit = drops.size();
            for(int i = 0; i < limit; i++) {
                Block.dropStack(world, pos, drops.get(i));
            }
            drops.clear();
        }

    }
}
