package grondag.tdnf;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ModInitializer;

public class TreesDoNotFloat implements ModInitializer {
    @Override
    public void onInitialize() {
        Dispatcher.init();
        Configurator.init();
    }
    
    public static final String MODID = "trees-do-not-float";
    
    public static final Logger LOG = LogManager.getLogger("trees-do-not-float");
}
