/*
 * Copyright 2020 zml
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.stellardrift.confabricate;

import ca.stellardrift.confabricate.typeserializers.MinecraftSerializers;
import com.google.errorprone.annotations.RestrictedApi;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.NodePath;
import org.spongepowered.configurate.extra.dfu.v4.ConfigurateOps;
import org.spongepowered.configurate.extra.dfu.v4.DataFixerTransformation;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.reference.ConfigurationReference;
import org.spongepowered.configurate.reference.WatchServiceListener;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;
import org.spongepowered.configurate.transformation.TransformAction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configurate integration holder, providing access to configuration loaders
 * pre-configured to work with Minecraft types.
 *
 * <p>This class has static utility methods for usage by other mods -- it should
 * not be instantiated by anyone but the mod loader.
 *
 * @since 1.0.0
 */
public class Confabricate implements ModInitializer {

    static final String MOD_ID = "confabricate";

    static final Logger LOGGER = LogManager.getLogger();

    private static WatchServiceListener listener;

    static {
        try {
            Confabricate.listener = WatchServiceListener.create();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    Confabricate.listener.close();
                } catch (final IOException e) {
                    LOGGER.catching(e);
                }
            }, "Confabricate shutdown thread"));
        } catch (final IOException e) {
            LOGGER.error("Could not initialize file listener", e);
        }
    }

    /**
     * Internal API to get a mod {@link ResourceLocation}.
     *
     * @param item path value
     * @return new identifier
     * @since 2.0.0
     */
    @RestrictedApi(explanation = "confabricate namespace is not open to others",
            link = "", allowedOnPath = ".*/ca/stellardrift/confabricate/.*")
    public static ResourceLocation id(final String item) {
        return new ResourceLocation(MOD_ID, item);
    }

    @Override
    public void onInitialize() {
        // initialize serializers early, fail fast
        MinecraftSerializers.collection();
    }

    /**
     * Get configuration options configured to use Confabricate's serializers.
     *
     * @return customized options
     * @since 2.0.0
     */
    public static ConfigurationOptions confabricateOptions() {
        return ConfigurationOptions.defaults()
                .serializers(MinecraftSerializers.collection());
    }

    /**
     * Create a configuration loader for the given mod's main
     * configuration file.
     *
     * <p>By default, this config file is in a dedicated directory for the mod.
     *
     * @param mod the mod wanting to access its config
     * @return a configuration loader in the Hocon format
     * @see #loaderFor(ModContainer, boolean, ConfigurationOptions)
     * @since 1.0.0
     */
    public static ConfigurationLoader<CommentedConfigurationNode> loaderFor(final ModContainer mod) {
        return loaderFor(mod, true);
    }

    /**
     * Get a configuration loader for a mod. The configuration will be in
     * Hocon format.
     *
     * <p>If the configuration is in its own directory, the path will be
     * <pre>&lt;config root&gt;/&lt;modid&gt;/&lt;modid&gt;.conf</pre>.
     * Otherwise, the path will be
     * <pre>&lt;config root&gt;/&lt;modid&gt;.conf</pre>.
     *
     * <p>The returned {@link ConfigurationLoader ConfigurationLoaders} will be
     * pre-configured to use the type serializers from
     * {@link MinecraftSerializers#collection()}, but will otherwise use
     * default settings.
     *
     * @param mod the mod to get the configuration loader for
     * @param ownDirectory whether the configuration should be in a directory
     *                     just for the mod, or a file in the config root
     * @return the newly created configuration loader
     * @since 1.0.0
     */
    public static ConfigurationLoader<CommentedConfigurationNode> loaderFor(final ModContainer mod, final boolean ownDirectory) {
        return loaderFor(mod, ownDirectory, confabricateOptions());
    }

    /**
     * Get a configuration loader for a mod. The configuration will be in
     * Hocon format.
     *
     * <p>If the configuration is in its own directory, the path will be
     * <pre>&lt;config root&gt;/&lt;modid&gt;/&lt;modid&gt;.conf</pre>.
     * Otherwise, the path will be
     * <pre>&lt;config root&gt;/&lt;modid&gt;.conf</pre>.
     *
     * <p>The returned {@link ConfigurationLoader ConfigurationLoaders} will be
     * pre-configured to use the type serializers from
     * {@link MinecraftSerializers#collection()}, but will otherwise use
     * default settings.
     *
     * @param mod the mod to get the configuration loader for
     * @param ownDirectory whether the configuration should be in a directory
     *                     just for the mod, or a file in the config root
     * @param options the options to use by default when loading
     * @return the newly created configuration loader
     * @since 2.0.0
     */
    public static ConfigurationLoader<CommentedConfigurationNode> loaderFor(
            final ModContainer mod,
            final boolean ownDirectory,
            final ConfigurationOptions options) {
        return HoconConfigurationLoader.builder()
                .path(configurationFile(mod, ownDirectory))
                .defaultOptions(options)
                .build();

    }

    /**
     * Create a configuration reference to the provided mod's main
     * configuration file.
     *
     * <p>By default, this config file is in a dedicated directory for the mod.
     * The returned reference will automatically reload.
     *
     * @param mod the mod wanting to access its config
     * @return a configuration reference for a loaded node in HOCON format
     * @throws ConfigurateException if a listener could not be established or if
     *                      the configuration failed to load.
     * @see #configurationFor(ModContainer, boolean, ConfigurationOptions)
     * @since 1.1.0
     */
    public static ConfigurationReference<CommentedConfigurationNode> configurationFor(final ModContainer mod) throws ConfigurateException {
        return configurationFor(mod, true);
    }

    /**
     * Get a configuration reference for a mod. The configuration will be in
     * Hocon format.
     *
     * <p>If the configuration is in its own directory, the path will be
     * <pre>&lt;config root&gt;/&lt;modid&gt;/&lt;modid&gt;.conf</pre>
     * Otherwise, the path will be
     * <pre>&lt;config root&gt;/&lt;modid&gt;.conf</pre>.
     *
     * <p>The reference's {@link ConfigurationLoader} will be pre-configured to
     * use the type serializers from {@link MinecraftSerializers#collection()}
     * but will otherwise use default settings.
     *
     * @param mod the mod to get the configuration loader for
     * @param ownDirectory whether the configuration should be in a directory
     *                     just for the mod
     * @return the newly created and loaded configuration reference
     * @throws ConfigurateException if a listener could not be established or
     *                              the configuration failed to load.
     * @since 1.1.0
     */
    public static ConfigurationReference<CommentedConfigurationNode> configurationFor(
            final ModContainer mod,
            final boolean ownDirectory) throws ConfigurateException {
        return configurationFor(mod, ownDirectory, confabricateOptions());
    }

    /**
     * Get a configuration reference for a mod. The configuration will be in
     * Hocon format.
     *
     * <p>If the configuration is in its own directory, the path will be
     * <pre>&lt;config root&gt;/&lt;modid&gt;/&lt;modid&gt;.conf</pre>
     * Otherwise, the path will be
     * <pre>&lt;config root&gt;/&lt;modid&gt;.conf</pre>.
     *
     * <p>The reference's {@link ConfigurationLoader} will be pre-configured to
     * use the type serializers from {@link MinecraftSerializers#collection()}
     * but will otherwise use default settings.
     *
     * @param mod the mod to get the configuration loader for
     * @param ownDirectory whether the configuration should be in a directory
     *                     just for the mod
     * @param options the options to use by default when loading
     * @return the newly created and loaded configuration reference
     * @throws ConfigurateException if a listener could not be established or
     *                              the configuration failed to load.
     * @since 2.0.0
     */
    public static ConfigurationReference<CommentedConfigurationNode> configurationFor(
            final ModContainer mod,
            final boolean ownDirectory,
            final ConfigurationOptions options) throws ConfigurateException {
        return fileWatcher().listenToConfiguration(path -> {
            return HoconConfigurationLoader.builder()
                    .path(path)
                    .defaultOptions(options)
                    .build();
        }, configurationFile(mod, ownDirectory));
    }

    /**
     * Get the path to a configuration file in HOCON format for {@code mod}.
     *
     * <p>HOCON uses the {@code .conf} file extension.
     *
     * @param mod container of the mod
     * @param ownDirectory whether the configuration should be in its own
     *                  directory, or in the main configuration directory
     * @return path to a configuration file
     * @since 1.1.0
     */
    public static Path configurationFile(final ModContainer mod, final boolean ownDirectory) {
        Path configRoot = FabricLoader.getInstance().getConfigDir();
        if (ownDirectory) {
            configRoot = configRoot.resolve(mod.getMetadata().getId());
        }
        try {
            Files.createDirectories(configRoot);
        } catch (final IOException ignore) {
            // we tried
        }
        return configRoot.resolve(mod.getMetadata().getId() + ".conf");
    }

    /**
     * Create a {@link ConfigurationTransformation} that applies a
     * {@link DataFixer} to a Configurate node. The current version of the node
     * is provided by the path {@code versionKey}. The transformation is
     * executed from the provided node.
     *
     * @param fixer the fixer containing DFU transformations to apply
     * @param reference the reference to the DFU {@link DSL} type representing
     *                  this node
     * @param targetVersion the version to convert to
     * @param versionKey the location of the data version in nodes provided to
     *                   the transformer
     * @return a transformation that executes a {@link DataFixer data fixer}.
     * @since 1.1.0
     */
    public static ConfigurationTransformation createTransformation(
            final DataFixer fixer,
            final DSL.TypeReference reference,
            final int targetVersion,
            final Object... versionKey) {
        return ConfigurationTransformation.builder()
                .addAction(NodePath.path(), createTransformAction(fixer, reference, targetVersion, versionKey))
                .build();

    }

    /**
     * Create a TransformAction applying a {@link DataFixer} to a Configurate
     * node. This can be used within {@link ConfigurationTransformation}
     * when some values are controlled by DFUs and some aren't.
     *
     * @param fixer the fixer containing DFU transformations to apply
     * @param reference the reference to the DFU {@link DSL} type representing this node
     * @param targetVersion the version to convert to
     * @param versionKey the location of the data version in nodes seen by
     *                  this action.
     * @return the created action
     * @since 1.1.0
     */
    public static TransformAction createTransformAction(
            final DataFixer fixer,
            final DSL.TypeReference reference,
            final int targetVersion,
            final Object... versionKey) {
        return (inputPath, valueAtPath) -> {
            final int currentVersion = valueAtPath.node(versionKey).getInt(-1);
            final Dynamic<ConfigurationNode> dyn = ConfigurateOps.wrap(valueAtPath);
            valueAtPath.set(fixer.update(reference, dyn, currentVersion, targetVersion).getValue());
            return null;
        };
    }

    /**
     * Access the shared watch service for listening to files in this game on
     * the default filesystem.
     *
     * @return watcher
     * @since 1.1.0
     */
    public static WatchServiceListener fileWatcher() {
        final WatchServiceListener ret = Confabricate.listener;
        if (ret == null) {
            throw new IllegalStateException("Configurate file watcher failed to initialize, check log for earlier errors");
        }
        return ret;
    }

    /**
     * Return a builder pre-configured to apply Minecraft's DataFixers to the
     * latest game save version.
     *
     * @return new transformation builder
     * @since 2.0.0
     */
    public static DataFixerTransformation.Builder minecraftDfuBuilder() {
        return DataFixerTransformation.dfuBuilder()
                .versionKey("minecraft-data-version")
                .dataFixer(DataFixers.getDataFixer())
                // This seems to always be a bit higher than the latest declared schema.
                // Don't know why, but the rest of the game uses this version.
                .targetVersion(SharedConstants.getCurrentVersion().getDataVersion().getVersion());
    }

}
