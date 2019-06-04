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

package grondag.tdnf.client;

import com.mojang.blaze3d.platform.GlStateManager;

import grondag.tdnf.FallingLogEntity;

import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Straight-up copy of FallingBlockEntityRenderer
 */
@Environment(EnvType.CLIENT)
public class FallingLogEntityRenderer extends EntityRenderer<FallingLogEntity> {
    public FallingLogEntityRenderer(EntityRenderDispatcher entityRenderDispatcher_1) {
        super(entityRenderDispatcher_1);
        this.field_4673 = 0.5F;
    }

    @Override
    public void render(FallingLogEntity fallingLogEntity, double double_1, double double_2, double double_3, float float_1, float float_2) {
        BlockState blockState_1 = fallingLogEntity.getBlockState();
        if (blockState_1.getRenderType() == BlockRenderType.MODEL) {
            World world_1 = fallingLogEntity.getWorldClient();
            if (blockState_1 != world_1.getBlockState(new BlockPos(fallingLogEntity)) && blockState_1.getRenderType() != BlockRenderType.INVISIBLE) {
                this.bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
                GlStateManager.pushMatrix();
                GlStateManager.disableLighting();
                Tessellator tessellator_1 = Tessellator.getInstance();
                BufferBuilder bufferBuilder_1 = tessellator_1.getBufferBuilder();
                if (this.renderOutlines) {
                    GlStateManager.enableColorMaterial();
                    GlStateManager.setupSolidRenderingTextureCombine(this.getOutlineColor(fallingLogEntity));
                }

                bufferBuilder_1.begin(7, VertexFormats.POSITION_COLOR_UV_LMAP);
                BlockPos blockPos_1 = new BlockPos(fallingLogEntity.x, fallingLogEntity.getBoundingBox().maxY, fallingLogEntity.z);
                GlStateManager.translatef((float)(double_1 - (double)blockPos_1.getX() - 0.5D), (float)(double_2 - (double)blockPos_1.getY()), (float)(double_3 - (double)blockPos_1.getZ() - 0.5D));
                BlockRenderManager blockRenderManager_1 = MinecraftClient.getInstance().getBlockRenderManager();
                blockRenderManager_1.getModelRenderer().tesselate(world_1, blockRenderManager_1.getModel(blockState_1), blockState_1, blockPos_1, bufferBuilder_1, false, new Random(), blockState_1.getRenderingSeed(fallingLogEntity.getFallingBlockPos()));
                tessellator_1.draw();
                if (this.renderOutlines) {
                    GlStateManager.tearDownSolidRenderingTextureCombine();
                    GlStateManager.disableColorMaterial();
                }

                GlStateManager.enableLighting();
                GlStateManager.popMatrix();
                super.render(fallingLogEntity, double_1, double_2, double_3, float_1, float_2);
            }
        }
    }

    @Override
    protected Identifier getTexture(FallingLogEntity var1) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEX;
    }
}