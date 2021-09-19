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

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import grondag.tdnf.Configurator;
import grondag.tdnf.TreesDoNotFloat;

/**
 * Hacked-up variant of FallingBlock
 */
public class FallingLogEntity extends FallingBlockEntity {

	public static ResourceLocation IDENTIFIER = new ResourceLocation(TreesDoNotFloat.MODID, "falling_log");

	public static final EntityType<? extends FallingLogEntity> FALLING_LOG = Registry.register(Registry.ENTITY_TYPE, IDENTIFIER,
			FabricEntityTypeBuilder.<FallingLogEntity>create(MobCategory.MISC, FallingLogEntity::new).dimensions(EntityDimensions.fixed(0.9f, 0.9f)).build());

	public FallingLogEntity(EntityType<? extends FallingLogEntity> entityType, Level world) {
		super(entityType, world);

		if(!world.isClientSide) {
			entityCount++;
		}

		blockState = Blocks.OAK_LOG.defaultBlockState();
	}

	public FallingLogEntity(Level world, double x, double y, double z, BlockState state) {
		this(FALLING_LOG, world);
		blockState = state;
		blocksBuilding = true;
		absMoveTo(x, y + (1.0F - getBbHeight()) / 2.0F, z);
		setDeltaMovement(Vec3.ZERO);
		xo = x;
		yo = y;
		zo = z;
		setStartPos(blockPosition());
	}

	@Override
	public void tick() {
		if (blockState.isAir()) {
			remove(RemovalReason.DISCARDED);
		} else {
			xo = getX();
			yo = getY();
			zo = getZ();
			final Block block_1 = blockState.getBlock();
			BlockPos myPos;
			time++;

			if (!isNoGravity()) {
				this.setDeltaMovement(getDeltaMovement().add(0.0D, -0.04D, 0.0D));
			}

			move(MoverType.SELF, getDeltaMovement());
			myPos = blockPosition();

			if (!onGround) {
				if (!level.isClientSide && (time > 100 && (myPos.getY() < 1 || myPos.getY() > 256) || time > 600)) {
					if (dropItem && level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
						this.spawnAtLocation(block_1);
					}

					remove(RemovalReason.DISCARDED);
				}
			} else {
				final BlockState localBlockState = level.getBlockState(myPos);
				this.setDeltaMovement(getDeltaMovement().multiply(0.7D, -0.5D, 0.7D));
				if (localBlockState.getBlock() != Blocks.MOVING_PISTON) {

					// if block below can be replaced then keep falling
					final BlockPos downPos = myPos.below(1);
					final BlockState downBlockState = level.getBlockState(downPos);
					if (downBlockState.canBeReplaced(new DirectionalPlaceContext(level, downPos, Direction.DOWN, ItemStack.EMPTY, Direction.UP))
							&& blockState.canSurvive(level, downPos)) {
						setPosRaw(myPos.getX() + 0.5, getY(), myPos.getZ() + 0.5);
						this.setDeltaMovement(0, getDeltaMovement().y, 0);
						onGround = false;
						verticalCollision = false;
					} else {
						remove(RemovalReason.DISCARDED);

						if (!level.isClientSide) {
							if (localBlockState
									.canBeReplaced(new DirectionalPlaceContext(level, myPos, Direction.DOWN, ItemStack.EMPTY, Direction.UP))
									&& blockState.canSurvive(level, myPos)) {
								if (blockState.hasProperty(BlockStateProperties.WATERLOGGED) && level.getFluidState(myPos).getType() == Fluids.WATER) {
									blockState = blockState.setValue(BlockStateProperties.WATERLOGGED, true);
								}

								if (level.setBlock(myPos, blockState, 3)) {
									// noop
								} else if (dropItem && level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
									this.spawnAtLocation(block_1);
								}
							} else if (dropItem && level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
								this.spawnAtLocation(block_1);
							}
						}
					}
				}
			}

			this.setDeltaMovement(getDeltaMovement().scale(0.98D));
		}
	}

	@Override
	public Packet<?> getAddEntityPacket() {
		final FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		toBuffer(buf);
		return ServerPlayNetworking.createS2CPacket(IDENTIFIER, buf);
	}

	@Override
	public void move(MoverType movementType, Vec3 vec) {
		destroyCollidingDisplaceableBlocks();
		super.move(movementType, vec);
	}

	private void destroyCollidingDisplaceableBlocks() {
		if (Configurator.hasBreaking) {
			final double x = getX();
			final double y = getY();
			final double z = getZ();
			final int i = Mth.floor(x - 0.5);
			final int j = Mth.ceil(x + 0.5);
			final int k = Mth.floor(y - 0.5);
			final int l = Mth.ceil(y + 0.5);
			final int i1 = Mth.floor(z - 0.5);
			final int j1 = Mth.ceil(z + 0.5);
			final BlockPos.MutableBlockPos searchPos = SEARCH_POS.get();

			for (int k1 = i; k1 < j; ++k1) {
				for (int l1 = k; l1 < l; ++l1) {
					for (int i2 = i1; i2 < j1; ++i2) {
						searchPos.set(k1, l1, i2);
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
		}

		super.remove(reason);
	}

	private static int entityCount = 0;

	public static boolean canSpawn() {
		return entityCount < Configurator.maxFallingBlocks;
	}
}
