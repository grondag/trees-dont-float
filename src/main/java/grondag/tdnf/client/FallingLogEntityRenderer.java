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

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.tdnf.world.FallingLogEntity;

/**
 * Straight-up copy of FallingBlockEntityRenderer
 */
@Environment(EnvType.CLIENT)
public class FallingLogEntityRenderer extends EntityRenderer<FallingLogEntity> {
	public FallingLogEntityRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
		shadowRadius = 0.5F;
	}

	@Override
	public void render(FallingLogEntity fallingLogEntity, float yawDelta, float tickDelta, PoseStack matrixStack, MultiBufferSource provider, int light) {
		final BlockState blockState = fallingLogEntity.getBlockState();

		if (blockState.getRenderShape() == RenderShape.MODEL) {
			final Level world = fallingLogEntity.getLevel();

			if (blockState != world.getBlockState(new BlockPos(fallingLogEntity.position())) && blockState.getRenderShape() == RenderShape.MODEL) {
				matrixStack.pushPose();
				final BlockPos blockPos = new BlockPos(fallingLogEntity.getX(), fallingLogEntity.getBoundingBox().maxY, fallingLogEntity.getZ());
				matrixStack.translate(-0.5D, 0.0D, -0.5D);
				final BlockRenderDispatcher blockRenderManager = Minecraft.getInstance().getBlockRenderer();
				blockRenderManager.getModelRenderer().tesselateBlock(world, blockRenderManager.getBlockModel(blockState), blockState, blockPos, matrixStack, provider.getBuffer(ItemBlockRenderTypes.getChunkRenderType(blockState)), false, new Random(), blockState.getSeed(fallingLogEntity.getStartPos()), OverlayTexture.NO_OVERLAY);
				matrixStack.popPose();
				super.render(fallingLogEntity, yawDelta, tickDelta, matrixStack, provider, light);
			}
		}
	}

	@Override
	public ResourceLocation getTextureLocation(FallingLogEntity var1) {
		return TextureAtlas.LOCATION_BLOCKS;
	}
}