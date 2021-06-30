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

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.AutomaticItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;

import grondag.tdnf.Configurator;
import grondag.tdnf.TreesDoNotFloat;

/**
 * Hacked-up variant of FallingBlock
 */
public class FallingLogEntity extends FallingBlockEntity {

	public static Identifier IDENTIFIER = new Identifier(TreesDoNotFloat.MODID, "falling_log");

	public static final EntityType<? extends FallingLogEntity> FALLING_LOG = Registry.register(Registry.ENTITY_TYPE, IDENTIFIER,
			FabricEntityTypeBuilder.<FallingLogEntity>create(SpawnGroup.MISC, FallingLogEntity::new).dimensions(EntityDimensions.fixed(0.9f, 0.9f)).build());

	public FallingLogEntity(EntityType<? extends FallingLogEntity> entityType, World world) {
		super(entityType, world);

		if(!world.isClient) {
			entityCount++;
		}

		block = Blocks.OAK_LOG.getDefaultState();
	}

	public FallingLogEntity(World world, double x, double y, double z, BlockState state) {
		this(FALLING_LOG, world);
		block = state;
		inanimate = true;
		updatePosition(x, y + (1.0F - getHeight()) / 2.0F, z);
		setVelocity(Vec3d.ZERO);
		prevX = x;
		prevY = y;
		prevZ = z;
		setFallingBlockPos(getBlockPos());
	}

	@Override
	public void tick() {
		if (block.isAir()) {
			remove(RemovalReason.DISCARDED);
		} else {
			prevX = getX();
			prevY = getY();
			prevZ = getZ();
			final Block block_1 = block.getBlock();
			BlockPos myPos;
			timeFalling++;

			if (!hasNoGravity()) {
				this.setVelocity(getVelocity().add(0.0D, -0.04D, 0.0D));
			}

			move(MovementType.SELF, getVelocity());
			myPos = getBlockPos();

			if (!onGround) {
				if (!world.isClient && (timeFalling > 100 && (myPos.getY() < 1 || myPos.getY() > 256) || timeFalling > 600)) {
					if (dropItem && world.getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS)) {
						this.dropItem(block_1);
					}

					remove(RemovalReason.DISCARDED);
				}
			} else {
				final BlockState localBlockState = world.getBlockState(myPos);
				this.setVelocity(getVelocity().multiply(0.7D, -0.5D, 0.7D));
				if (localBlockState.getBlock() != Blocks.MOVING_PISTON) {

					// if block below can be replaced then keep falling
					final BlockPos downPos = myPos.down(1);
					final BlockState downBlockState = world.getBlockState(downPos);
					if (downBlockState.canReplace(new AutomaticItemPlacementContext(world, downPos, Direction.DOWN, ItemStack.EMPTY, Direction.UP))
							&& block.canPlaceAt(world, downPos)) {
						setPos(myPos.getX() + 0.5, getY(), myPos.getZ() + 0.5);
						this.setVelocity(0, getVelocity().y, 0);
						onGround = false;
						verticalCollision = false;
					} else {
						remove(RemovalReason.DISCARDED);

						if (!world.isClient) {
							if (localBlockState
									.canReplace(new AutomaticItemPlacementContext(world, myPos, Direction.DOWN, ItemStack.EMPTY, Direction.UP))
									&& block.canPlaceAt(world, myPos)) {
								if (block.contains(Properties.WATERLOGGED) && world.getFluidState(myPos).getFluid() == Fluids.WATER) {
									block = block.with(Properties.WATERLOGGED, true);
								}

								if (world.setBlockState(myPos, block, 3)) {
									// noop
								} else if (dropItem && world.getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS)) {
									this.dropItem(block_1);
								}
							} else if (dropItem && world.getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS)) {
								this.dropItem(block_1);
							}
						}
					}
				}
			}

			this.setVelocity(getVelocity().multiply(0.98D));
		}
	}

	@Override
	public Packet<?> createSpawnPacket() {
		final PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
		toBuffer(buf);
		return ServerPlayNetworking.createS2CPacket(IDENTIFIER, buf);
	}

	@Override
	public void move(MovementType movementType, Vec3d vec) {
		destroyCollidingDisplaceableBlocks();
		super.move(movementType, vec);
	}

	private void destroyCollidingDisplaceableBlocks() {
		if (Configurator.hasBreaking) {
			final double x = getX();
			final double y = getY();
			final double z = getZ();
			final int i = MathHelper.floor(x - 0.5);
			final int j = MathHelper.ceil(x + 0.5);
			final int k = MathHelper.floor(y - 0.5);
			final int l = MathHelper.ceil(y + 0.5);
			final int i1 = MathHelper.floor(z - 0.5);
			final int j1 = MathHelper.ceil(z + 0.5);
			final BlockPos.Mutable searchPos = SEARCH_POS.get();

			for (int k1 = i; k1 < j; ++k1) {
				for (int l1 = k; l1 < l; ++l1) {
					for (int i2 = i1; i2 < j1; ++i2) {
						searchPos.set(k1, l1, i2);
						final BlockState state = world.getBlockState(searchPos);
						if (Configurator.BREAKABLES.contains(state.getMaterial())) {
							world.breakBlock(searchPos.toImmutable(), true);
						}
					}
				}
			}
		}
	}

	private static final ThreadLocal<BlockPos.Mutable> SEARCH_POS = ThreadLocal.withInitial(BlockPos.Mutable::new);

	public void toBuffer(PacketByteBuf buf) {
		buf.writeVarInt(getId());
		buf.writeVarInt(Block.getRawIdFromState(block));
		buf.writeUuid(uuid);
		buf.writeDouble(getX());
		buf.writeDouble(getY());
		buf.writeDouble(getZ());
		buf.writeByte(MathHelper.floor(getPitch() * 256.0F / 360.0F));
		buf.writeByte(MathHelper.floor(getYaw() * 256.0F / 360.0F));
		final Vec3d velocity = getVelocity();
		buf.writeShort((int) (MathHelper.clamp(velocity.x, -3.9D, 3.9D) * 8000.0D));
		buf.writeShort((int) (MathHelper.clamp(velocity.y, -3.9D, 3.9D) * 8000.0D));
		buf.writeShort((int) (MathHelper.clamp(velocity.z, -3.9D, 3.9D) * 8000.0D));
	}

	public void fromBuffer(PacketByteBuf buf) {
		setId(buf.readVarInt());
		block = Block.getStateFromRawId(buf.readVarInt());
		uuid = buf.readUuid();
		final double x = buf.readDouble();
		final double y = buf.readDouble();
		final double z = buf.readDouble();
		setPos(x, y, z);
		updateTrackedPosition(x, y, z);
		setPitch(buf.readByte() * 360 / 256.0F);
		setYaw(buf.readByte());
		final double vx = buf.readShort() / 8000.0D;
		final double vy = buf.readShort() / 8000.0D;
		final double vz = buf.readShort() / 8000.0D;
		this.setVelocity(vx, vy, vz);
	}


	@Override
	public void remove(RemovalReason reason) {

		if (!isRemoved() && !world.isClient) {
			--entityCount;
		}

		super.remove(reason);
	}

	private static int entityCount = 0;

	public static boolean canSpawn() {
		return entityCount < Configurator.maxFallingBlocks;
	}
}
