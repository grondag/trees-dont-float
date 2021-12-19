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
		maxDistance = prop == null ? 0 : prop.getPossibleValues().stream().max(Integer::compare).orElse(0);
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
