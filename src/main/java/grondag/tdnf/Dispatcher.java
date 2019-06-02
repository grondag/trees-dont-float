package grondag.tdnf;

import java.util.IdentityHashMap;

import grondag.fermion.world.PackedBlockPos;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import net.fabricmc.fabric.api.event.world.WorldTickCallback;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class Dispatcher {
    
    private static final long NO_JOB = -1L;
    
    public static void init() {
        WorldTickCallback.EVENT.register(Dispatcher::routeTick);
    }
    
    private static class WorldJobs {
        private final LongArrayFIFOQueue jobList = new LongArrayFIFOQueue();
        private TreeCutter cutter = null;
        private long currentJob = NO_JOB;
        
        TreeCutter cutter() {
            TreeCutter result = cutter;
            if(result == null) {
                result = new TreeCutter();
                cutter = result;
            }
            return result;
        }

        public void run(World world) {
            long result = currentJob;
            if(result == NO_JOB & !jobList.isEmpty()) {
                result = jobList.dequeueLong();
                currentJob = result;
                cutter().reset(result);
            }
            
            if(result != NO_JOB) {
                if(cutter.tick(world)) {
                    currentJob = NO_JOB;
                };
            }
        }
    }
    
    private static final IdentityHashMap<World, WorldJobs> worldJobs = new IdentityHashMap<>();
    
    public static void routeTick(World world) {
        if(world.isClient) {
            return;
        };
        
        WorldJobs jobs = worldJobs.get(world);
        if(jobs == null) {
            return;
        }
        jobs.run(world);
    }
    
    public static void enqueCheck(World world, BlockPos pos) {
        if(world.isClient) {
            return;
        }
        
        WorldJobs jobs = worldJobs.get(world);
        if(jobs == null) {
            jobs = new WorldJobs();
            worldJobs.put(world, jobs);
        }
        
        jobs.jobList.enqueue(PackedBlockPos.pack(pos.getX(), pos.getY() + 1, pos.getZ()));
    }
}
