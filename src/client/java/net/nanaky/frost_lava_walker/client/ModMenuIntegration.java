package net.nanaky.frost_lava_walker.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (!FabricLoader.getInstance().isModLoaded("cloth-config")) {
            return parent -> null; // No config screen without ClothConfig
        }
        // Delegate to a separate class so ClothConfig classes are only
        // loaded if ClothConfig is actually present
        return ClothConfigScreenFactory.create();
    }
}