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

import java.util.Random;

import grondag.tdnf.world.FallingLogEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
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
	public void render(FallingLogEntity fallingLogEntity, double x, double y, double z, float float_1, float float_2, class_4587 matrixStack, class_4597 vertexConsumer) {
		BlockState blockState_1 = fallingLogEntity.getBlockState();
		if (blockState_1.getRenderType() == BlockRenderType.MODEL) {
			World world_1 = fallingLogEntity.getWorldClient();
			if (blockState_1 != world_1.getBlockState(new BlockPos(fallingLogEntity)) && blockState_1.getRenderType() == BlockRenderType.MODEL) {
				matrixStack.method_22903();
				BlockPos blockPos_1 = new BlockPos(fallingLogEntity.x, fallingLogEntity.getBoundingBox().maxY, fallingLogEntity.z);
				matrixStack.method_22904((double)(-(blockPos_1.getX() & 15)) - 0.5D, (double)(-(blockPos_1.getY() & 15)), (double)(-(blockPos_1.getZ() & 15)) - 0.5D);
				BlockRenderManager blockRenderManager_1 = MinecraftClient.getInstance().getBlockRenderManager();
				blockRenderManager_1.getModelRenderer().tesselate(world_1, blockRenderManager_1.getModel(blockState_1), blockState_1, blockPos_1, matrixStack, vertexConsumer.getBuffer(BlockRenderLayer.method_22715(blockState_1)), false, new Random(), blockState_1.getRenderingSeed(fallingLogEntity.getFallingBlockPos()));
				matrixStack.method_22909();
				super.render(fallingLogEntity, x, y, z, float_1, float_2, matrixStack, vertexConsumer);
			}
		}
	}

	@Override
	public Identifier getTexture(FallingLogEntity var1) {
		return SpriteAtlasTexture.BLOCK_ATLAS_TEX;
	}
}