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

import ca.stellardrift.confabricate.Confabricate;
import ca.stellardrift.confabricate.typeserializers.MinecraftSerializers;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourceReloadListenerKeys;
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.extra.dfu.v4.DataFixerTransformation;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.reference.ConfigurationReference;
import org.spongepowered.configurate.reference.ValueReference;

import static org.spongepowered.configurate.transformation.ConfigurationTransformation.WILDCARD_OBJECT;

/**
 * A test mod that uses an auto-reloadable configuration.
 */
public class ConfabricateTester implements ModInitializer {

    static final Logger LOGGER = LogManager.getLogger();
    private static ConfabricateTester instance;

    private @MonotonicNonNull ConfigurationReference<CommentedConfigurationNode> configFile;
    private @MonotonicNonNull ValueReference<TestmodConfig, CommentedConfigurationNode> config;

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


        // Load config
        try {
            this.configFile = Confabricate.configurationFor(container, true, ConfigurationOptions.defaults()
                    .serializers(MinecraftSerializers.collection())
                    .shouldCopyDefaults(true)
                    .implicitInitialization(true));

            // Handle updating with game changes
            final CommentedConfigurationNode node = this.configFile.node();
            final DataFixerTransformation xform = Confabricate.minecraftDfuBuilder()
                    .addType(References.ITEM_STACK, "items", WILDCARD_OBJECT) // every child of "items" should be upgraded as an ItemStack
                    .build();

            final boolean wasEmpty = node.empty();
            final int oldVersion = xform.version(node);
            xform.apply(node);
            final int newVersion = xform.version(node);
            if (newVersion > oldVersion && !wasEmpty) {
                LOGGER.info("Updated configuration from version {} to {}", oldVersion, newVersion);
            }

            this.config = this.configFile.referenceTo(TestmodConfig.class);
            ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new SimpleResourceReloadListener<Void>() {
                @Override
                public ResourceLocation getFabricId() {
                    return new ResourceLocation("confabricate-testmod", "config");
                }

                @Override
                public CompletableFuture<Void> load(final ResourceManager manager, final ProfilerFiller profiler, final Executor executor) {
                    final CompletableFuture<Void> ret = new CompletableFuture<>();
                    executor.execute(() -> {
                        try {
                            ConfabricateTester.this.configFile.load();
                            ret.complete(null);
                        } catch (final ConfigurateException e) {
                            ret.completeExceptionally(e);
                        }
                    });
                    return ret;
                }

                @Override
                public CompletableFuture<Void> apply(
                    final Void data,
                    final ResourceManager manager,
                    final ProfilerFiller profiler,
                    final Executor executor
                ) {
                    return CompletableFuture.completedFuture(data);
                }

                @Override
                public Collection<ResourceLocation> getFabricDependencies() {
                    return Collections.singletonList(ResourceReloadListenerKeys.TAGS);
                }
            });
            this.configFile.save();
        } catch (final ConfigurateException e) {
            throw new RuntimeException("Unable to load configuration for " + container.getMetadata().getId(), e);
        }

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            TestCommands.register(this, dispatcher);
        });

        // Register protection events
        AttackBlockCallback.EVENT.register((player, world, hand, blockPos, direction) -> {
            if (!world.isClientSide && player instanceof ServerPlayer && hand == InteractionHand.MAIN_HAND) {
                final InteractionResult testAttack = this.configuration().protection().testAttackWith((ServerPlayer) player,
                                                                                                 player.getMainHandItem());

                if (testAttack != InteractionResult.PASS) {
                    return testAttack;
                }

                return this.configuration().protection().testBreak((ServerPlayer) player,
                                                                   world.getBlockState(blockPos).getBlock());
            }
            return InteractionResult.PASS;
        });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, result) -> {
            if (!world.isClientSide && player instanceof ServerPlayer && hand == InteractionHand.MAIN_HAND) {
                return this.configuration().protection().testAttackWith((ServerPlayer) player, player.getMainHandItem());
            }
            return InteractionResult.PASS;
        });
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClientSide && player instanceof ServerPlayer && hand == InteractionHand.MAIN_HAND) {
                return new InteractionResultHolder<>(this.configuration().protection().testUseItem((ServerPlayer) player,
                                                                                             player.getMainHandItem()), player.getMainHandItem());
            }
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        });
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClientSide && player instanceof ServerPlayer && hand == InteractionHand.MAIN_HAND) {
                final ItemStack heldItem = player.getMainHandItem();
                if (heldItem.getItem() instanceof BlockItem) {
                    final InteractionResult result = this.configuration().protection().testPlace((ServerPlayer) player,
                                                                                            ((BlockItem) heldItem.getItem()).getBlock());
                    if (result == InteractionResult.FAIL) { // if we can't place, then let's restore inventory
                        ((ServerPlayer) player).connection.send(
                            new ClientboundContainerSetSlotPacket(
                                -2,
                                player.containerMenu.getStateId(),
                                player.getInventory().selected,
                                heldItem
                            )
                        );
                    }
                    return result;
                }
            }
            return InteractionResult.PASS;
        });

    }

    public TestmodConfig configuration() {
        return this.config.get();
    }

    @ConfigSerializable
    public static class TestmodConfig {
        private Component message = new TextComponent("Welcome to the server!");
        private List<ItemStack> items = new ArrayList<>();
        @Comment("Protection configuration. Entries for each type will be "
                + "processed in order, and will be denied based on the first matching.")
        private ProtectionSection protection = new ProtectionSection();

        {
            final ItemStack stack = new ItemStack(Items.STONE_SWORD, 3);
            stack.enchant(Enchantments.SHARPNESS, 3);
            this.items.add(stack);
        }

        public Component message() {
            return this.message;
        }

        public List<ItemStack> items() {
            return this.items;
        }

        public ProtectionSection protection() {
            return this.protection;
        }

    }

    @ConfigSerializable
    static class ProtectionSection {
        @Comment("Log checks performed")
        private boolean debug;
        @Comment("Check for breaking blocks")
        private List<ProtectionEntry<Block>> blockBreak = new ArrayList<>();
        @Comment("Check for placing blocks")
        private List<ProtectionEntry<Block>> blockPlace = new ArrayList<>();
        @Comment("Checked for using (i.e. right clicking with) an item")
        private List<ProtectionEntry<Item>> useItem = new ArrayList<>();
        @Comment("Checked for attacking (i.e. left clicking) with an item")
        private List<ProtectionEntry<Item>> attackWithItem = new ArrayList<>();

        ProtectionSection() {
            this.blockBreak.add(new ProtectionEntry<>());
        }

        public InteractionResult testPlace(final ServerPlayer actor, final Block targetBlock) {
            return this.test(
                this.blockPlace,
                "place block",
                actor,
                targetBlock.builtInRegistryHolder()
            );
        }

        public InteractionResult testBreak(final ServerPlayer actor, final Block targetBlock) {
            return this.test(
                this.blockBreak,
                "break block",
                actor,
                targetBlock.builtInRegistryHolder()
            );
        }

        public InteractionResult testUseItem(final ServerPlayer actor, final ItemStack usedItem) {
            return this.test(
                this.useItem,
                "use item",
                actor,
                usedItem.getItem().builtInRegistryHolder()
            );
        }

        public InteractionResult testAttackWith(final ServerPlayer actor, final ItemStack usedItem) {
            return this.test(
                this.attackWithItem,
                "attack with item",
                actor,
                usedItem.getItem().builtInRegistryHolder()
            );
        }

        private <V> InteractionResult test(final List<ProtectionEntry<V>> entries, final String description, final ServerPlayer actor,
                final Holder<V> target) {
            InteractionResult result = InteractionResult.PASS;
            for (final ProtectionEntry<V> entry : entries) {
                result = entry.test(actor, target);
                if (result == InteractionResult.FAIL) {
                    if (entry.denyMessage != null) {
                        actor.sendMessage(entry.denyMessage, Util.NIL_UUID);
                    }
                    break;
                } else if (result != InteractionResult.PASS) {
                    break;
                }
            }
            if (this.debug) {
                LOGGER.info("Checked {} on player {} using {}: {}", description, actor.getScoreboardName(), target, result);
            }
            return result;
        }
    }

    @ConfigSerializable
    static class ProtectionEntry<V> {
        @Comment("Operator level to exempt users from this protection")
        private int exemptLevel = 2;
        @Comment("Message to send when a user is forbidden from this action")
        private Component denyMessage = new TextComponent("You cannot do that!").withStyle(s -> s.withColor(TextColor.fromRgb(0xFF0000)));
        @Comment("Types to catch in this entry")
        private HolderSet<V> types = HolderSet.direct(List.of());

        public InteractionResult test(final ServerPlayer player, final Holder<V> value) {
            if (this.exemptLevel != -1 && player.hasPermissions(this.exemptLevel)) { // we are exempt
                return InteractionResult.PASS;
            }

            if (this.types == null || !this.types.contains(value)) { // not an applicable item
                return InteractionResult.PASS;
            }

            return InteractionResult.FAIL; // block
        }
    }

}
