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
	private static boolean isLogInProgress = false;
	private static boolean shouldCheckBreakEvents = true;

	/**
	 * True when break events should be tested against break condition because the block being
	 * broken is not a log AND one of the following is true:
	 *
	 * <ol>
	 * <li> There is no player condition
	 * <li> Player meets the condition
	 * <li> Player is not involved
	 * </ol>
	 *
	 * <p>Relies heavily on the single-threaded nature of server-side event processing.
	 */
	public static boolean shouldCheckBreakEvents() {
		return shouldCheckBreakEvents;
	}

	public static boolean beforeBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, /* Nullable */ BlockEntity blockEntity) {
		isLogInProgress = LogTest.test(state);
		shouldCheckBreakEvents = !isLogInProgress && (player == null || Configurator.activeWhen.test(player));
		return true;
	}

	public static void onCanceled(World world, PlayerEntity player, BlockPos pos, BlockState state, /* Nullable */ BlockEntity blockEntity) {
		isLogInProgress = false;
		shouldCheckBreakEvents = true;
	}

	public static void onBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, /* Nullable */ BlockEntity blockEntity) {
		if (isLogInProgress) {
			if (LogTest.test(state)) {
				if (Configurator.fallCondition != FallCondition.USE_TOOL || DropHandler.hasAxe(player, player.getMainHandStack())) {
					Dispatcher.enqueCheck((ServerWorld) world, pos, (ServerPlayerEntity) player);
				}
			}

			isLogInProgress = false;
		}

		shouldCheckBreakEvents = true;
	}
}
