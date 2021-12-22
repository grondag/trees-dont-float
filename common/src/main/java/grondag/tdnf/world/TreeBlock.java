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

package grondag.tdnf.world;

import net.minecraft.world.level.block.state.BlockState;

public interface TreeBlock {
	int UNKNOWN = 0;
	int LOG = 1;
	int FUNGUS_LOG = 2;
	int FUNGUS_LEAF = 4;
	int OTHER = 8;

	int LOG_MASK = LOG | FUNGUS_LOG;

	int FUNGUS_MASK = FUNGUS_LOG | FUNGUS_LEAF;

	int treeBlockType();

	default boolean isLog() {
		return (treeBlockType() & LOG_MASK) != 0;
	}

	static int getType(BlockState blockState) {
		return ((TreeBlock) blockState.getBlock()).treeBlockType();
	}

	static boolean isLog(BlockState blockState) {
		return ((TreeBlock) blockState.getBlock()).isLog();
	}
}
