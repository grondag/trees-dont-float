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

import grondag.tdnf.Configurator.FallCondition;
import grondag.tdnf.world.Dispatcher;
import grondag.tdnf.world.DropHandler;
import grondag.tdnf.world.LogTest;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PlayerBreakHandler {
	private static boolean isActive = false;

	/**
	 * True when player break action is active and non-player fall conditions should be ignored.
	 */
	public static boolean isActive() {
		return isActive;
	}

	public static boolean beforeBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, /* Nullable */ BlockEntity blockEntity) {
		if (player != null && LogTest.test(state)) {
			isActive = true;
		}

		return true;
	}

	public static void onCanceled(World world, PlayerEntity player, BlockPos pos, BlockState state, /* Nullable */ BlockEntity blockEntity) {
		isActive = false;
	}

	public static void onBreak(World world, PlayerEntity playerEntity, BlockPos pos, BlockState state, /* Nullable */ BlockEntity blockEntity) {
		isActive = false;

		if (playerEntity != null && LogTest.test(state)) {
			final ServerPlayerEntity player = (ServerPlayerEntity) playerEntity;

			if (!Configurator.activeWhen.test(player)) {
				return;
			}

			if (Configurator.fallCondition == FallCondition.USE_TOOL) {
				if(DropHandler.hasAxe(player, player.getMainHandStack())) {
					Dispatcher.enqueCheck((ServerWorld) world, pos, player);
				}
			} else {
				//            System.out.println("onBreak playerEntity = " + (playerEntity == null ? "NULL" : playerEntity.toString()));
				Dispatcher.enqueCheck((ServerWorld) world, pos, player);
			}
		}
	}
}
