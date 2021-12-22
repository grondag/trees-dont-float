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

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

import grondag.tdnf.config.Configurator;

public class ProtectionTracker extends SavedData {
	public static final String FILE_ID = "tdnf_protected";
	private static final String TAG_NAME = "tdnf_protected";

	private final LongOpenHashSet set;

	private ProtectionTracker(LongOpenHashSet set) {
		this.set = set;
	}

	public ProtectionTracker() {
		this(new LongOpenHashSet());
	}

	public static ProtectionTracker load(CompoundTag tag) {
		return new ProtectionTracker(new LongOpenHashSet(tag.getLongArray(TAG_NAME)));
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		tag.putLongArray(TAG_NAME, set.toLongArray());
		return tag;
	}

	public boolean isProtected(long position) {
		return Configurator.protectPlacedBlocks && set.contains(position);
	}

	public boolean isProtected(BlockPos pos) {
		return isProtected(pos.asLong());
	}

	public void protect(long position) {
		set.add(position);
		setDirty();
	}

	public void protect(BlockPos pos) {
		protect(pos.asLong());
	}

	public void unprotect(long position) {
		set.remove(position);
		setDirty();
	}

	public void unprotect(BlockPos pos) {
		unprotect(pos.asLong());
	}
}
