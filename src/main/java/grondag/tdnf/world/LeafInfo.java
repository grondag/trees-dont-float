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

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.function.ToIntFunction;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Property;

public class LeafInfo implements ToIntFunction<BlockState> {
	private static final LeafInfo INVALID = new LeafInfo(null);

	public final int maxDistance;
	private final IntProperty prop;

	private LeafInfo(IntProperty prop) {
		this.prop = prop;
		maxDistance = prop == null ? 0 : prop.getValues().stream().max(Integer::compare).get();
	}

	private static final IdentityHashMap<Block, LeafInfo> MAP = new IdentityHashMap<>();

	public static LeafInfo get(Block block) {
		return MAP.computeIfAbsent(block, b -> {
			final Collection<Property<?>> props = b.getStateManager().getProperties();

			for (final Property<?> p : props) {
				if (p.getType() == Integer.class && p.getName().equalsIgnoreCase("distance")) {
					return new LeafInfo((IntProperty) p);
				}
			}

			return INVALID;
		});
	}

	@Override
	public int applyAsInt(BlockState value) {
		return prop == null ? 1 : value.get(prop);
	}
}
