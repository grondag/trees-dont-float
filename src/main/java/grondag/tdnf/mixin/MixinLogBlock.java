package grondag.tdnf.mixin;

import org.spongepowered.asm.mixin.Mixin;

import grondag.tdnf.Dispatcher;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LogBlock;
import net.minecraft.block.MaterialColor;
import net.minecraft.block.PillarBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Mixin(LogBlock.class)
public abstract class MixinLogBlock extends PillarBlock {
    
    public MixinLogBlock(MaterialColor materialColor_1, Block.Settings block$Settings_1) {
        super(block$Settings_1);
    }

    @Override
    public void onBlockRemoved(BlockState oldState, World world, BlockPos blockPos, BlockState newState, boolean notify) {
       if (oldState.getBlock() != newState.getBlock()) {
           Dispatcher.enqueCheck(world, blockPos);
       }
       super.onBlockRemoved(oldState, world, blockPos, newState, notify);
    }
}
