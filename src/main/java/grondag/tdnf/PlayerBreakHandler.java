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
import grondag.tdnf.world.TreeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

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

	public static boolean beforeBreak(Level world, Player player, BlockPos pos, BlockState state, /* Nullable */ BlockEntity blockEntity) {
		isLogInProgress = TreeBlock.isLog(state);
		shouldCheckBreakEvents = !isLogInProgress && (player == null || Configurator.activeWhen.test(player));
		return true;
	}

	public static void onCanceled(Level world, Player player, BlockPos pos, BlockState state, /* Nullable */ BlockEntity blockEntity) {
		isLogInProgress = false;
		shouldCheckBreakEvents = true;
	}

	public static void onBreak(Level world, Player player, BlockPos pos, BlockState state, /* Nullable */ BlockEntity blockEntity) {
		if (isLogInProgress) {
			if (TreeBlock.isLog(state)) {
				if (Configurator.fallCondition != FallCondition.USE_TOOL || DropHandler.hasAxe(player, player.getMainHandItem())) {
					Dispatcher.enqueCheck((ServerLevel) world, pos, (ServerPlayer) player);
				}
			}

			isLogInProgress = false;
		}

		shouldCheckBreakEvents = true;
	}
}
