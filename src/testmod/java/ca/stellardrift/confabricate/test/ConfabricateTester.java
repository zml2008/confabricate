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

import static org.spongepowered.configurate.transformation.ConfigurationTransformation.WILDCARD_OBJECT;

import ca.stellardrift.confabricate.Confabricate;
import ca.stellardrift.confabricate.typeserializers.MinecraftSerializers;
import com.google.common.collect.ImmutableSet;
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
import net.minecraft.block.Block;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.tag.Tag;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.Util;
import net.minecraft.util.profiler.Profiler;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

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
                    .addType(TypeReferences.ITEM_STACK, "items", WILDCARD_OBJECT) // every child of "items" should be upgraded as an ItemStack
                    .build();

            final boolean wasEmpty = node.empty();
            final int oldVersion = xform.version(node);
            xform.apply(node);
            final int newVersion = xform.version(node);
            if (newVersion > oldVersion && !wasEmpty) {
                LOGGER.info("Updated configuration from version {} to {}", oldVersion, newVersion);
            }

            this.config = this.configFile.referenceTo(TestmodConfig.class);
            ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleResourceReloadListener<Void>() {
                @Override
                public Identifier getFabricId() {
                    return new Identifier("confabricate-testmod", "config");
                }

                @Override
                public CompletableFuture<Void> load(final ResourceManager manager, final Profiler profiler, final Executor executor) {
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
                public CompletableFuture<Void> apply(final Void data, final ResourceManager manager, final Profiler profiler,
                        final Executor executor) {
                    return CompletableFuture.completedFuture(data);
                }

                @Override
                public Collection<Identifier> getFabricDependencies() {
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
            if (!world.isClient && player instanceof ServerPlayerEntity && hand == Hand.MAIN_HAND) {
                final ActionResult testAttack = this.configuration().protection().testAttackWith((ServerPlayerEntity) player,
                                                                                                 player.getMainHandStack());

                if (testAttack != ActionResult.PASS) {
                    return testAttack;
                }

                return this.configuration().protection().testBreak((ServerPlayerEntity) player,
                                                                   world.getBlockState(blockPos).getBlock());
            }
            return ActionResult.PASS;
        });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, result) -> {
            if (!world.isClient && player instanceof ServerPlayerEntity && hand == Hand.MAIN_HAND) {
                return this.configuration().protection().testAttackWith((ServerPlayerEntity) player, player.getMainHandStack());
            }
            return ActionResult.PASS;
        });
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClient && player instanceof ServerPlayerEntity && hand == Hand.MAIN_HAND) {
                return new TypedActionResult<>(this.configuration().protection().testUseItem((ServerPlayerEntity) player,
                                                                                             player.getMainHandStack()), player.getMainHandStack());
            }
            return TypedActionResult.pass(player.getStackInHand(hand));
        });
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient && player instanceof ServerPlayerEntity && hand == Hand.MAIN_HAND) {
                final ItemStack heldItem = player.getMainHandStack();
                if (heldItem.getItem() instanceof BlockItem) {
                    final ActionResult result = this.configuration().protection().testPlace((ServerPlayerEntity) player,
                                                                                            ((BlockItem) heldItem.getItem()).getBlock());
                    if (result == ActionResult.FAIL) { // if we can't place, then let's restore inventory
                        ((ServerPlayerEntity) player).networkHandler.sendPacket(
                                new ScreenHandlerSlotUpdateS2CPacket(-2,
                                        player.getInventory().selectedSlot, heldItem));
                    }
                    return result;
                }
            }
            return ActionResult.PASS;
        });

    }

    public TestmodConfig configuration() {
        return this.config.get();
    }

    @ConfigSerializable
    public static class TestmodConfig {
        private Text message = new LiteralText("Welcome to the server!");
        private List<ItemStack> items = new ArrayList<>();
        @Comment("Protection configuration. Entries for each type will be "
                + "processed in order, and will be denied based on the first matching.")
        private ProtectionSection protection = new ProtectionSection();

        {
            final ItemStack stack = new ItemStack(Items.STONE_SWORD, 3);
            stack.addEnchantment(Enchantments.SHARPNESS, 3);
            this.items.add(stack);
        }

        public Text message() {
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
        private boolean debug = false;
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

        public ActionResult testPlace(final ServerPlayerEntity actor, final Block targetBlock) {
            return test(this.blockPlace, "place block", actor, targetBlock);
        }

        public ActionResult testBreak(final ServerPlayerEntity actor, final Block targetBlock) {
            return test(this.blockBreak, "break block", actor, targetBlock);
        }

        public ActionResult testUseItem(final ServerPlayerEntity actor, final ItemStack usedItem) {
            return test(this.useItem, "use item", actor, usedItem.getItem());
        }

        public ActionResult testAttackWith(final ServerPlayerEntity actor, final ItemStack usedItem) {
            return test(this.attackWithItem, "attack with item", actor, usedItem.getItem());
        }

        private <V> ActionResult test(final List<ProtectionEntry<V>> entries, final String description, final ServerPlayerEntity actor,
                final V target) {
            ActionResult result = ActionResult.PASS;
            for (final ProtectionEntry<V> entry : entries) {
                result = entry.test(actor, target);
                if (result == ActionResult.FAIL) {
                    if (entry.denyMessage != null) {
                        actor.sendSystemMessage(entry.denyMessage, Util.NIL_UUID);
                    }
                    break;
                } else if (result != ActionResult.PASS) {
                    break;
                }
            }
            if (this.debug) {
                LOGGER.info("Checked {} on player {} using {}: {}", description, actor.getEntityName(), target, result);
            }
            return result;
        }
    }

    @ConfigSerializable
    static class ProtectionEntry<V> {
        @Comment("Operator level to exempt users from this protection")
        private int exemptLevel = 2;
        @Comment("Message to send when a user is forbidden from this action")
        private Text denyMessage = new LiteralText("You cannot do that!").styled(s -> s.withColor(TextColor.fromRgb(0xFF0000)));
        @Comment("Types to catch in this entry")
        private Tag<V> types = Tag.of(ImmutableSet.of());

        public ActionResult test(final ServerPlayerEntity player, final V value) {
            if (player.hasPermissionLevel(this.exemptLevel)) { // we are exempt
                return ActionResult.PASS;
            }

            if (this.types == null || !this.types.contains(value)) { // not an applicable item
                return ActionResult.PASS;
            }

            return ActionResult.FAIL; // block
        }
    }

}
