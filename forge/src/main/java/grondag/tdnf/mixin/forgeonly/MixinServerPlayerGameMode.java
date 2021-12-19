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

package grondag.tdnf.mixin.forgeonly;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.state.BlockState;

import grondag.tdnf.PlayerBreakHandler;

/**
 * Forge does not have the same granularity of events as Fabric
 * and messes hard with block breaking logic, so we use mixins
 * to emulate the same flow of events.
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class MixinServerPlayerGameMode {
	@Shadow protected ServerLevel level;
	@Shadow protected ServerPlayer player;

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;removedByPlayer(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;ZLnet/minecraft/world/level/material/FluidState;)Z"), method = "removeBlock", locals = LocalCapture.CAPTURE_FAILHARD, remap = false)
	private void beforeBreakBlock(BlockPos pos, boolean canHarvest, CallbackInfoReturnable<Boolean> cir, BlockState state) {
		PlayerBreakHandler.beforeBreak(level, player, pos, state, null);
	}

	@Inject(at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/level/block/state/BlockState;removedByPlayer(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;ZLnet/minecraft/world/level/material/FluidState;)Z"), method = "removeBlock", locals = LocalCapture.CAPTURE_FAILHARD, remap = false)
	private void afterBreakBlock(BlockPos pos, boolean canHarvest, CallbackInfoReturnable<Boolean> cir, BlockState state, boolean removed) {
		if (removed) {
			PlayerBreakHandler.onBreak(level, player, pos, state, null);
		} else {
			PlayerBreakHandler.onCanceled(level, player, pos, state, null);
		}
	}
}
