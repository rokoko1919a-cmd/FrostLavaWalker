package net.nanaky.frost_lava_walker.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import net.nanaky.frost_lava_walker.config.ConfigManager;
import net.nanaky.frost_lava_walker.config.LavaWalkerConfig;

public class ClothConfigScreenFactory {

    public static ConfigScreenFactory<?> create() {
        return parent -> {
            LavaWalkerConfig cfg = ConfigManager.INSTANCE;
            LavaWalkerConfig def = new LavaWalkerConfig();

            ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.frost_lava_walker.title"))
                .setSavingRunnable(ConfigManager::save);

            ConfigEntryBuilder eb = builder.entryBuilder();

            ConfigCategory client = builder.getOrCreateCategory(
                Component.translatable("config.frost_lava_walker.category.client"));

            client.addEntry(eb.startBooleanToggle(
                    Component.translatable("config.frost_lava_walker.show_particles"), cfg.showParticles)
                .setDefaultValue(def.showParticles)
                .setTooltip(Component.translatable("config.frost_lava_walker.show_particles.tooltip"))
                .setSaveConsumer(v -> cfg.showParticles = v)
                .build());

            client.addEntry(eb.startBooleanToggle(
                    Component.translatable("config.frost_lava_walker.play_sound_effects"), cfg.playSoundEffects)
                .setDefaultValue(def.playSoundEffects)
                .setTooltip(Component.translatable("config.frost_lava_walker.play_sound_effects.tooltip"))
                .setSaveConsumer(v -> cfg.playSoundEffects = v)
                .build());

            ConfigCategory server = builder.getOrCreateCategory(
                Component.translatable("config.frost_lava_walker.category.server"));

            server.addEntry(eb.startBooleanToggle(
                    Component.translatable("config.frost_lava_walker.lava_walker_enabled"), cfg.lavaWalkerEnabled)
                .setDefaultValue(def.lavaWalkerEnabled)
                .setTooltip(Component.translatable("config.frost_lava_walker.lava_walker_enabled.tooltip"))
                .setSaveConsumer(v -> cfg.lavaWalkerEnabled = v)
                .build());

            server.addEntry(eb.startIntSlider(
                    Component.translatable("config.frost_lava_walker.base_radius"), cfg.baseRadius, 0, 9)
                .setDefaultValue(def.baseRadius)
                .setTooltip(Component.translatable("config.frost_lava_walker.base_radius.tooltip"))
                .setSaveConsumer(v -> cfg.baseRadius = v)
                .build());

            server.addEntry(eb.startIntSlider(
                    Component.translatable("config.frost_lava_walker.gilded_initial_ticks"), cfg.gildedInitialTicks, 1, 40)
                .setDefaultValue(def.gildedInitialTicks)
                .setTooltip(Component.translatable("config.frost_lava_walker.gilded_initial_ticks.tooltip"))
                .setSaveConsumer(v -> cfg.gildedInitialTicks = v)
                .build());

            server.addEntry(eb.startIntSlider(
                    Component.translatable("config.frost_lava_walker.blackstone_main_ticks"), cfg.blackstoneMainTicks, 1, 200)
                .setDefaultValue(def.blackstoneMainTicks)
                .setTooltip(Component.translatable("config.frost_lava_walker.blackstone_main_ticks.tooltip"))
                .setSaveConsumer(v -> cfg.blackstoneMainTicks = v)
                .build());

            server.addEntry(eb.startIntSlider(
                    Component.translatable("config.frost_lava_walker.gilded_warning_ticks"), cfg.gildedWarningTicks, 1, 40)
                .setDefaultValue(def.gildedWarningTicks)
                .setTooltip(Component.translatable("config.frost_lava_walker.gilded_warning_ticks.tooltip"))
                .setSaveConsumer(v -> cfg.gildedWarningTicks = v)
                .build());

            server.addEntry(eb.startIntSlider(
                    Component.translatable("config.frost_lava_walker.magma_short_ticks"), cfg.magmaShortTicks, 1, 40)
                .setDefaultValue(def.magmaShortTicks)
                .setTooltip(Component.translatable("config.frost_lava_walker.magma_short_ticks.tooltip"))
                .setSaveConsumer(v -> cfg.magmaShortTicks = v)
                .build());

            server.addEntry(eb.startIntSlider(
                    Component.translatable("config.frost_lava_walker.cooldown_extra_ticks"), cfg.cooldownExtraTicks, 0, 200)
                .setDefaultValue(def.cooldownExtraTicks)
                .setTooltip(Component.translatable("config.frost_lava_walker.cooldown_extra_ticks.tooltip"))
                .setSaveConsumer(v -> cfg.cooldownExtraTicks = v)
                .build());

            return builder.build();
        };
    }
}