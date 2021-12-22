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

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

import grondag.tdnf.Platform;
import grondag.tdnf.TreesDoNotFloat;
import grondag.tdnf.config.Configurator;

/**
 * Hacked-up variant of FallingBlock.
 */
@SuppressWarnings("unused")
public abstract class BaseFallingLogEntity extends FallingBlockEntity {
	public static final ResourceLocation IDENTIFIER = new ResourceLocation(TreesDoNotFloat.MODID, "falling_log");
	static final boolean DEBUG = false;

	public BaseFallingLogEntity(EntityType<? extends BaseFallingLogEntity> entityType, Level world) {
		super(entityType, world);

		if (!world.isClientSide) {
			entityCount++;

			if (DEBUG) {
				TreesDoNotFloat.LOG.info("Created new log entity without position or block state. New entity count is " + entityCount);
			}
		}

		blockState = Blocks.OAK_LOG.defaultBlockState();
	}

	public BaseFallingLogEntity(Level world, double x, double y, double z, BlockState state) {
		this(Platform.fallingLogEntityType(), world);
		blockState = state;
		blocksBuilding = true;
		setPos(x, y + (1.0F - getBbHeight()) / 2.0F, z);
		setDeltaMovement(Vec3.ZERO);
		xo = x;
		yo = y;
		zo = z;
		setStartPos(blockPosition());

		if (DEBUG && !world.isClientSide) {
			TreesDoNotFloat.LOG.info("Created new log entity " + this.getId() + " @ " + blockPosition().toString() + " with block state " + state + ". New entity count is " + entityCount);
		}
	}

	@Override
	public void tick() {
		if (blockState.isAir()) {
			discard();

			if (DEBUG && !this.level.isClientSide) {
				TreesDoNotFloat.LOG.info("Discarding log entity " + this.getId() + " because block state is air.");
			}

			return;
		}

		// This was in an earlier version but now we never use the origin and don't need to update it.
		// Vanilla falling block do a clip test from the origin every tick.
		// Presumably if a piston or something else blocks the path before it
		// falls then it will essentially back up and get stopped there.
		//xo = getX();
		//yo = getY();
		//zo = getZ();

		time++;

		final BlockPos myPosPreMove = blockPosition();

		final boolean inWorldRange = myPosPreMove.getY() >= level.getMinBuildHeight() && myPosPreMove.getY() <= level.getMaxBuildHeight();

		if (time > 200 || !inWorldRange) {
			spawnAtLocation(blockState.getBlock());
			discard();

			if (DEBUG && !this.level.isClientSide) {
				TreesDoNotFloat.LOG.info("Discarding log entity " + this.getId() + " after spawning drops because time expired or not in world range.");
			}
		} else {
			destroyCollidingDisplaceableBlocks();

			setDeltaMovement(getDeltaMovement().add(0.0D, -0.04D, 0.0D));

			move(MoverType.SELF, getDeltaMovement());

			if (onGround || verticalCollision) {
				final BlockPos myPos = blockPosition();
				final BlockPos downPos = myPos.below(1);
				final BlockState downBlockState = level.getBlockState(downPos);
				final boolean canFall = (downBlockState.canBeReplaced(new DirectionalPlaceContext(level, downPos, Direction.DOWN, ItemStack.EMPTY, Direction.UP)) && blockState.canSurvive(level, downPos));

				// If nearest block pos lets us fall through, then move to it so that we can.
				if (canFall) {
					onGround = false;
					verticalCollision = false;
					setPos(myPos.getX() + 0.5, myPos.getY() - 0.04D, myPos.getZ() + 0.5);
					setDeltaMovement(0, -0.04D, 0);
				} else {
					if (DEBUG && !this.level.isClientSide) {
						TreesDoNotFloat.LOG.info("Detected unable to fall post-move.");
					}

					spawnAndDiscard(myPos, downPos, downBlockState);
				}
			} else {
				setDeltaMovement(getDeltaMovement().scale(0.98D));
			}
		}
	}

	private void spawnAndDiscard(BlockPos atPos, BlockPos belowPos, BlockState belowState) {
		if (!level.isClientSide && level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
			final BlockState localBlockState = level.getBlockState(atPos);

			if (localBlockState.canBeReplaced(new DirectionalPlaceContext(level, atPos, Direction.DOWN, ItemStack.EMPTY, Direction.UP))
					&& belowState.isFaceSturdy(level, belowPos, Direction.UP, SupportType.CENTER)
					&& level.isUnobstructed(this, Shapes.block().move(atPos.getX(), atPos.getY(), atPos.getZ()))
			) {
				if (level.setBlock(atPos, blockState, 3)) {
					if (DEBUG && !this.level.isClientSide) {
						TreesDoNotFloat.LOG.info("Discarding log entity " + this.getId() + " after placing block.");
					}
				} else {
					spawnAtLocation(blockState.getBlock());

					if (DEBUG && !this.level.isClientSide) {
						TreesDoNotFloat.LOG.info("Discarding log entity " + this.getId() + " on tick " + time + " after spawning drops because set block failed.");
					}
				}
			} else {
				spawnAtLocation(blockState.getBlock());

				if (DEBUG && !this.level.isClientSide) {
					TreesDoNotFloat.LOG.info("Discarding log entity " + this.getId() + " on tick " + time + " after spawning drops because lower block is not sturdy or position is obstructed.");
				}
			}
		}

		discard();
	}

	private void destroyCollidingDisplaceableBlocks() {
		if (Configurator.hasBreaking) {
			final double x = getX();
			final double y = getY();
			final double z = getZ();
			final int x0 = Mth.floor(x - 0.5);
			final int x1 = Mth.ceil(x + 0.5);
			final int y0 = Mth.floor(y - 0.5);
			final int y1 = Mth.ceil(y + 0.5);
			final int z0 = Mth.floor(z - 0.5);
			final int z1 = Mth.ceil(z + 0.5);
			final BlockPos.MutableBlockPos searchPos = SEARCH_POS.get();

			for (int ix = x0; ix <= x1; ++ix) {
				for (int iy = y0; iy <= y1; ++iy) {
					for (int iz = z0; iz <= z1; ++iz) {
						searchPos.set(ix, iy, iz);
						final BlockState state = level.getBlockState(searchPos);

						if (Configurator.BREAKABLES.contains(state.getMaterial())) {
							level.destroyBlock(searchPos.immutable(), true);
						}
					}
				}
			}
		}
	}

	private static final ThreadLocal<BlockPos.MutableBlockPos> SEARCH_POS = ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);

	public void toBuffer(FriendlyByteBuf buf) {
		buf.writeVarInt(getId());
		buf.writeVarInt(Block.getId(blockState));
		buf.writeUUID(uuid);
		buf.writeDouble(getX());
		buf.writeDouble(getY());
		buf.writeDouble(getZ());
		buf.writeByte(Mth.floor(getXRot() * 256.0F / 360.0F));
		buf.writeByte(Mth.floor(getYRot() * 256.0F / 360.0F));
		final Vec3 velocity = getDeltaMovement();
		buf.writeShort((int) (Mth.clamp(velocity.x, -3.9D, 3.9D) * 8000.0D));
		buf.writeShort((int) (Mth.clamp(velocity.y, -3.9D, 3.9D) * 8000.0D));
		buf.writeShort((int) (Mth.clamp(velocity.z, -3.9D, 3.9D) * 8000.0D));
	}

	public void fromBuffer(FriendlyByteBuf buf) {
		setId(buf.readVarInt());
		blockState = Block.stateById(buf.readVarInt());
		uuid = buf.readUUID();
		final double x = buf.readDouble();
		final double y = buf.readDouble();
		final double z = buf.readDouble();
		setPosRaw(x, y, z);
		setPacketCoordinates(x, y, z);
		setXRot(buf.readByte() * 360 / 256.0F);
		setYRot(buf.readByte());
		final double vx = buf.readShort() / 8000.0D;
		final double vy = buf.readShort() / 8000.0D;
		final double vz = buf.readShort() / 8000.0D;
		this.setDeltaMovement(vx, vy, vz);
	}

	@Override
	public void remove(RemovalReason reason) {
		if (!isRemoved() && !level.isClientSide) {
			--entityCount;

			if (DEBUG && !this.level.isClientSide && entityCount == 0) {
				TreesDoNotFloat.LOG.info("No falling log entities remaining.");
			}
		}

		super.remove(reason);
	}

	private static int entityCount = 0;

	public static boolean canSpawn() {
		return entityCount < Configurator.maxFallingBlocks;
	}
}
