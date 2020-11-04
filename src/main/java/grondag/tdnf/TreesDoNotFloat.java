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

import grondag.tdnf.world.Dispatcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

public class TreesDoNotFloat implements ModInitializer {
	@Override
	public void onInitialize() {
		Dispatcher.init();
		Configurator.init();

		PlayerBlockBreakEvents.BEFORE.register(PlayerBreakHandler::beforeBreak);
		PlayerBlockBreakEvents.AFTER.register(PlayerBreakHandler::onBreak);
		PlayerBlockBreakEvents.CANCELED.register(PlayerBreakHandler::onCanceled);
	}

	public static final String MODID = "trees-do-not-float";

	public static final Logger LOG = LogManager.getLogger("trees-do-not-float");
}
