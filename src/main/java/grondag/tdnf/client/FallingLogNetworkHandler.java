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

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import grondag.tdnf.Configurator;
import grondag.tdnf.world.FallingLogEntity;

@Environment(EnvType.CLIENT)
public class FallingLogNetworkHandler {
    public static void accept(Minecraft client, ClientPacketListener handler, FriendlyByteBuf buffer, PacketSender responseSender) {
        if (Configurator.renderFallingLogs) {
            final FallingLogEntity entity = new FallingLogEntity(FallingLogEntity.FALLING_LOG, client.level);
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
