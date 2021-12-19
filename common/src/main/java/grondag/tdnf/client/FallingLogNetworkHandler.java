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

package grondag.tdnf.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;

import grondag.tdnf.Configurator;
import grondag.tdnf.Platform;
import grondag.tdnf.world.FallingLogEntity;

public class FallingLogNetworkHandler {
	public static void accept(Minecraft client, ClientPacketListener handler, FriendlyByteBuf buffer) {
		if (Configurator.renderFallingLogs) {
			final FallingLogEntity entity = new FallingLogEntity(Platform.fallingLogEntityType(), client.level);
			entity.fromBuffer(buffer);

			if (client.isSameThread()) {
				spawn(client, entity);
			} else {
				client.execute(() -> spawn(client, entity));
			}
		}
	}

	private static void spawn(Minecraft client, FallingLogEntity entity) {
		final ClientLevel world = client.level;

		if (world == null) {
			return;
		}

		world.putNonPlayerEntity(entity.getId(), entity);
	}
}
