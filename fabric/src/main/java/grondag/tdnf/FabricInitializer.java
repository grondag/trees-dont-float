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

package grondag.tdnf;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

import grondag.tdnf.config.Configurator;
import grondag.tdnf.world.Dispatcher;

public class FabricInitializer implements ModInitializer {
	@Override
	public void onInitialize() {
		Configurator.init();
		ServerTickEvents.END_WORLD_TICK.register(Dispatcher::routeTick);
		PlayerBlockBreakEvents.BEFORE.register(PlayerBreakHandler::beforeBreak);
		PlayerBlockBreakEvents.AFTER.register(PlayerBreakHandler::onBreak);
		PlayerBlockBreakEvents.CANCELED.register(PlayerBreakHandler::onCanceled);
	}
}
