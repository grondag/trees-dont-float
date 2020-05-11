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
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCategory;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.AutomaticItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;

import grondag.tdnf.Configurator;
import grondag.tdnf.TreesDoNotFloat;

/**
 * Hacked-up variant of FallingBlock
 */
public class FallingLogEntity extends Entity {

	public static Identifier IDENTIFIER = new Identifier(TreesDoNotFloat.MODID, "falling_log");

	public static final EntityType<? extends FallingLogEntity> FALLING_LOG = Registry.register(Registry.ENTITY_TYPE, IDENTIFIER,
			FabricEntityTypeBuilder.<FallingLogEntity>create(EntityCategory.MISC, FallingLogEntity::new).size(EntityDimensions.fixed(0.9f, 0.9f)).build());

	public FallingLogEntity(World world, double x, double y, double z, BlockState state) {
		this(FALLING_LOG, world);
		if(!world.isClient) {
			entityCount++;
		}
		fallingBlockState = state;
		inanimate = true;
		updatePosition(x, y + (1.0F - getHeight()) / 2.0F, z);
		setVelocity(Vec3d.ZERO);
		prevX = x;
		prevY = y;
		prevZ = z;
		setFallingBlockPos(getBlockPos());
	}

	public FallingLogEntity(EntityType<?> entityType, World world_1) {
		super(entityType, world_1);
		if(!world.isClient) {
			entityCount++;
		}
		fallingBlockState = Blocks.OAK_LOG.getDefaultState();
		dropItem = true;
		fallHurtMax = 40;
		fallHurtAmount = 2.0F;
	}

	private BlockState fallingBlockState;
	public int timeFalling;
	public boolean dropItem;
	private boolean destroyedOnLanding;
	private boolean hurtEntities;
	private int fallHurtMax;
	private float fallHurtAmount;
	protected static final TrackedData<BlockPos> BLOCK_POS;

	@Override
	public boolean isAttackable() {
		return false;
	}

	public void setFallingBlockPos(BlockPos blockPos_1) {
		dataTracker.set(BLOCK_POS, blockPos_1);
	}

	@Environment(EnvType.CLIENT)
	public BlockPos getFallingBlockPos() {
		return dataTracker.get(BLOCK_POS);
	}

	@Override
	protected boolean canClimb() {
		return false;
	}

	@Override
	protected void initDataTracker() {
		dataTracker.startTracking(BLOCK_POS, BlockPos.ORIGIN);
	}

	@Override
	public boolean collides() {
		return !removed;
	}

	@Override
	public void tick() {
		if (fallingBlockState.isAir()) {
			remove();
		} else {
			prevX = getX();
			prevY = getY();
			prevZ = getZ();
			final Block block_1 = fallingBlockState.getBlock();
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

					remove();
				}
			} else {
				final BlockState localBlockState = world.getBlockState(myPos);
				this.setVelocity(getVelocity().multiply(0.7D, -0.5D, 0.7D));
				if (localBlockState.getBlock() != Blocks.MOVING_PISTON) {

					// if block below can be replaced then keep falling
					final BlockPos downPos = myPos.down(1);
					final BlockState downBlockState = world.getBlockState(downPos);
					if (downBlockState.canReplace(new AutomaticItemPlacementContext(world, downPos, Direction.DOWN, ItemStack.EMPTY, Direction.UP))
							&& fallingBlockState.canPlaceAt(world, downPos)) {
						setPos(myPos.getX() + 0.5, getY(), myPos.getZ() + 0.5);
						this.setVelocity(0, getVelocity().y, 0);
						onGround = false;
						verticalCollision = false;
					} else {
						remove();

						if (!world.isClient) {
							if (!destroyedOnLanding) {
								if (localBlockState
										.canReplace(new AutomaticItemPlacementContext(world, myPos, Direction.DOWN, ItemStack.EMPTY, Direction.UP))
										&& fallingBlockState.canPlaceAt(world, myPos)) {
									if (fallingBlockState.contains(Properties.WATERLOGGED) && world.getFluidState(myPos).getFluid() == Fluids.WATER) {
										fallingBlockState = fallingBlockState.with(Properties.WATERLOGGED, true);
									}

									if (world.setBlockState(myPos, fallingBlockState, 3)) {
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
			}

			this.setVelocity(getVelocity().multiply(0.98D));
		}
	}

	@Override
	public boolean handleFallDamage(float distIn, float float_2) {
		if (hurtEntities) {
			final int dist = MathHelper.ceil(distIn - 1.0F);

			if (dist > 0) {
				for (final Entity e : world.getEntities(this, getBoundingBox())) {
					e.damage(DamageSource.FALLING_BLOCK, Math.min(MathHelper.floor(dist * fallHurtAmount), fallHurtMax));
				}
			}
		}

		return false;
	}

	@Override
	protected void writeCustomDataToTag(CompoundTag tag) {
		tag.put("BlockState", NbtHelper.fromBlockState(fallingBlockState));
		tag.putInt("Time", timeFalling);
		tag.putBoolean("DropItem", dropItem);
		tag.putBoolean("HurtEntities", hurtEntities);
		tag.putFloat("FallHurtAmount", fallHurtAmount);
		tag.putInt("FallHurtMax", fallHurtMax);
	}

	@Override
	protected void readCustomDataFromTag(CompoundTag compoundTag_1) {
		fallingBlockState = NbtHelper.toBlockState(compoundTag_1.getCompound("BlockState"));
		timeFalling = compoundTag_1.getInt("Time");
		if (compoundTag_1.contains("HurtEntities", 99)) {
			hurtEntities = compoundTag_1.getBoolean("HurtEntities");
			fallHurtAmount = compoundTag_1.getFloat("FallHurtAmount");
			fallHurtMax = compoundTag_1.getInt("FallHurtMax");
		}

		if (compoundTag_1.contains("DropItem", 99)) {
			dropItem = compoundTag_1.getBoolean("DropItem");
		}

		if (fallingBlockState.isAir()) {
			fallingBlockState = Blocks.OAK_LOG.getDefaultState();
		}
	}

	@Environment(EnvType.CLIENT)
	public World getWorldClient() {
		return world;
	}

	public void setHurtEntities(boolean boolean_1) {
		hurtEntities = boolean_1;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public boolean doesRenderOnFire() {
		return false;
	}

	@Override
	public void populateCrashReport(CrashReportSection crashReportSection_1) {
		super.populateCrashReport(crashReportSection_1);
		crashReportSection_1.add("Immitating BlockState", fallingBlockState.toString());
	}

	public BlockState getBlockState() {
		return fallingBlockState;
	}

	@Override
	public boolean entityDataRequiresOperator() {
		return true;
	}

	@Override
	public Packet<?> createSpawnPacket() {
		final PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
		toBuffer(buf);
		return ServerSidePacketRegistry.INSTANCE.toPacket(IDENTIFIER, buf);
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
		buf.writeVarInt(getEntityId());
		buf.writeVarInt(Block.getRawIdFromState(fallingBlockState));
		buf.writeUuid(uuid);
		buf.writeDouble(getX());
		buf.writeDouble(getY());
		buf.writeDouble(getZ());
		buf.writeByte(MathHelper.floor(pitch * 256.0F / 360.0F));
		buf.writeByte(MathHelper.floor(yaw * 256.0F / 360.0F));
		final Vec3d velocity = getVelocity();
		buf.writeShort((int) (MathHelper.clamp(velocity.x, -3.9D, 3.9D) * 8000.0D));
		buf.writeShort((int) (MathHelper.clamp(velocity.y, -3.9D, 3.9D) * 8000.0D));
		buf.writeShort((int) (MathHelper.clamp(velocity.z, -3.9D, 3.9D) * 8000.0D));
	}

	public void fromBuffer(PacketByteBuf buf) {
		setEntityId(buf.readVarInt());
		fallingBlockState = Block.getStateFromRawId(buf.readVarInt());
		uuid = buf.readUuid();
		final double x = buf.readDouble();
		final double y = buf.readDouble();
		final double z = buf.readDouble();
		setPos(x, y, z);
		updateTrackedPosition(x, y, z);
		pitch = buf.readByte() * 360 / 256.0F;
		yaw = buf.readByte();
		final double vx = buf.readShort() / 8000.0D;
		final double vy = buf.readShort() / 8000.0D;
		final double vz = buf.readShort() / 8000.0D;
		this.setVelocity(vx, vy, vz);
	}


	@Override
	public void remove() {
		if (!removed) {
			entityCount--;
		}

		super.remove();
	}


	static {
		BLOCK_POS = DataTracker.registerData(FallingLogEntity.class, TrackedDataHandlerRegistry.BLOCK_POS);
	}

	private static int entityCount = 0;

	public static boolean canSpawn() {
		return entityCount < Configurator.maxFallingBlocks;
	}
}
