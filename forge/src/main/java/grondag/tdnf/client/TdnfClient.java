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

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fmlclient.ConfigGuiHandler;

import grondag.tdnf.Platform;
import grondag.tdnf.TreesDoNotFloat;
import grondag.tdnf.config.Configurator;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = TreesDoNotFloat.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class TdnfClient {
	@SubscribeEvent
	public static void clientSetup(FMLClientSetupEvent event) {
		ModLoadingContext.get().registerExtensionPoint(ConfigGuiHandler.ConfigGuiFactory.class, () ->
				new ConfigGuiHandler.ConfigGuiFactory((minecraft, screen) -> new PresetConfigScreen(screen, Configurator.writeConfig())));
	}

	@SubscribeEvent
	public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(Platform.fallingLogEntityType(), FallingLogEntityRenderer::new);
	}
}
