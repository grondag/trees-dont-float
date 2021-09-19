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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class LeafInfo implements ToIntFunction<BlockState> {
	private static final LeafInfo INVALID = new LeafInfo(null);

	public final int maxDistance;
	private final IntegerProperty prop;

	private LeafInfo(IntegerProperty prop) {
		this.prop = prop;
		maxDistance = prop == null ? 0 : prop.getPossibleValues().stream().max(Integer::compare).get();
	}

	private static final IdentityHashMap<Block, LeafInfo> MAP = new IdentityHashMap<>();

	public static LeafInfo get(Block block) {
		return MAP.computeIfAbsent(block, b -> {
			final Collection<Property<?>> props = b.getStateDefinition().getProperties();

			for (final Property<?> p : props) {
				if (p.getValueClass() == Integer.class && p.getName().equalsIgnoreCase("distance")) {
					return new LeafInfo((IntegerProperty) p);
				}
			}

			return INVALID;
		});
	}

	@Override
	public int applyAsInt(BlockState value) {
		return prop == null ? 1 : value.getValue(prop);
	}
}
