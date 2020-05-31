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

import static ca.stellardrift.confabricate.Confabricate.LOGGER;
import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import net.minecraft.SharedConstants;
import net.minecraft.datafixer.Schemas;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.transformation.ConfigurationTransformation;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A transformation that exposes a single DataFixer to a configuration in a
 * friendly way.
 *
 * <p>Because Configurate does not have a schema model and DFU does, this
 * transformation works by explicitly providing a mapping between configurate
 * node paths and DFU TypeReferences.
 *
 * <p>For working with Minecraft data, the {@link #minecraftDfuBuilder()} method
 * will provide an appropriately configured builder that only needs to be
 * populated with type definitions.
 */
public final class DataFixerTransformation extends ConfigurationTransformation {

    private final List<?> versionPath;
    private final int targetVersion;
    private final ConfigurationTransformation wrapped;
    private final ThreadLocal<Integer> versionHolder;

    /**
     * Create a builder that can work with any DFU DataFixer.
     *
     * @return the builder
     */
    public static Builder dfuBuilder() {
        return new Builder();
    }

    /**
     * Return a builder pre-configured to apply Minecraft's DataFixers to the
     * latest game save version.
     *
     * @return new builder
     */
    public static Builder minecraftDfuBuilder() {
        return new Builder()
                .setVersionPath("minecraft-data-version")
                .setDataFixer(Schemas.getFixer())
                // This seems to always be a bit higher than the latest declared schema.
                // Don't know why, but the rest of the game uses this version.
                .setTargetVersion(SharedConstants.getGameVersion().getWorldVersion());
    }

    DataFixerTransformation(final List<?> versionPath, final int targetVersion, final ConfigurationTransformation wrapped,
            final ThreadLocal<Integer> versionHolder) {
        this.versionPath = ImmutableList.copyOf(versionPath);
        this.targetVersion = targetVersion;
        this.wrapped = wrapped;
        this.versionHolder = versionHolder;
    }

    @Override
    public void apply(@NonNull final ConfigurationNode node) {
        final ConfigurationNode versionNode = node.getNode(this.versionPath);
        final int currentVersion = versionNode.getInt(-1);
        if (currentVersion < this.targetVersion) {
            this.versionHolder.set(currentVersion);
            this.wrapped.apply(node);
            versionNode.setValue(this.targetVersion);
        } else if (currentVersion > this.targetVersion) {
            LOGGER.warn("Version in node {} (v{}) is higher than latest available version (v{}). Downgrades are not supported!",
                Arrays.toString(node.getPath()), currentVersion, this.targetVersion);
        }
    }

    /**
     * Path of the node (relative to starting node) that holds the current
     * schema version.
     *
     * @return path to version
     */
    public List<?> getVersionPath() {
        return this.versionPath;
    }

    /**
     * Get the version from a specific configuration node, using the configured
     * {@linkplain #getVersionPath() version path}.
     *
     * @param root Base node to query
     * @return version, or -1 if this node is unversioned.
     */
    public int getVersion(final ConfigurationNode root) {
        return requireNonNull(root, "root").getNode(getVersionPath()).getInt(-1);
    }

    /**
     * Builder for {@link DataFixerTransformation}.
     */
    public static class Builder {
        private List<?> versionPath = Collections.singletonList("dfu-version");
        private int targetVersion = -1;
        private DataFixer fixer;
        private final Set<Pair<DSL.TypeReference, Object[]>> dataFixes = new HashSet<>();

        /**
         * Set the fixer to use to process.
         *
         * @param fixer the fixer
         * @return this
         */
        public Builder setDataFixer(final DataFixer fixer) {
            this.fixer = requireNonNull(fixer);
            return this;
        }

        /**
         * Set the path of the node to query and store the node's schema
         * version at.
         *
         * @param path the path
         * @return this
         */
        public Builder setVersionPath(final Object... path) {
            this.versionPath = ImmutableList.copyOf(requireNonNull(path, "path"));
            return this;
        }

        /**
         * Set the desired target version. If none is specified, the newest
         * available version will be determined from the DataFixer.
         *
         * @param targetVersion target version
         * @return this
         */
        public Builder setTargetVersion(final int targetVersion) {
            this.targetVersion = targetVersion;
            return this;
        }

        public Builder type(final DSL.TypeReference type, final Object... path) {
            this.dataFixes.add(Pair.of(type, path));
            return this;
        }

        /**
         * Create a new transformation based on the provided info.
         *
         * @return new transformation
         */
        public DataFixerTransformation build() {
            requireNonNull(this.fixer, "A fixer must be provided!");
            if (this.targetVersion == -1) {
                // DataFixer gets a schema by subsetting the sorted list of schemas with (0, version + 1), so we do max int - 1 to avoid overflow
                this.targetVersion = DataFixUtils.getVersion(this.fixer.getSchema(Integer.MAX_VALUE - 1).getVersionKey());
            }
            final ConfigurationTransformation.Builder wrappedBuilder = ConfigurationTransformation.builder();
            final ThreadLocal<Integer> versionHolder = new ThreadLocal<>();
            for (Pair<DSL.TypeReference, Object[]> fix : this.dataFixes) {
                wrappedBuilder.addAction(fix.getSecond(), (path, valueAtPath) -> {
                    valueAtPath.setValue(this.fixer.update(fix.getFirst(), ConfigurateOps.wrap(valueAtPath),
                            versionHolder.get(), this.targetVersion).getValue());
                    return null;
                });
            }
            return new DataFixerTransformation(this.versionPath, this.targetVersion, wrappedBuilder.build(), versionHolder);
        }

    }

}
