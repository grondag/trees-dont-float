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

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.server.level.ServerLevel;

import grondag.tdnf.world.Dispatcher;

@Mod.EventBusSubscriber(modid = TreesDoNotFloat.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TdnfEvent {
	@SubscribeEvent
	public static void worldTickEvent(TickEvent.WorldTickEvent event) {
		if (event.side == LogicalSide.SERVER) {
			if (event.phase == TickEvent.Phase.END) {
				Dispatcher.routeTick((ServerLevel) event.world);
			}
		}
	}
}
