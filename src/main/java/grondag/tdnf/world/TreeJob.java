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

import java.util.concurrent.ArrayBlockingQueue;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public class TreeJob {
    private TreeJob() {}
    
    private long startPos;
    private ServerPlayerEntity player;
    private ItemStack stack;
    
    /** packed staring pos */
    public long startPos() {
        return startPos;
    }
    
    /** player who initiated the break, if known */
    public ServerPlayerEntity player() {
        return player;
    }
    
    /** stack used by player who initiated the break, if known */
    public ItemStack stack() {
        return stack;
    }
    
    public void release() {
        this.player = null;
        this.stack = null;
        POOL.offer(this);
    }
    
    private static final ArrayBlockingQueue<TreeJob> POOL = new ArrayBlockingQueue<>(512);
    
    public static TreeJob claim(long startPos, ServerPlayerEntity player, ItemStack stack) {
        TreeJob result = POOL.poll();
        if (result == null) {
            result = new TreeJob();
        }
        result.startPos = startPos;
        result.player = player;
        result.stack = stack;
        return result;
    }
}
