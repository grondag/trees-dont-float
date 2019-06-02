package grondag.tdnf;

import java.util.Comparator;
import java.util.PriorityQueue;

import grondag.fermion.world.PackedBlockPos;
import grondag.tdnf.Configurator.EffectLevel;
import io.netty.util.internal.ThreadLocalRandom;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EntityContext;
import net.minecraft.fluid.FluidState;
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
    private Operation operation = Operation.IDLE;

    /** if search in progress, starting state of search */
    private BlockState startState = Blocks.AIR.getDefaultState(); 

    /** If search in progress, starting point of search 
     *  Not used during search, but is serialized if saved while search in progress. */

    private final PriorityQueue<Visit> toVisit = new PriorityQueue<>(
            new Comparator<Visit>() 
            {
                @Override
                public int compare(Visit o1, Visit o2)
                {
                    return Byte.compare(o1.type, o2.type);
                }
            });

    private final Long2ByteOpenHashMap visited = new Long2ByteOpenHashMap();

    private final LongArrayFIFOQueue toClear = new LongArrayFIFOQueue();

    private final LongArrayFIFOQueue toTick = new LongArrayFIFOQueue();

    private final BlockPos.Mutable searchPos = new BlockPos.Mutable();

    private long startPos;

    private boolean needsFirstEffect = true;

    private int cooldownTicks = 0;

    private static final byte POS_TYPE_LOG_FROM_ABOVE = 0;
    private static final byte POS_TYPE_LOG = 1;
    private static final byte POS_TYPE_LOG_FROM_DIAGONAL = 2;
    private static final byte POS_TYPE_LEAF = 3;
    private static final byte POS_TYPE_IGNORE = 4;

    //wtf java? why?
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
                toClear.clear();
                startState = Blocks.AIR.getDefaultState();
                this.startPos = startPos;
                operation = Operation.IDLE;
                needsFirstEffect = true;
            }

            public boolean tick(World world) {
                if(cooldownTicks > 0) {
                    cooldownTicks--;
                    return false;
                }

                switch(this.operation) {
                case IDLE:
                    this.operation = startSearch(world);
                    return this.operation == Operation.COMPLETE;

                case SEARCHING: {
                    Operation op = Operation.SEARCHING;
                    int budget = Configurator.maxSearchPosPerTick;
                    while(budget-- > 0 && op == Operation.SEARCHING) {
                        op = doSearch(world);
                    }
                    this.operation = op;
                    return op == Operation.COMPLETE;
                }

                case CLEARING: {
                    Operation op = Operation.CLEARING;
                    int budget = Configurator.maxBreaksPerTick;
                    while(budget-- > 0 && op == Operation.CLEARING) {
                        op = doClearing(world);
                    }
                    this.cooldownTicks = Configurator.breakCooldownTicks;
                    this.operation = op;
                    return op == Operation.COMPLETE;
                }

                case TICKING: {
                    Operation op = Operation.TICKING;
                    int budget = Configurator.maxBreaksPerTick;
                    while(budget-- > 0 && op == Operation.TICKING) {
                        op = doTicking(world);
                    }
                    this.cooldownTicks = Configurator.breakCooldownTicks;
                    this.operation = op;
                    return op == Operation.COMPLETE;
                }

                case COMPLETE:
                default:
                    return true;
                }
            }

            private Operation startSearch(World world) {
                final long packedPos = this.startPos;
                PackedBlockPos.unpackTo(packedPos, searchPos);

                BlockState state = world.getBlockState(searchPos);

                if(state.getBlock().matches(BlockTags.LOGS)) {
                    this.visited.put(packedPos, POS_TYPE_LOG);

                    // shoudln't really be necessary, but reflect the
                    // reason we are doing this is the block below is (or was) hot lava
                    this.visited.put(PackedBlockPos.down(packedPos), POS_TYPE_IGNORE);

                    this.startState = state;

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
                        // leaves are always valid to visit, even from other leaves
                        this.visited.put(packedPos, POS_TYPE_LEAF);

                        // restart depth of search when transition from log to leaf
                        if(fromType != POS_TYPE_LEAF) newDepth = 0;

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
                    this.visited.long2ByteEntrySet()
                    .stream()
                    .filter(e -> e.getByteValue() != POS_TYPE_IGNORE)
                    .sorted(new  Comparator<Long2ByteMap.Entry>() {

                        @Override
                        public int compare(Entry o1, Entry o2) {
                            // logs before leaves
                            return Byte.compare(o1.getByteValue(), o2.getByteValue());
                        }

                    })
                    .forEach(e -> this.toClear.enqueue(e.getLongKey()));

                    if(this.toClear.isEmpty()) {
                        return Operation.COMPLETE;
                    } else {
                        // prep for leaves
                        toTick.clear();
                        return Operation.CLEARING;
                    }
                } 
                else return Operation.SEARCHING;

            }

            private void enqueIfViable(long packedPos, byte type, byte depth) {
                if(this.visited.containsKey(packedPos)) return;

                if(depth == Byte.MAX_VALUE || depth < 0) return;

                if(type == POS_TYPE_LEAF && depth > 7) return;

                this.toVisit.offer(new Visit(packedPos, type, depth));
            }

            private Operation doClearing(World world) {
                long packedPos = this.toClear.dequeueLong();
                BlockPos pos = PackedBlockPos.unpack(packedPos);
                BlockState state = world.getBlockState(pos);
                Block block = state.getBlock();

                if(block.matches(BlockTags.LOGS)) {
                    breakLog(pos, world);
                } else if(block.matches(BlockTags.LEAVES)) {
                    if(state.get(LeavesBlock.DISTANCE) != 7) {
                        block.onScheduledTick(state, world, pos, null);
                    }
                    // do block ticks in reverse order
                    toTick.enqueueFirst(packedPos);
                }

                return this.toClear.isEmpty() ? Operation.TICKING:  Operation.CLEARING;
            }

            private void breakLog(BlockPos pos, World world) {
                BlockState blockState = world.getBlockState(pos);
                if (blockState.isAir()) {
                    return;
                }
                final FluidState fluidState = world.getFluidState(pos);
                final BlockEntity blockEntity = blockState.getBlock().hasBlockEntity() ? world.getBlockEntity(pos) : null;
                Block.dropStacks(blockState, world, pos, blockEntity);

                world.setBlockState(pos, fluidState.getBlockState(), 3);

                final EffectLevel fxLevel = Configurator.effectLevel;
                if(fxLevel == EffectLevel.ALL || (fxLevel == EffectLevel.SOME && (needsFirstEffect || ThreadLocalRandom.current().nextInt(4) == 0))) {
                    world.playLevelEvent(2001, pos, Block.getRawIdFromState(blockState));
                    needsFirstEffect = false;
                }
            }
            
            private Operation doTicking(World world) {
                if(toTick.isEmpty()) {
                    return Operation.IDLE;
                }

                BlockPos pos = PackedBlockPos.unpack(toTick.dequeueLong());
                BlockState state = world.getBlockState(pos);
                Block block = state.getBlock();

                if(block.matches(BlockTags.LEAVES))
                    block.onRandomTick(state, world, pos, null);

                return Operation.TICKING;
            }
}
