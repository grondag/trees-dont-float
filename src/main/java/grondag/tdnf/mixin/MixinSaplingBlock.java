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

package grondag.tdnf.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.common.base.Predicates;

import grondag.tdnf.world.Dispatcher;
import net.minecraft.block.SaplingBlock;

@Mixin(SaplingBlock.class)
public class MixinSaplingBlock {
    @Inject(at = @At("HEAD"), method = "generate")
    private void beforeGenerate(CallbackInfo ci) {
        Dispatcher.suspend(Predicates.alwaysFalse());
    }

    @Inject(at = @At("RETURN"), method = "generate")
    private void afterGenerate(CallbackInfo ci) {
        Dispatcher.resume();
    }
}
