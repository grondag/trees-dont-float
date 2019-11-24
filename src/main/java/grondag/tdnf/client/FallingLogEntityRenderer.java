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
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
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
		field_4673 = 0.5F;
	}

	@Override
	public void render(FallingLogEntity fallingLogEntity, float yawDelta, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider provider, int light) {
		final BlockState blockState = fallingLogEntity.getBlockState();

		if (blockState.getRenderType() == BlockRenderType.MODEL) {
			final World world = fallingLogEntity.getWorldClient();

			if (blockState != world.getBlockState(new BlockPos(fallingLogEntity)) && blockState.getRenderType() == BlockRenderType.MODEL) {
				matrixStack.push();
				final BlockPos blockPos = new BlockPos(fallingLogEntity.getX(), fallingLogEntity.getBoundingBox().y2, fallingLogEntity.getZ());
				matrixStack.translate(-0.5D, 0.0D, -0.5D);
				final BlockRenderManager blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
				blockRenderManager.getModelRenderer().render(world, blockRenderManager.getModel(blockState), blockState, blockPos, matrixStack, provider.getBuffer(RenderLayers.getBlockLayer(blockState)), false, new Random(), blockState.getRenderingSeed(fallingLogEntity.getFallingBlockPos()), OverlayTexture.DEFAULT_UV);
				matrixStack.pop();
				super.render(fallingLogEntity, yawDelta, tickDelta, matrixStack, provider, light);
			}
		}
	}

	@Override
	public Identifier getTexture(FallingLogEntity var1) {
		return SpriteAtlasTexture.BLOCK_ATLAS_TEX;
	}
}