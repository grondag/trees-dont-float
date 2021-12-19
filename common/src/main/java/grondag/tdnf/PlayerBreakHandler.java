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

package grondag.tdnf;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import grondag.tdnf.Configurator.FallCondition;
import grondag.tdnf.world.Dispatcher;
import grondag.tdnf.world.DropHandler;
import grondag.tdnf.world.TreeBlock;

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
