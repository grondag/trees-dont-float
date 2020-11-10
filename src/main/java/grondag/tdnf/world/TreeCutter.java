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

import java.util.Random;
import java.util.function.Predicate;

import grondag.tdnf.Configurator;
import io.netty.util.internal.ThreadLocalRandom;
import it.unimi.dsi.fastutil.longs.Long2IntMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongComparators;
import it.unimi.dsi.fastutil.longs.LongHeapPriorityQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
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
 *
 * Search algorithm:
 * <ul>
 * <li>Flood fill all logs, up to search limit.
 * <li>During search support blocks directly under logs are marked.
 * <li>If any support block is directly attached to the start position, search ends.
 * <ii>"Directly attached" means only directly vertical/sideways connections.
 * <li>If no support blocks are found, all found logs are broken.
 * <li>If support blocks are found not directly attached to origin, search is made from each one...
 * <li>During search, can go up, sideways, diagonal down, or diagonal up.
 * <li>Diagonal can only continue sideways or in same diagonal direction.
 * <li>All blocks marked in this second search are removed from the set.
 * </ul>
 */
public class TreeCutter {
	private Operation operation = Operation.COMPLETE;

	private final LongHeapPriorityQueue toVisit = new LongHeapPriorityQueue();

	/**
	 * Packed positions that have received a valid visit on forward pass.
	 * Values are one of the SEARCH_ constants and when the value indicates a log
	 * the depth of the log is also stored in the upper bits of the value.
	 * This enables sorting of logs by depth order later on.
	 */
	private final Long2IntOpenHashMap forwardVisits = new Long2IntOpenHashMap();

	private static final int VISIT_TYPE_MASK = 0xFF;
	private static final int VISIT_DEPTH_SHIFT = 8;

	/** packed positions that have received a valid visit on leaf pass */
	private final Long2IntOpenHashMap leafVisits = new Long2IntOpenHashMap();

	/** packed positions of supports for inverse search - populated during forward search */
	private final LongArrayFIFOQueue supports = new LongArrayFIFOQueue();

	private final LongOpenHashSet doomed = new LongOpenHashSet();

	/** packed positions of logs to be cleared - populated during pre-clearing */
	private final LongArrayList logs = new LongArrayList();

	private final ObjectArrayList<BlockState> fallingLogStates = new ObjectArrayList<>();

	/** Used to iterate {@link #fallingLogs} */
	private int fallingLogIndex = 0;

	/** leaves to be cleared - populated during pre-clearing */
	private final LongArrayFIFOQueue leaves = new LongArrayFIFOQueue();

	/** general purpose mutable pos */
	private final BlockPos.Mutable searchPos = new BlockPos.Mutable();

	/** iterator traversed during pre-clearing */
	private ObjectIterator<Entry> visitIterator = null;

	private final TreeJob job;

	private final DropHandler dropHandler = new DropHandler();

	private final FxManager fx = new FxManager();

	private int logMask;

	/** Counter for enforcing configured per-second break max */
	private int breakBudget = 0;

	private int xStart = 0;
	private int zStart = 0;
	// all below are used for center-of-mass and fall velocity handling
	private final int LOG_FACTOR = 5; // logs worth this much more than leaves
	private int xSum = 0;
	private int zSum = 0;
	private double xVelocity = 0;
	private double zVelocity = 0;

	private Axis fallAxis = Axis.X;

	// Numeric order here drives priority queue

	/** used in queue but not in visits */
	private static final int SEARCH_LOG_DOWN = 0;
	private static final int SEARCH_LOG = 1;
	/** used in queue but not in visits */
	private static final int SEARCH_LOG_DIAGONAL_DOWN = 2;
	private static final int SEARCH_LOG_DIAGONAL = 3;
	private static final int SEARCH_IGNORE = 4;
	private static final int SEARCH_SUPPORT = 5;
	private static final int SEARCH_LEAF = 6;

	/** default map return value - prevents ambiguity */
	private static final int SEARCH_NOT_PRESENT = Integer.MAX_VALUE;

	//	 All below are for compact representation of reverse search space
	/**
	 * Log (or search position) reached directly via adjacent sides.
	 * Can transmit support in any direction.
	 */
	private static final int REVERSE_LOG = 0;

	/**
	 * Log (or search position) reached sideways diagonally from another
	 * sideways diagonal log or from a directly-connected log.<p>
	 *
	 * Can transmit support only to other diagonal logs.
	 * This means child positions must also be diagonal.
	 */
	private static final int REVERSE_DIAGONAL = 1;

	/**
	 * Log (or search position) reached diagonally up from another
	 * sideways or up diagonal log or from a directly-adjacent log.<p>
	 *
	 * Can transmit support only to other up diagonal logs.
	 * Sideways diagonal positions that originate from an up diagonal
	 * are classified as up diagonals.
	 */
	private static final int REVERSE_DIAGONAL_UP = 2;

	/**
	 * Log (or search position) reached diagonally down from another
	 * sideways or down diagonal log or from a directly-adjacent log.<p>
	 *
	 * Can transmit support only to other down diagonal logs.
	 * Sideways diagonal positions that originate from a down diagonal
	 * are classified as down diagonals.
	 */
	private static final int REVERSE_DIAGONAL_DOWN = 3;

	TreeCutter(TreeJob job) {
		this.job = job;
		forwardVisits.defaultReturnValue(SEARCH_NOT_PRESENT);
	}

	private long packedVisit(long packedPos, int depth, int type) {
		final int x = BlockPos.unpackLongX(packedPos);
		final int y = BlockPos.unpackLongY(packedPos);
		final int z = BlockPos.unpackLongZ(packedPos);

		assert Math.abs(x - xStart) <= 255;
		assert Math.abs(z - zStart) <= 255;

		final int px = (x + 255 - xStart) & 511;
		final int py = y & 0xFF;
		final int pz = (z + 255 - zStart) & 511;

		return px | (py << 9) | (pz << 17) | ((long) depth << 26) | ((long) type << 34);
	}

	private int getVisitPackedDepth(long visit) {
		return (int) ((visit >>> 26) & 0xFF);
	}

	private int getVisitPackedType(long visit) {
		return (int) ((visit >>> 34) & 0xFF);
	}

	private long getVisitPackedPos(long visit) {
		final int v = (int) visit;
		final int x = (v & 511) - 255 + xStart;
		final int y = (v >> 9) & 0xFF;
		final int z = ((v >> 17) & 511) - 255 + zStart;

		return BlockPos.asLong(x, y, z);
	}

	public static BlockPos getPos(int key, BlockPos origin) {
		final int x = (key & 0xFFF) - 2047 + origin.getX();
		final int z = ((key >> 20) & 0xFFF) - 2047 + origin.getZ();
		final int y = (key >> 12) & 0xFF;
		return new BlockPos(x, y, z);
	}

	public void reset() {
		dropHandler.reset(job);
		forwardVisits.clear();
		leafVisits.clear();
		supports.clear();
		doomed.clear();
		toVisit.clear();
		logs.clear();
		fallingLogStates.clear();
		leaves.clear();
		fx.reset();
		xSum = 0;
		zSum = 0;
		xStart = BlockPos.unpackLongX(job.startPos());
		zStart = BlockPos.unpackLongZ(job.startPos());
		visitIterator = null;
		operation = opStartSearch;
		fallingLogIndex = 0;
	}

	public void prepareForTick(ServerWorld world) {
		final int max = Configurator.maxBreaksPerSecond;
		breakBudget += max;
		breakBudget = breakBudget > max ? max : breakBudget;
		fx.prepareForTick();
	}

	public void tick(ServerWorld world) {
		// if we have to end early, at least spawn drops
		if(job.isCancelled(world) || job.isTimedOut()) {
			dropHandler.spawnDrops(world);
			operation = Operation.COMPLETE;
		} else {
			int i = 0;

			do {
				operation = operation.apply(world);
			} while (++i <= 8 && canRun() && operation != Operation.COMPLETE);
		}
	}

	public boolean canRun() {
		return breakBudget > 0;
	}

	public boolean isComplete() {
		return operation == Operation.COMPLETE;
	}

	private final Operation opStartSearch = this::startSearch;

	private Operation startSearch(World world) {
		final long packedPos = job.startPos();
		searchPos.set(packedPos);
		final BlockState state = world.getBlockState(searchPos);

		final int logType = TreeBlock.getType(state);

		if ((logType & TreeBlock.LOG_MASK) != 0) {
			logMask = logType == TreeBlock.LOG ? TreeBlock.LOG : (Configurator.breakFungalLeaves ? TreeBlock.FUNGUS_MASK : TreeBlock.FUNGUS_LOG);
			//            this.startState = state;
			//            this.startBlock = state.getBlock();

			// don't need to mix in depth because will be zero
			forwardVisits.put(packedPos, SEARCH_LOG);

			// shoudln't really be necessary, but reflect the
			// reason we are doing this is the block below is (or was) non-supporting
			forwardVisits.put(BlockPos.add(packedPos, 0, -1, 0), SEARCH_IGNORE);

			enqueForwardIfViable(BlockPos.add(packedPos, 0, 1, 0), SEARCH_LOG, 0);
			enqueForwardIfViable(BlockPos.add(packedPos, -1, 0, 0), SEARCH_LOG, 0);
			enqueForwardIfViable(BlockPos.add(packedPos, 1, 0, 0), SEARCH_LOG, 0);
			enqueForwardIfViable(BlockPos.add(packedPos, 0, 0, -1), SEARCH_LOG, 0);
			enqueForwardIfViable(BlockPos.add(packedPos, 0, 0, 1), SEARCH_LOG, 0);

			enqueForwardIfViable(BlockPos.add(packedPos, -1, 0, -1), SEARCH_LOG_DIAGONAL, 0);
			enqueForwardIfViable(BlockPos.add(packedPos, -1, 0, 1), SEARCH_LOG_DIAGONAL, 0);
			enqueForwardIfViable(BlockPos.add(packedPos, 1, 0, -1), SEARCH_LOG_DIAGONAL, 0);
			enqueForwardIfViable(BlockPos.add(packedPos, 1, 0, 1), SEARCH_LOG_DIAGONAL, 0);

			enqueForwardIfViable(BlockPos.add(packedPos, -1, 1, -1), SEARCH_LOG_DIAGONAL, 0);
			enqueForwardIfViable(BlockPos.add(packedPos, -1, 1, 0), SEARCH_LOG_DIAGONAL, 0);
			enqueForwardIfViable(BlockPos.add(packedPos, -1, 1, 1), SEARCH_LOG_DIAGONAL, 0);
			enqueForwardIfViable(BlockPos.add(packedPos, 0, 1, -1), SEARCH_LOG_DIAGONAL, 0);
			enqueForwardIfViable(BlockPos.add(packedPos, 0, 1, 1), SEARCH_LOG_DIAGONAL, 0);
			enqueForwardIfViable(BlockPos.add(packedPos, 1, 1, -1), SEARCH_LOG_DIAGONAL, 0);
			enqueForwardIfViable(BlockPos.add(packedPos, 1, 1, 0), SEARCH_LOG_DIAGONAL, 0);
			enqueForwardIfViable(BlockPos.add(packedPos, 1, 1, 1), SEARCH_LOG_DIAGONAL, 0);
			return opForwardSearch;
		} else {
			return Operation.COMPLETE;
		}
	}

	private final Operation opForwardSearch = this::forwardSearch;

	private Operation forwardSearch(World world) {
		final long toVisit = this.toVisit.dequeueLong();

		final long packedPos = getVisitPackedPos(toVisit);

		searchPos.set(packedPos);

		final int searchType = getVisitPackedType(toVisit);

		final int newDepth = getVisitPackedDepth(toVisit) + 1;

		if (!forwardVisits.containsKey(packedPos)) {
			final BlockState state = world.getBlockState(searchPos);

			if ((TreeBlock.getType(state) & logMask) != 0) {
				assert searchType == SEARCH_LOG_DOWN || searchType == SEARCH_LOG || searchType == SEARCH_LOG_DIAGONAL || searchType == SEARCH_LOG_DIAGONAL_DOWN;
				final boolean diagonal = searchType == SEARCH_LOG_DIAGONAL || searchType == SEARCH_LOG_DIAGONAL_DOWN;
				forwardVisits.put(packedPos, (diagonal ? SEARCH_LOG_DIAGONAL : SEARCH_LOG) | (newDepth << VISIT_DEPTH_SHIFT));

				if (diagonal) {
					enqueForwardIfViable(BlockPos.add(packedPos, 0, -1, 0), SEARCH_LOG_DIAGONAL_DOWN, newDepth);
				} else {
					enqueForwardIfViable(BlockPos.add(packedPos, 0, -1, 0), SEARCH_LOG_DOWN, newDepth);
				}

				final int newDirectType = diagonal ? SEARCH_LOG_DIAGONAL : SEARCH_LOG;

				enqueForwardIfViable(BlockPos.add(packedPos, 0, 1, 0), newDirectType, newDepth);
				enqueForwardIfViable(BlockPos.add(packedPos, -1, 0, 0), newDirectType, newDepth);
				enqueForwardIfViable(BlockPos.add(packedPos, 1, 0, 0), newDirectType, newDepth);
				enqueForwardIfViable(BlockPos.add(packedPos, 0, 0, -1), newDirectType, newDepth);
				enqueForwardIfViable(BlockPos.add(packedPos, 0, 0, 1), newDirectType, newDepth);

				enqueForwardIfViable(BlockPos.add(packedPos, -1, 0, -1), SEARCH_LOG_DIAGONAL, newDepth);
				enqueForwardIfViable(BlockPos.add(packedPos, -1, 0, 1), SEARCH_LOG_DIAGONAL, newDepth);
				enqueForwardIfViable(BlockPos.add(packedPos, 1, 0, -1), SEARCH_LOG_DIAGONAL, newDepth);
				enqueForwardIfViable(BlockPos.add(packedPos, 1, 0, 1), SEARCH_LOG_DIAGONAL, newDepth);

				enqueForwardIfViable(BlockPos.add(packedPos, -1, 1, -1), SEARCH_LOG_DIAGONAL, newDepth);
				enqueForwardIfViable(BlockPos.add(packedPos, -1, 1, 0), SEARCH_LOG_DIAGONAL, newDepth);
				enqueForwardIfViable(BlockPos.add(packedPos, -1, 1, 1), SEARCH_LOG_DIAGONAL, newDepth);
				enqueForwardIfViable(BlockPos.add(packedPos, 0, 1, -1), SEARCH_LOG_DIAGONAL, newDepth);
				enqueForwardIfViable(BlockPos.add(packedPos, 0, 1, 1), SEARCH_LOG_DIAGONAL, newDepth);
				enqueForwardIfViable(BlockPos.add(packedPos, 1, 1, -1), SEARCH_LOG_DIAGONAL, newDepth);
				enqueForwardIfViable(BlockPos.add(packedPos, 1, 1, 0), SEARCH_LOG_DIAGONAL, newDepth);
				enqueForwardIfViable(BlockPos.add(packedPos, 1, 1, 1), SEARCH_LOG_DIAGONAL, newDepth);

				enqueForwardIfViable(BlockPos.add(packedPos, -1, -1, -1), SEARCH_LOG_DIAGONAL, newDepth);
				enqueForwardIfViable(BlockPos.add(packedPos, -1, -1, 0), SEARCH_LOG_DIAGONAL, newDepth);
				enqueForwardIfViable(BlockPos.add(packedPos, -1, -1, 1), SEARCH_LOG_DIAGONAL, newDepth);
				enqueForwardIfViable(BlockPos.add(packedPos, 0, -1, -1), SEARCH_LOG_DIAGONAL, newDepth);
				enqueForwardIfViable(BlockPos.add(packedPos, 0, -1, 1), SEARCH_LOG_DIAGONAL, newDepth);
				enqueForwardIfViable(BlockPos.add(packedPos, 1, -1, -1), SEARCH_LOG_DIAGONAL, newDepth);
				enqueForwardIfViable(BlockPos.add(packedPos, 1, -1, 0), SEARCH_LOG_DIAGONAL, newDepth);
				enqueForwardIfViable(BlockPos.add(packedPos, 1, -1, 1), SEARCH_LOG_DIAGONAL, newDepth);
			} else if (state.getBlock().isIn(BlockTags.LEAVES)) {
				forwardVisits.put(packedPos, SEARCH_IGNORE);
			} else {
				if (searchType == SEARCH_LOG_DOWN) {
					// if found a supporting block for a directly connected log
					// then tree remains standing
					if (Block.isFaceFullSquare(state.getCollisionShape(world, searchPos, ShapeContext.absent()), Direction.UP)) {
						return Operation.COMPLETE;
					} else {
						forwardVisits.put(packedPos, SEARCH_IGNORE);
					}
				} else if (searchType == SEARCH_LOG_DIAGONAL_DOWN) {
					// if found a supporting block for a diagonally connected log
					// then record it for later reverse search
					if (Block.isFaceFullSquare(state.getCollisionShape(world, searchPos, ShapeContext.absent()), Direction.UP)) {
						forwardVisits.put(packedPos, SEARCH_SUPPORT);
						supports.enqueue(packedVisit(BlockPos.add(packedPos, 0, 1, 0), 0, REVERSE_LOG));
					} else {
						forwardVisits.put(packedPos, SEARCH_IGNORE);
					}
				} else {
					forwardVisits.put(packedPos, SEARCH_IGNORE);
				}
			}
		}

		if (this.toVisit.isEmpty()) {
			if (supports.isEmpty()) {
				return opPreProcessLogs1;
			} else {
				return opReverseSearch;
			}
		} else {
			return opForwardSearch;
		}
	}

	private void enqueForwardIfViable(long packedPos, int type, int depth) {
		if (forwardVisits.containsKey(packedPos)) {
			return;
		}

		if (depth >= 127 || depth < 0) {
			return;
		}

		toVisit.enqueue(packedVisit(packedPos, depth, type));
	}

	private final Operation opReverseSearch = this::reverseSearch;

	private boolean canReverseSearchRemove(long packedPos) {
		// must be a log at search position for us to do anything
		// log may have been removed by an earlier iteration
		// never remove directly connected logs
		return (forwardVisits.get(packedPos) & VISIT_TYPE_MASK) == SEARCH_LOG_DIAGONAL;
	}

	/**
	 * Searches backwards from supports and removes logs with support and weak connections to break start.
	 */
	private Operation reverseSearch(World world) {
		long visit;

		if (toVisit.isEmpty()) {
			if (supports.isEmpty()) {
				return opPreProcessLogs1;
			} else {
				visit = supports.dequeueLong();
			}
		} else {
			visit = toVisit.dequeueLong();
		}

		final long packedPos = getVisitPackedPos(visit);
		final int searchType = getVisitPackedType(visit);

		if (canReverseSearchRemove(packedPos)) {
			forwardVisits.remove(packedPos);

			enqueReverseIfViable(BlockPos.add(packedPos, 0, 1, 0), searchType);
			enqueReverseIfViable(BlockPos.add(packedPos, -1, 0, 0), searchType);
			enqueReverseIfViable(BlockPos.add(packedPos, 1, 0, 0), searchType);
			enqueReverseIfViable(BlockPos.add(packedPos, 0, 0, -1), searchType);
			enqueReverseIfViable(BlockPos.add(packedPos, 0, 0, 1), searchType);

			final int newType = searchType == REVERSE_LOG ? REVERSE_DIAGONAL : searchType;

			enqueReverseIfViable(BlockPos.add(packedPos, -1, 0, -1), newType);
			enqueReverseIfViable(BlockPos.add(packedPos, -1, 0, 1), newType);
			enqueReverseIfViable(BlockPos.add(packedPos, 1, 0, -1), newType);
			enqueReverseIfViable(BlockPos.add(packedPos, 1, 0, 1), newType);

			if (searchType != REVERSE_DIAGONAL_DOWN) {
				enqueReverseIfViable(BlockPos.add(packedPos, -1, 1, -1), REVERSE_DIAGONAL_UP);
				enqueReverseIfViable(BlockPos.add(packedPos, -1, 1, 0), REVERSE_DIAGONAL_UP);
				enqueReverseIfViable(BlockPos.add(packedPos, -1, 1, 1), REVERSE_DIAGONAL_UP);
				enqueReverseIfViable(BlockPos.add(packedPos, 0, 1, -1), REVERSE_DIAGONAL_UP);
				enqueReverseIfViable(BlockPos.add(packedPos, 0, 1, 1), REVERSE_DIAGONAL_UP);
				enqueReverseIfViable(BlockPos.add(packedPos, 1, 1, -1), REVERSE_DIAGONAL_UP);
				enqueReverseIfViable(BlockPos.add(packedPos, 1, 1, 0), REVERSE_DIAGONAL_UP);
				enqueReverseIfViable(BlockPos.add(packedPos, 1, 1, 1), REVERSE_DIAGONAL_UP);
			}

			if (searchType != REVERSE_DIAGONAL_UP) {
				enqueReverseIfViable(BlockPos.add(packedPos, -1, -1, -1), REVERSE_DIAGONAL_DOWN);
				enqueReverseIfViable(BlockPos.add(packedPos, -1, -1, 0), REVERSE_DIAGONAL_DOWN);
				enqueReverseIfViable(BlockPos.add(packedPos, -1, -1, 1), REVERSE_DIAGONAL_DOWN);
				enqueReverseIfViable(BlockPos.add(packedPos, 0, -1, -1), REVERSE_DIAGONAL_DOWN);
				enqueReverseIfViable(BlockPos.add(packedPos, 0, -1, 1), REVERSE_DIAGONAL_DOWN);
				enqueReverseIfViable(BlockPos.add(packedPos, 1, -1, -1), REVERSE_DIAGONAL_DOWN);
				enqueReverseIfViable(BlockPos.add(packedPos, 1, -1, 0), REVERSE_DIAGONAL_DOWN);
				enqueReverseIfViable(BlockPos.add(packedPos, 1, -1, 1), REVERSE_DIAGONAL_DOWN);
			}
		}

		return opReverseSearch;
	}

	private void enqueReverseIfViable(long packedPos, int type) {
		if (canReverseSearchRemove(packedPos)) {
			toVisit.enqueue(packedVisit(packedPos, 0, type));
		}
	}

	private final Operation opPreProcessLogs1 = this::preProcessLogs1;

	/**
	 * Adds logs to doomed/log collection and enqueues adjacent spaces for leaf search.
	 */
	private Operation preProcessLogs1(World world) {
		final ObjectIterator<Entry> it = forwardVisits.long2IntEntrySet().iterator();

		while (it.hasNext()) {
			final Entry e = it.next();
			final int packedType = e.getIntValue();
			final int type = packedType & VISIT_TYPE_MASK;

			if (type != SEARCH_IGNORE && type != SEARCH_SUPPORT) {
				logs.add(packedVisit(e.getLongKey(), packedType >>> VISIT_DEPTH_SHIFT, 0));
			}
		}

		if (logs.isEmpty()) {
			return Operation.COMPLETE;
		} else {
			logs.sort(LongComparators.NATURAL_COMPARATOR);
			return opPreProcessLogs2;
		}
	}

	private final Operation opPreProcessLogs2 = this::preProcessLogs2;

	/**
	 * Adds logs to doomed/log collection and enqueues adjacent spaces for leaf search.
	 */
	private Operation preProcessLogs2(World world) {
		// trim logs to size
		int excess = logs.size() - computeLogLimit(world);

		while (excess-- > 0) {
			logs.popLong();
		}

		final int limit = logs.size();

		if (limit == 0) {
			return Operation.COMPLETE;
		}

		for (int i = 0; i < limit; ++i) {
			// replace relative-packed with depth used for sorting with full packed pos
			final long packed = logs.getLong(i);
			final long packedPos = getVisitPackedPos(packed);
			logs.set(i, packedPos);

			if (Configurator.keepLogsIntact) {
				xSum += (BlockPos.unpackLongX(packedPos) - xStart) * LOG_FACTOR;
				zSum += (BlockPos.unpackLongZ(packedPos) - zStart) * LOG_FACTOR;
			}

			leafVisits.put(packedPos, SEARCH_LOG);
			doomed.add(packedPos);
		}

		// sort logs bottom-up for falling purposes
		logs.sort((l0, l1) -> Integer.compare(BlockPos.unpackLongY(l1), BlockPos.unpackLongY(l0)));
		visitIterator = leafVisits.long2IntEntrySet().iterator();
		return opFindLeavesPre;
	}

	private final Operation opFindLeavesPre = this::findLeavesPre;

	private Operation findLeavesPre(World world) {
		final ObjectIterator<Entry> it = visitIterator;

		if (it.hasNext()) {
			final long packedPos = it.next().getLongKey();

			enqueLeafIfViable(BlockPos.add(packedPos, 0, 1, 0), SEARCH_LOG, 1);
			enqueLeafIfViable(BlockPos.add(packedPos, 0, -1, 0), SEARCH_LOG, 1);
			enqueLeafIfViable(BlockPos.add(packedPos, -1, 0, 0), SEARCH_LOG, 1);
			enqueLeafIfViable(BlockPos.add(packedPos, 1, 0, 0), SEARCH_LOG, 1);
			enqueLeafIfViable(BlockPos.add(packedPos, 0, 0, -1), SEARCH_LOG, 1);
			enqueLeafIfViable(BlockPos.add(packedPos, 0, 0, 1), SEARCH_LOG, 1);

			enqueLeafIfViable(BlockPos.add(packedPos, -1, 0, -1), SEARCH_LOG, 1);
			enqueLeafIfViable(BlockPos.add(packedPos, -1, 0, 1), SEARCH_LOG, 1);
			enqueLeafIfViable(BlockPos.add(packedPos, 1, 0, -1), SEARCH_LOG, 1);
			enqueLeafIfViable(BlockPos.add(packedPos, 1, 0, 1), SEARCH_LOG, 1);

			enqueLeafIfViable(BlockPos.add(packedPos, -1, 1, -1), SEARCH_LOG, 1);
			enqueLeafIfViable(BlockPos.add(packedPos, -1, 1, 0), SEARCH_LOG, 1);
			enqueLeafIfViable(BlockPos.add(packedPos, -1, 1, 1), SEARCH_LOG, 1);
			enqueLeafIfViable(BlockPos.add(packedPos, 0, 1, -1), SEARCH_LOG, 1);
			enqueLeafIfViable(BlockPos.add(packedPos, 0, 1, 1), SEARCH_LOG, 1);
			enqueLeafIfViable(BlockPos.add(packedPos, 1, 1, -1), SEARCH_LOG, 1);
			enqueLeafIfViable(BlockPos.add(packedPos, 1, 1, 0), SEARCH_LOG, 1);
			enqueLeafIfViable(BlockPos.add(packedPos, 1, 1, 1), SEARCH_LOG, 1);

			enqueLeafIfViable(BlockPos.add(packedPos, -1, -1, -1), SEARCH_LOG, 1);
			enqueLeafIfViable(BlockPos.add(packedPos, -1, -1, 0), SEARCH_LOG, 1);
			enqueLeafIfViable(BlockPos.add(packedPos, -1, -1, 1), SEARCH_LOG, 1);
			enqueLeafIfViable(BlockPos.add(packedPos, 0, -1, -1), SEARCH_LOG, 1);
			enqueLeafIfViable(BlockPos.add(packedPos, 0, -1, 1), SEARCH_LOG, 1);
			enqueLeafIfViable(BlockPos.add(packedPos, 1, -1, -1), SEARCH_LOG, 1);
			enqueLeafIfViable(BlockPos.add(packedPos, 1, -1, 0), SEARCH_LOG, 1);
			enqueLeafIfViable(BlockPos.add(packedPos, 1, -1, 1), SEARCH_LOG, 1);

			return opFindLeavesPre;
		} else {
			return opFindLeaves;
		}
	}

	private final Operation opFindLeaves = this::findLeaves;

	private Operation findLeaves(World world) {
		final long toVisit = this.toVisit.dequeueLong();
		final long packedPos = getVisitPackedPos(toVisit);
		searchPos.set(packedPos);
		int expectedDepth = getVisitPackedDepth(toVisit);

		if (!leafVisits.containsKey(packedPos)) {
			final BlockState state = world.getBlockState(searchPos);
			final Block block = state.getBlock();
			final int searchType = getVisitPackedType(toVisit);

			if (block.isIn(BlockTags.LEAVES)) {
				final LeafInfo inf = LeafInfo.get(block);
				final int actualDepth = inf.applyAsInt(state);

				// Leaves directly adjacent always included
				// Otherwise must have expected distance
				if (searchType == SEARCH_LOG || actualDepth == Math.min(inf.maxDistance, expectedDepth)) {
					leafVisits.put(packedPos, SEARCH_LEAF);

					if (Configurator.keepLogsIntact) {
						xSum += (BlockPos.unpackLongX(packedPos) - xStart);
						zSum += (BlockPos.unpackLongZ(packedPos) - zStart);
						leaves.enqueue(packedPos);
					} else if (Configurator.fastLeafDecay) {
						leaves.enqueue(packedPos);
					}

					expectedDepth = Math.min(inf.maxDistance, actualDepth + 1);
					enqueLeafIfViable(BlockPos.add(packedPos, 0, -1, 0), SEARCH_LEAF, expectedDepth);
					enqueLeafIfViable(BlockPos.add(packedPos, 0, 1, 0), SEARCH_LEAF, expectedDepth);
					enqueLeafIfViable(BlockPos.add(packedPos, -1, 0, 0), SEARCH_LEAF, expectedDepth);
					enqueLeafIfViable(BlockPos.add(packedPos, 1, 0, 0), SEARCH_LEAF, expectedDepth);
					enqueLeafIfViable(BlockPos.add(packedPos, 0, 0, -1), SEARCH_LEAF, expectedDepth);
					enqueLeafIfViable(BlockPos.add(packedPos, 0, 0, 1), SEARCH_LEAF, expectedDepth);

					// diagonals are one more - Manhattan distance
					expectedDepth = Math.min(inf.maxDistance, expectedDepth + 1);
					enqueLeafIfViable(BlockPos.add(packedPos, -1, 0, -1), SEARCH_LEAF, expectedDepth);
					enqueLeafIfViable(BlockPos.add(packedPos, -1, 0, 1), SEARCH_LEAF, expectedDepth);
					enqueLeafIfViable(BlockPos.add(packedPos, 1, 0, -1), SEARCH_LEAF, expectedDepth);
					enqueLeafIfViable(BlockPos.add(packedPos, 1, 0, 1), SEARCH_LEAF, expectedDepth);

					enqueLeafIfViable(BlockPos.add(packedPos, -1, 1, -1), SEARCH_LEAF, expectedDepth);
					enqueLeafIfViable(BlockPos.add(packedPos, -1, 1, 0), SEARCH_LEAF, expectedDepth);
					enqueLeafIfViable(BlockPos.add(packedPos, -1, 1, 1), SEARCH_LEAF, expectedDepth);
					enqueLeafIfViable(BlockPos.add(packedPos, 0, 1, -1), SEARCH_LEAF, expectedDepth);
					enqueLeafIfViable(BlockPos.add(packedPos, 0, 1, 1), SEARCH_LEAF, expectedDepth);
					enqueLeafIfViable(BlockPos.add(packedPos, 1, 1, -1), SEARCH_LEAF, expectedDepth);
					enqueLeafIfViable(BlockPos.add(packedPos, 1, 1, 0), SEARCH_LEAF, expectedDepth);
					enqueLeafIfViable(BlockPos.add(packedPos, 1, 1, 1), SEARCH_LEAF, expectedDepth);

					enqueLeafIfViable(BlockPos.add(packedPos, -1, -1, -1), SEARCH_LEAF, expectedDepth);
					enqueLeafIfViable(BlockPos.add(packedPos, -1, -1, 0), SEARCH_LEAF, expectedDepth);
					enqueLeafIfViable(BlockPos.add(packedPos, -1, -1, 1), SEARCH_LEAF, expectedDepth);
					enqueLeafIfViable(BlockPos.add(packedPos, 0, -1, -1), SEARCH_LEAF, expectedDepth);
					enqueLeafIfViable(BlockPos.add(packedPos, 0, -1, 1), SEARCH_LEAF, expectedDepth);
					enqueLeafIfViable(BlockPos.add(packedPos, 1, -1, -1), SEARCH_LEAF, expectedDepth);
					enqueLeafIfViable(BlockPos.add(packedPos, 1, -1, 0), SEARCH_LEAF, expectedDepth);
					enqueLeafIfViable(BlockPos.add(packedPos, 1, -1, 1), SEARCH_LEAF, expectedDepth);
				}

				// Don't ignore leaves if distance doesn't match - may be matched via a different path
			} else {
				leafVisits.put(packedPos, SEARCH_IGNORE);
			}
		}

		if (this.toVisit.isEmpty()) {
			return opDoPreClearing;
		} else {
			return opFindLeaves;
		}
	}

	private void enqueLeafIfViable(long packedPos, int type, int depth) {
		if (leafVisits.containsKey(packedPos)) {
			return;
		}

		if (depth >= 127 || depth < 0) {
			return;
		}

		toVisit.enqueue(packedVisit(packedPos, depth, type));
	}

	private final Operation opDoPreClearing = this::doPreClearing;

	private Operation doPreClearing(World world) {
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

		if (Configurator.keepLogsIntact) {
			final double div = logs.size() * LOG_FACTOR + leaves.size();
			final double xCenterOfMass = xStart + xSum / div;
			final double zCenterOfMass = zStart + zSum / div;
			final Random r = ThreadLocalRandom.current();

			xVelocity = xCenterOfMass - xStart;
			zVelocity = zCenterOfMass - zStart;

			if (xVelocity == 0 && zVelocity == 0) {
				xVelocity = r.nextGaussian();
				zVelocity = r.nextGaussian();
				fallAxis = Axis.Y;
			} else {
				fallAxis = Math.abs(xVelocity) > Math.abs(zVelocity) ? Axis.X : Axis.Z;
			}

			// normalize
			final double len = Math.sqrt(xVelocity * xVelocity + zVelocity * zVelocity);
			xVelocity /= len;
			zVelocity /= len;

			// first pass for entities is top to bottom, positive index good for building state list
			fallingLogIndex = 0;

			return logs.isEmpty() ? dropHandler.opDoDrops : opDoLogDropping1;
		} else {
			return opDoLogClearing;
		}
	}

	private final Operation opDoLeafClearing = this::doLeafClearing;

	private Operation doLeafClearing(ServerWorld world) {
		if(leaves.isEmpty()) {
			return dropHandler.opDoDrops;
		}

		final long packedPos = leaves.dequeueLong();
		final BlockPos pos = searchPos.set(packedPos);
		final BlockState state = world.getBlockState(pos);
		final Block block = state.getBlock();

		if (BlockTags.LEAVES.contains(block)) {
			if(!Configurator.leafDurability || checkDurability(world, state, pos)) {
				breakBlock(pos, world);
				breakBudget -= 20;
			} else {
				return dropHandler.opDoDrops;
			}
		}

		return opDoLeafClearing;
	}

	private final Operation opDoLogClearing = this::doLogClearing;

	private Operation doLogClearing(ServerWorld world) {
		final long packedPos = logs.popLong();
		final BlockPos pos = searchPos.set(packedPos);
		final BlockState state = world.getBlockState(pos);

		if ((TreeBlock.getType(state) & logMask) != 0) {
			if(checkDurability(world, state, pos)) {
				breakBudget -= 20;
				breakBlock(pos, world);
			} else {
				return dropHandler.opDoDrops;
			}
		}

		if(logs.isEmpty()) {
			// drop leaves now in case player doesn't want to wait for logs
			dropHandler.spawnDrops(world);
			return opDoLeafClearing;
		} else {
			return opDoLogClearing;
		}
	}

	private final Predicate<BlockPos> suspender = p -> doomed.contains(p.asLong());

	private void breakBlock(BlockPos pos, ServerWorld world) {
		final BlockState blockState = world.getBlockState(pos);
		final Block block = blockState.getBlock();
		final boolean isLeaf = BlockTags.LEAVES.contains(block);

		if ((TreeBlock.getType(blockState) & logMask) == 0 && !isLeaf) {
			// notify fx to increase chance because chance is based on totals reported earlier
			fx.request(false);
			return;
		}

		final FluidState fluidState = world.getFluidState(pos);
		final BlockEntity blockEntity = block.hasBlockEntity() ? world.getBlockEntity(pos) : null;

		dropHandler.doDrops(blockState, world, pos, blockEntity);
		Dispatcher.suspend(suspender);
		world.setBlockState(pos, fluidState.getBlockState(), 3);
		Dispatcher.resume();

		if (fx.request(true)) {
			world.syncWorldEvent(2001, pos, Block.getRawIdFromState(blockState));
		}

		applyHunger(isLeaf, block);
	}

	private boolean checkDurability(World world, BlockState state, BlockPos pos) {
		if (Configurator.consumeDurability && job.hasAxe() && !job.player().isCreative()) {
			final ItemStack stack = job.stack();

			if (Configurator.protectTools && stack.getDamage() >= stack.getMaxDamage() - 2) {
				return false;
			}

			stack.getItem().postMine(stack, world, state, pos, job.player());
			return true;
		} else {
			return true;
		}
	}

	private void applyHunger(boolean isLeaf, Block block) {
		if (Configurator.applyHunger && (!isLeaf || Configurator.leafHunger)) {
			final ServerPlayerEntity player = job.player();

			if (player != null && !player.isCreative()) {
				player.addExhaustion(0.005F);
				player.incrementStat(Stats.MINED.getOrCreateStat(block));
			}
		}
	}

	private final Operation opDoLogDropping1 = this::doLogDropping1;

	//TODO: implement block break limits for falling logs
	private Operation doLogDropping1(World world) {
		final int i = fallingLogIndex++;

		if (i >= logs.size()) {
			// second pass is bottom to top - negative index iteration avoids array re-shuffle each pass
			fallingLogIndex = logs.size();

			//FIXME: not in right place - what about logs partially completed - can abort then? See below also
			// logs are all removed so should not stop after this
			job.disableCancel();
			return opDoLogDropping2;
		}

		final long packedPos = logs.getLong(i);
		final BlockPos pos = searchPos.set(packedPos);
		final BlockState state = world.getBlockState(pos);
		fallingLogStates.add(state);

		if(checkDurability(world, state, pos)) {
			applyHunger(false, state.getBlock());
			world.setBlockState(pos, Blocks.AIR.getDefaultState());

			breakBudget -= 20;
			return opDoLogDropping1;
		} else {
			return dropHandler.opDoDrops;
		}
	}

	private final Operation opDoLogDropping2 = this::doLogDropping2;

	private Operation doLogDropping2(ServerWorld world) {
		int i = --fallingLogIndex;

		if (i < 0) {
			// drop leaves now in case player doesn't want to wait for logs
			dropHandler.spawnDrops(world);
			return opDoLeafClearing;
		}

		if (job.isTimedOut()) {
			while(i >= 0) {
				final long packedPos = logs.getLong(i);
				final BlockPos pos = searchPos.set(packedPos);
				BlockState state = fallingLogStates.get(i);

				if(state.contains(PillarBlock.AXIS)) {
					state = state.with(PillarBlock.AXIS, fallAxis);
				}

				dropHandler.doDrops(state, world, pos, null);
				--i;
			}

			return this::doLeafClearing;
		} else {
			if(FallingLogEntity.canSpawn()) {
				final long packedPos = logs.getLong(i);
				final BlockPos pos = searchPos.set(packedPos);
				BlockState state = fallingLogStates.get(i);

				if(state.contains(PillarBlock.AXIS)) {
					state = state.with(PillarBlock.AXIS, fallAxis);
				}

				final FallingLogEntity entity = new FallingLogEntity(world, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, state);
				final double height = Math.sqrt(Math.max(0, pos.getY() - BlockPos.unpackLongY(job.startPos()))) * 0.2;
				entity.addVelocity(xVelocity * height, 0, zVelocity * height);
				world.spawnEntity(entity);
			} else {
				// force exit till next tick
				breakBudget = breakBudget > 0 ? 0 : breakBudget;
			}

			return opDoLogDropping2;
		}
	}

	/** how many logs player can break - used to implement configured limits */
	private int computeLogLimit(World world) {
		if (job.player() == null) {
			return Configurator.nonPlayerLogLimit;
		} else {
			int result = Configurator.playerBaseLogLimit;

			if (job.hasAxe()) {
				final ItemStack stack = job.stack();
				final Item item = stack.getItem();

				if (item instanceof ToolItem) {
					final int tier = 1 + ((ToolItem) item).getMaterial().getMiningLevel();
					result += tier * Configurator.toolTierLogBonus;
				}

				if (Configurator.enableEfficiencyLogMultiplier) {
					final int enchant = EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, stack);
					result *= (enchant + 1);
				}
			}

			return result;
		}
	}
}
