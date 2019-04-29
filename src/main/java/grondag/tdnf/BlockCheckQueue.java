package grondag.smart_chest;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.util.math.BlockPos;

/**
 * Maintains ordered queue of packed block positions that need handling.
 * Will not add a position if it is already in the queue.
 */
public abstract class BlockCheckQueue 
{
    private final LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
    
    private final LongOpenHashSet set = new LongOpenHashSet();
    
    /**
     * Use in all cases when mutable is appropriate. This class is not intended
     * to be threadsafe and avoids recursion, so re-entrancy should not be a problem.
     */
    protected final BlockPos.Mutable searchPos = new BlockPos.Mutable();
    
    public void enqueueCheck(long packedBlockPos)
    {
        if(set.add(packedBlockPos))
            queue.enqueue(packedBlockPos);
    }
    
    protected boolean isEmpty()
    {
        return this.queue.isEmpty();
    }
    
    protected int size()
    {
        return this.queue.size();
    }
    
    protected long dequeueCheck()
    {
        long result = queue.dequeueLong();
        set.rem(result);
        return result;
    }
}
