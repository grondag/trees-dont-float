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

import java.util.Random;

import grondag.tdnf.Configurator;

import net.minecraft.util.math.MathHelper;

public class FxManager {
	/** Limits particle and sound spawning */
	private int fxBudget = Configurator.effectsPerSecond;

	/** End (ms) of current FX accounting period */
	private long fxClockEnd = 0;

	private int expectedTotalBreaks = 0;

	private float fxChance = 0;

	private float fxChanceTotal = 0;

	private boolean dirtyForecast = true;

	private final Random random = new Random();

	public void addExpected(int breakCount) {
		expectedTotalBreaks += breakCount;
		dirtyForecast = true;
	}

	/** call when new tree */
	public void reset() {
		expectedTotalBreaks = 0;
		dirtyForecast = true;
		if(fxBudget == 0) {
			// allow some fx when multiple trees per second
			fxBudget = Math.max(0, Configurator.effectsPerSecond - 2);
		}
	}

	public boolean request(boolean isBreak) {
		if(dirtyForecast) {
			if(fxBudget > 0) {
				final int estimatedTicks = Math.round((fxClockEnd - System.currentTimeMillis()) / 50);
				final int expectedBreaksThisSecond = Math.min(expectedTotalBreaks, Configurator.maxBreaksPerTick * estimatedTicks);
				fxChance = MathHelper.clamp((float)fxBudget / expectedBreaksThisSecond, 0f, 1f);
			} else {
				fxChance = 0;
			}
			fxChanceTotal = 0;
			dirtyForecast = false;
		}

		expectedTotalBreaks--;

		fxChanceTotal += fxChance;

		if(isBreak && random.nextFloat() < fxChanceTotal) {
			fxChanceTotal -= 1;
			return true;
		} else {
			return false;
		}
	}

	public void prepareForTick() {
		final long ms = System.currentTimeMillis();

		if(ms > fxClockEnd) {
			fxBudget = Configurator.effectsPerSecond;
			fxClockEnd  = ms + 1000;
			dirtyForecast = true;
		}
	}
}
