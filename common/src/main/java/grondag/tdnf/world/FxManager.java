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

package grondag.tdnf.world;

import java.util.Random;

import net.minecraft.util.Mth;

import grondag.tdnf.Configurator;

public class FxManager {
	/** Limits particle and sound spawning. */
	private int fxBudget = Configurator.effectsPerSecond;

	/** End (ms) of current FX accounting period. */
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

	/** call when new tree. */
	public void reset() {
		expectedTotalBreaks = 0;
		dirtyForecast = true;

		if (fxBudget == 0) {
			// allow some fx when multiple trees per second
			fxBudget = Math.max(0, Configurator.effectsPerSecond - 2);
		}
	}

	public boolean request(boolean isBreak) {
		if (dirtyForecast) {
			if (fxBudget > 0) {
				final int estimatedTicks = Math.round((fxClockEnd - System.currentTimeMillis()) / 50);
				final int expectedBreaksThisSecond = Math.min(expectedTotalBreaks, Configurator.maxBreaksPerSecond * estimatedTicks);
				fxChance = Mth.clamp((float) fxBudget / expectedBreaksThisSecond, 0f, 1f);
			} else {
				fxChance = 0;
			}

			fxChanceTotal = 0;
			dirtyForecast = false;
		}

		expectedTotalBreaks--;

		fxChanceTotal += fxChance;

		if (isBreak && random.nextFloat() < fxChanceTotal) {
			fxChanceTotal -= 1;
			return true;
		} else {
			return false;
		}
	}

	public void prepareForTick() {
		final long ms = System.currentTimeMillis();

		if (ms > fxClockEnd) {
			fxBudget = Configurator.effectsPerSecond;
			fxClockEnd = ms + 1000;
			dirtyForecast = true;
		}
	}
}
