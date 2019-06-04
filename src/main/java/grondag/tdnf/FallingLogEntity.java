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

import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCategory;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.AutomaticItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Packet;
import net.minecraft.state.property.Properties;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.util.TagHelper;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

/**
 * Hacked-up variant of FallingBlock
 */
public class FallingLogEntity extends Entity {

    public static Identifier IDENTIFIER = new Identifier(TreesDoNotFloat.MODID, "falling_log");
    
    public static final EntityType<? extends FallingLogEntity> FALLING_LOG =
            Registry.register(
                    Registry.ENTITY_TYPE,
                    IDENTIFIER,
                    FabricEntityTypeBuilder.<FallingLogEntity>create(EntityCategory.MISC, FallingLogEntity::new).size(EntitySize.constant(1, 1)).build()
                    );

    public FallingLogEntity(World world, double x, double y, double z, BlockState state) {
        this(FALLING_LOG, world);
        this.block = state;
        this.field_6033 = true;
        this.setPosition(x, y + (double)((1.0F - this.getHeight()) / 2.0F), z);
        this.setVelocity(Vec3d.ZERO);
        this.prevX = x;
        this.prevY = y;
        this.prevZ = z;
        this.setFallingBlockPos(new BlockPos(this));
    }

    public FallingLogEntity(EntityType<?> entityType, World world_1) {
        super(entityType, world_1);
        this.block = Blocks.OAK_LOG.getDefaultState();
        this.dropItem = true;
        this.fallHurtMax = 40;
        this.fallHurtAmount = 2.0F;
    }

    private BlockState block;
    public int timeFalling;
    public boolean dropItem;
    private boolean destroyedOnLanding;
    private boolean hurtEntities;
    private int fallHurtMax;
    private float fallHurtAmount;
    protected static final TrackedData<BlockPos> BLOCK_POS;

    @Override
    public boolean canPlayerAttack() {
        return false;
    }

    public void setFallingBlockPos(BlockPos blockPos_1) {
        this.dataTracker.set(BLOCK_POS, blockPos_1);
    }

    @Environment(EnvType.CLIENT)
    public BlockPos getFallingBlockPos() {
        return (BlockPos)this.dataTracker.get(BLOCK_POS);
    }

    @Override
    protected boolean canClimb() {
        return false;
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(BLOCK_POS, BlockPos.ORIGIN);
    }

    @Override
    public boolean collides() {
        return !this.removed;
    }

    @Override
    public void tick() {
        if (this.block.isAir()) {
            this.remove();
        } else {
            this.prevX = this.x;
            this.prevY = this.y;
            this.prevZ = this.z;
            Block block_1 = this.block.getBlock();
            BlockPos myPos;
            if (this.timeFalling++ == 0) {
                myPos = new BlockPos(this);
                if (this.world.getBlockState(myPos).getBlock() == block_1) {
                    this.world.clearBlockState(myPos, false);
                } else if (!this.world.isClient) {
                    this.remove();
                    return;
                }
            }

            if (!this.hasNoGravity()) {
                this.setVelocity(this.getVelocity().add(0.0D, -0.04D, 0.0D));
            }

            this.move(MovementType.SELF, this.getVelocity());
            // PERF: mutable?
            myPos = new BlockPos(this);

            if (!this.onGround) {
                if (!this.world.isClient && (this.timeFalling > 100 && (myPos.getY() < 1 || myPos.getY() > 256) || this.timeFalling > 600)) {
                    if (this.dropItem && this.world.getGameRules().getBoolean("doEntityDrops")) {
                        this.dropItem(block_1);
                    }

                    this.remove();
                }
            } else {
                BlockState localBlockState = this.world.getBlockState(myPos);
                this.setVelocity(this.getVelocity().multiply(0.7D, -0.5D, 0.7D));
                if (localBlockState.getBlock() != Blocks.MOVING_PISTON) {

                    // if block below can be replaced then keep falling
                    BlockPos downPos = myPos.down();
                    BlockState downBlockState = this.world.getBlockState(downPos);
                    if(downBlockState.canReplace(new AutomaticItemPlacementContext(this.world, downPos, Direction.DOWN, ItemStack.EMPTY, Direction.UP)) && this.block.canPlaceAt(this.world, downPos)) {
                        this.setPosition(myPos.getX() + 0.5, this.y, myPos.getZ() + 0.5);
                        this.setVelocity(0, this.getVelocity().y, 0);
                        this.onGround = false;
                        this.collided = false;
                    } else {
                        this.remove();
                        if(!this.world.isClient) {
                            if (!this.destroyedOnLanding) {
                                if (localBlockState.canReplace(new AutomaticItemPlacementContext(this.world, myPos, Direction.DOWN, ItemStack.EMPTY, Direction.UP)) && this.block.canPlaceAt(this.world, myPos)) {
                                    if (this.block.contains(Properties.WATERLOGGED) && this.world.getFluidState(myPos).getFluid() == Fluids.WATER) {
                                        this.block = (BlockState)this.block.with(Properties.WATERLOGGED, true);
                                    }

                                    if (this.world.setBlockState(myPos, this.block, 3)) {
                                        // noop
                                    } else if (this.dropItem && this.world.getGameRules().getBoolean("doEntityDrops")) {
                                        this.dropItem(block_1);
                                    }
                                } else if (this.dropItem && this.world.getGameRules().getBoolean("doEntityDrops")) {
                                    this.dropItem(block_1);
                                }
                            }
                        }
                    }
                }
            }

            this.setVelocity(this.getVelocity().multiply(0.98D));
        }
    }

    @Override
    public void handleFallDamage(float float_1, float float_2) {
        if (this.hurtEntities) {
            int int_1 = MathHelper.ceil(float_1 - 1.0F);
            if (int_1 > 0) {
                List<Entity> list_1 = Lists.newArrayList(this.world.getEntities(this, this.getBoundingBox()));
                boolean boolean_1 = this.block.matches(BlockTags.ANVIL);
                DamageSource damageSource_1 = boolean_1 ? DamageSource.ANVIL : DamageSource.FALLING_BLOCK;
                Iterator<Entity> var7 = list_1.iterator();

                while(var7.hasNext()) {
                    Entity entity_1 = (Entity)var7.next();
                    entity_1.damage(damageSource_1, (float)Math.min(MathHelper.floor((float)int_1 * this.fallHurtAmount), this.fallHurtMax));
                }

                if (boolean_1 && (double)this.random.nextFloat() < 0.05000000074505806D + (double)int_1 * 0.05D) {
                    BlockState blockState_1 = AnvilBlock.getLandingState(this.block);
                    if (blockState_1 == null) {
                        this.destroyedOnLanding = true;
                    } else {
                        this.block = blockState_1;
                    }
                }
            }
        }

    }

    @Override
    protected void writeCustomDataToTag(CompoundTag compoundTag_1) {
        compoundTag_1.put("BlockState", TagHelper.serializeBlockState(this.block));
        compoundTag_1.putInt("Time", this.timeFalling);
        compoundTag_1.putBoolean("DropItem", this.dropItem);
        compoundTag_1.putBoolean("HurtEntities", this.hurtEntities);
        compoundTag_1.putFloat("FallHurtAmount", this.fallHurtAmount);
        compoundTag_1.putInt("FallHurtMax", this.fallHurtMax);
    }

    @Override
    protected void readCustomDataFromTag(CompoundTag compoundTag_1) {
        this.block = TagHelper.deserializeBlockState(compoundTag_1.getCompound("BlockState"));
        this.timeFalling = compoundTag_1.getInt("Time");
        if (compoundTag_1.containsKey("HurtEntities", 99)) {
            this.hurtEntities = compoundTag_1.getBoolean("HurtEntities");
            this.fallHurtAmount = compoundTag_1.getFloat("FallHurtAmount");
            this.fallHurtMax = compoundTag_1.getInt("FallHurtMax");
        } else if (this.block.matches(BlockTags.ANVIL)) {
            this.hurtEntities = true;
        }

        if (compoundTag_1.containsKey("DropItem", 99)) {
            this.dropItem = compoundTag_1.getBoolean("DropItem");
        }

        if (this.block.isAir()) {
            this.block = Blocks.OAK_LOG.getDefaultState();
        }

    }

    @Environment(EnvType.CLIENT)
    public World getWorldClient() {
        return this.world;
    }

    public void setHurtEntities(boolean boolean_1) {
        this.hurtEntities = boolean_1;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public boolean doesRenderOnFire() {
        return false;
    }

    @Override
    public void populateCrashReport(CrashReportSection crashReportSection_1) {
        super.populateCrashReport(crashReportSection_1);
        crashReportSection_1.add("Immitating BlockState", (Object)this.block.toString());
    }

    public BlockState getBlockState() {
        return this.block;
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
    
    public void toBuffer(PacketByteBuf buf) {
        buf.writeVarInt(this.getEntityId());
        buf.writeUuid(this.uuid);
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeByte(MathHelper.floor(this.pitch * 256.0F / 360.0F));
        buf.writeByte(MathHelper.floor(this.yaw * 256.0F / 360.0F));
        final Vec3d velocity = this.getVelocity();
        buf.writeShort((int)(MathHelper.clamp(velocity.x, -3.9D, 3.9D) * 8000.0D));
        buf.writeShort((int)(MathHelper.clamp(velocity.y, -3.9D, 3.9D) * 8000.0D));
        buf.writeShort((int)(MathHelper.clamp(velocity.z, -3.9D, 3.9D) * 8000.0D));
    }
    
    public void fromBuffer(PacketByteBuf buf) {
        this.setEntityId(buf.readVarInt());
        this.uuid = buf.readUuid();
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        method_18003(x, y, z);
        this.pitch = (float)(buf.readByte() * 360) / 256.0F;
        this.yaw = buf.readByte();
        final double vx = (double)buf.readShort() / 8000.0D;
        final double vy = (double)buf.readShort() / 8000.0D;
        final double vz = (double)buf.readShort() / 8000.0D;
        this.setVelocity(vx, vy, vz);
    }

    static {
        BLOCK_POS = DataTracker.registerData(FallingBlockEntity.class, TrackedDataHandlerRegistry.BLOCK_POS);
    }
}