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

import net.minecraft.block.BlockState;

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
