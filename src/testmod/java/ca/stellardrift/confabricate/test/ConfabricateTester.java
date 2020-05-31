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

package ca.stellardrift.confabricate.test;

import static ninja.leaping.configurate.transformation.ConfigurationTransformation.WILDCARD_OBJECT;

import ca.stellardrift.confabricate.Confabricate;
import ca.stellardrift.confabricate.DataFixerTransformation;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import ninja.leaping.configurate.reference.ConfigurationReference;
import ninja.leaping.configurate.reference.ValueReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A test mod that uses an auto-reloadable configuration.
 */
public class ConfabricateTester implements ModInitializer {

    static final Logger LOGGER = LogManager.getLogger();
    private static ConfabricateTester instance;

    private @MonotonicNonNull ConfigurationReference<CommentedConfigurationNode> configFile;
    private @MonotonicNonNull ValueReference<TestmodConfig> config;

    /**
     * Get the active mod instance, throwing an {@link IllegalStateException} if
     * called too early in mod initialization.
     *
     * @return mod singleton
     */
    public static ConfabricateTester instance() {
        final ConfabricateTester ret = instance;
        if (ret == null) {
            throw new IllegalStateException("Confabricate tester has not yet been initialized!");
        }
        return ret;
    }

    @Override
    public void onInitialize() {
        instance = this;
        LOGGER.info("Confabricate test mod loaded");
        final ModContainer container = FabricLoader.getInstance().getModContainer("confabricate-testmod")
                        .orElseThrow(() -> new IllegalArgumentException("Could not find container for testmod"));

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            TestCommands.register(this, dispatcher);
        });

        // Load config
        try {
            this.configFile = Confabricate.createConfigurationFor(container);
            // Handle updating with game changes
            final ConfigurationNode node = this.configFile.getNode();
            final DataFixerTransformation xform = DataFixerTransformation.minecraftDfuBuilder()
                    .type(TypeReferences.ITEM_STACK, "items", WILDCARD_OBJECT) // every child of "items" should be upgraded as an ItemStack
                    .build();

            final boolean wasEmpty = node.isEmpty();
            final int oldVersion = xform.getVersion(node);
            xform.apply(node);
            final int newVersion = xform.getVersion(node);
            if (newVersion > oldVersion && !wasEmpty) {
                LOGGER.info("Updated configuration from version {} to {}", oldVersion, newVersion);
            }

            this.config = this.configFile.referenceTo(TestmodConfig.class);
            this.configFile.save();
        } catch (IOException | ObjectMappingException e) {
            throw new RuntimeException("Unable to load configuration for " + container.getMetadata().getId(), e);
        }

    }

    public TestmodConfig getConfiguration() {
        return this.config.get();
    }

    @ConfigSerializable
    public static class TestmodConfig {
        @Setting
        private Text message = new LiteralText("Welcome to the server!");

        @Setting
        private List<ItemStack> items = new ArrayList<>();

        {
            final ItemStack stack = new ItemStack(Items.STONE_SWORD, 3);
            stack.addEnchantment(Enchantments.SHARPNESS, 3);
            this.items.add(stack);
        }

        public Text getMessage() {
            return this.message;
        }

        public List<ItemStack> getItems() {
            return this.items;
        }
    }

}
