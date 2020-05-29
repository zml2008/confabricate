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
import net.minecraft.util.Identifier;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializerCollection;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;
import ninja.leaping.configurate.reference.ConfigurationReference;
import ninja.leaping.configurate.reference.WatchServiceListener;
import ninja.leaping.configurate.transformation.ConfigurationTransformation;
import ninja.leaping.configurate.transformation.TransformAction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Configurate integration holder, providing access to configuration loaders
 * pre-configured to work with Minecraft types.
 *
 * <p>This class has static utility methods for usage by other mods -- it should
 * not be instantiated by anyone but the mod loader.
 */
public class Confabricate implements ModInitializer {

    static final String MOD_ID = "confabricate";

    private static Confabricate instance;
    static final Logger LOGGER = LogManager.getLogger();

    private WatchServiceListener listener;

    /**
     * Constructor for loader usage only.
     */
    public Confabricate() {
        if (instance != null) {
            throw new ExceptionInInitializerError("Confabricate can only be initialized by the Fabric mod loader");
        }
        instance = this;
    }

    @RestrictedApi(explanation = "confabricate namespace is not open to others",
            link = "", allowedOnPath = ".*/ca/stellardrift/confabricate/.*")
    public static Identifier id(final String item) {
        return new Identifier(MOD_ID, item);
    }

    @Override
    public void onInitialize() {
        try {
            this.listener = WatchServiceListener.create();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    this.listener.close();
                } catch (final IOException e) {
                    LOGGER.catching(e);
                }
            }, "Confabricate shutdown thread"));
        } catch (final IOException e) {
            LOGGER.error("Could not initialize file listener", e);
        }

        // initialize serializers early, fail fast
        MinecraftSerializers.collection();

        // Commands for testing
        // CommandRegistry.INSTANCE.register(false, TestCommands::register);
    }

    /**
     * Get a {@link TypeSerializerCollection} which contains additional
     * {@link TypeSerializers} for Minecraft types, in addition to the defaults
     * provided by Configurate.
     *
     * <p>While the collection is mutable, modifying it is discouraged in favor
     * of working with a new child, created with
     * {@link TypeSerializerCollection#newChild()}. Collections of serializers
     * will become immutable in Configurate 4.0
     *
     * @return Confabricate's collection of serializers.
     * @deprecated Use {@link MinecraftSerializers#collection()} instead
     */
    @Deprecated
    public static TypeSerializerCollection getMinecraftTypeSerializers() {
        return MinecraftSerializers.collection();
    }

    /**
     * Create a configuration loader for the given mod's main
     * configuration file.
     *
     * <p>By default, this config file is in a dedicated directory for the mod.
     *
     * @see #createLoaderFor(ModContainer, boolean)
     * @param mod The mod wanting to access its config
     * @return A configuration loader in the Hocon format
     */
    public static ConfigurationLoader<CommentedConfigurationNode> createLoaderFor(final ModContainer mod) {
        return createLoaderFor(mod, true);
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
     * @param mod The mod to get the configuration loader for
     * @param ownDirectory Whether the configuration should be in a directory
     *                     just for the mod, or a file in the config root
     * @return The newly created configuration loader
     */
    public static ConfigurationLoader<CommentedConfigurationNode> createLoaderFor(final ModContainer mod, final boolean ownDirectory) {
        return HoconConfigurationLoader.builder()
                .setPath(getConfigurationFile(mod, ownDirectory))
                .setDefaultOptions(o -> o.withSerializers(MinecraftSerializers.collection()))
                .build();
    }

    /**
     * Create a configuration reference to the provided mod's main
     * configuration file.
     *
     * <p>By default, this config file is in a dedicated directory for the mod.
     * The returned reference will automatically reload.
     *
     * @see #createConfigurationFor(ModContainer, boolean)
     * @param mod The mod wanting to access its config
     * @return A configuration reference for a loaded node in HOCON format
     * @throws IOException if a listener could not be established or if the
     *                     configuration failed to load
     */
    public static ConfigurationReference<CommentedConfigurationNode> createConfigurationFor(final ModContainer mod) throws IOException {
        return createConfigurationFor(mod, true);
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
     * @param mod The mod to get the configuration loader for
     * @param ownDirectory Whether the configuration should be in a directory
     *                     just for the mod
     * @return The newly created and loaded configuration reference
     * @throws IOException if a listener could not be established or the
     *                     configuration failed to load.
     */
    public static ConfigurationReference<CommentedConfigurationNode> createConfigurationFor(final ModContainer mod,
            final boolean ownDirectory) throws IOException {
        return getFileWatcher().listenToConfiguration(path -> {
            return HoconConfigurationLoader.builder()
                    .setPath(path)
                    .setDefaultOptions(o -> o.withSerializers(MinecraftSerializers.collection()))
                    .build();
        }, getConfigurationFile(mod, ownDirectory));
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
     */
    public static Path getConfigurationFile(final ModContainer mod, final boolean ownDirectory) {
        Path configRoot = FabricLoader.getInstance().getConfigDirectory().toPath();
        if (ownDirectory) {
            configRoot = configRoot.resolve(mod.getMetadata().getId());
        }
        return configRoot.resolve(mod.getMetadata().getId() + ".conf");
    }

    /**
     * See static variant.
     *
     * @param fixer The fixer containing DFU transformations to apply
     * @param reference The reference to the DFU {@link DSL} type representing this node
     * @param targetVersion The version to convert to
     * @param versionKey The location of the data version in nodes provided to
     *                  the transformer
     * @return A transformation that executes a {@link DataFixer} transformation.
     * @deprecated see {@link #createTransformation(DataFixer, DSL.TypeReference, int, Object...)}, this should have been static
     */
    @Deprecated
    public ConfigurationTransformation createTransformationFrom(final DataFixer fixer, final DSL.TypeReference reference,
            final int targetVersion, final Object... versionKey) {
        return createTransformation(fixer, reference, targetVersion, versionKey);
    }

    /**
     * See static variant.
     *
     * @param fixer The fixer containing DFU transformations to apply
     * @param reference The reference to the DFU {@link DSL} type representing
     *                  this node
     * @param targetVersion The version to convert to
     * @param versionKey The location of the data version in nodes provided to
     *                   the transformer
     * @return A transformation that executes a {@link DataFixer} transformation.
     * @deprecated {@link #createTransformAction(DataFixer, DSL.TypeReference, int, Object...)}, this should have been static
     */
    @Deprecated
    public TransformAction createTransformActionFrom(final DataFixer fixer, final DSL.TypeReference reference,
            final int targetVersion, final Object... versionKey) {
        return createTransformAction(fixer, reference, targetVersion, versionKey);
    }

    /**
     * Create a {@link ConfigurationTransformation} that applies a
     * {@link DataFixer} to a Configurate node. The current version of the node
     * is provided by the path {@code versionKey}. The transformation is
     * executed from the provided node.
     *
     * @param fixer The fixer containing DFU transformations to apply
     * @param reference The reference to the DFU {@link DSL} type representing
     *                  this node
     * @param targetVersion The version to convert to
     * @param versionKey The location of the data version in nodes provided to
     *                   the transformer
     * @return A transformation that executes a {@link DataFixer} transformation.
     */
    public static ConfigurationTransformation createTransformation(final DataFixer fixer, final DSL.TypeReference reference, final int targetVersion,
            final Object... versionKey) {
        return ConfigurationTransformation.builder()
                .addAction(new Object[]{}, createTransformAction(fixer, reference, targetVersion, versionKey))
                .build();

    }

    /**
     * Create a TransformAction applying a {@link DataFixer} to a Configurate
     * node. This can be used within {@link ConfigurationTransformation}
     * when some values are controlled by DFUs and some aren't.
     *
     * @param fixer The fixer containing DFU transformations to apply
     * @param reference The reference to the DFU {@link DSL} type representing this node
     * @param targetVersion The version to convert to
     * @param versionKey The location of the data version in nodes seen by
     *                  this action.
     * @return The created action
     */
    public static TransformAction createTransformAction(final DataFixer fixer, final DSL.TypeReference reference,
            final int targetVersion, final Object... versionKey) {
        return (inputPath, valueAtPath) -> {
            final int currentVersion = valueAtPath.getNode(versionKey).getInt(-1);
            final Dynamic<ConfigurationNode> dyn = ConfigurateOps.wrap(valueAtPath);
            valueAtPath.setValue(fixer.update(reference, dyn, currentVersion, targetVersion).getValue());
            return null;
        };
    }

    /**
     * Access the shared watch service for listening to files in this game on
     * the default filesystem.
     *
     * @return watcher
     */
    public static WatchServiceListener getFileWatcher() {
        final WatchServiceListener ret = instance.listener;
        if (ret == null) {
            throw new IllegalStateException("Configurate file watcher failed to initialize, check log for earlier errors");
        }
        return ret;
    }

}
