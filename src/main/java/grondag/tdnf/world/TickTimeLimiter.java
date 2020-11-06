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

package grondag.tdnf.world;

import grondag.tdnf.Configurator;

public class TickTimeLimiter {
	private TickTimeLimiter() {}

	static long maxTime;

	public static void reset() {
		maxTime = System.nanoTime() + 1000000000 / 100 * Configurator.tickBudget;
	}

	public static boolean canRun() {
		return System.nanoTime() < maxTime;
	}
}
