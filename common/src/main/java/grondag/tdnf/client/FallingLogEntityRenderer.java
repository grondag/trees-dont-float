/*
 * This file is part of Trees Do Not Float and is licensed to the project under
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

package grondag.tdnf.client;

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
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

import grondag.tdnf.FallingLogEntity;

/**
 * Straight-up copy of FallingBlockEntityRenderer.
 */
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
				blockRenderManager.getModelRenderer().tesselateBlock(world, blockRenderManager.getBlockModel(blockState), blockState, blockPos, matrixStack, provider.getBuffer(ItemBlockRenderTypes.getChunkRenderType(blockState)), false, RandomSource.create(), blockState.getSeed(fallingLogEntity.getStartPos()), OverlayTexture.NO_OVERLAY);
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
