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

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import ca.stellardrift.confabricate.NbtNodeAdapter;
import ca.stellardrift.confabricate.typeserializers.MinecraftSerializers;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

final class TestCommands {

    private TestCommands() {}

    private static final Path currentDir = FileSystems.getDefault().getPath(".");
    private static final Component COMMA = new TextComponent(", ");
    private static final TextColor MESSAGE_COLOR = TextColor.fromRgb(0x2268ab);

    public static void register(final ConfabricateTester mod, final CommandDispatcher<CommandSourceStack> src) {
        src.register(literal("confabricate")
                .requires(scs -> scs.hasPermission(4))
                .then(dumpCommand())
                .then(parseObjectCommand()));
        //src.register(literal("confab").redirect(root));

        src.register(literal("test-kit")
                .requires(scs -> scs.hasPermission(2))
                .executes(ctx -> {
                    final List<ItemStack> itemsToGive = mod.configuration().items();
                    if (itemsToGive == null || itemsToGive.isEmpty()) {
                        ctx.getSource().sendFailure(new TextComponent("No items are defined in the kit!"));
                        return 0;
                    }

                    final ServerPlayer target = ctx.getSource().getPlayerOrException();
                    final MutableComponent output = new TextComponent("You have been given: ");
                    output.withStyle(style -> style.withColor(MESSAGE_COLOR));

                    boolean first = true;
                    for (final ItemStack stack : itemsToGive) {
                        target.getInventory().placeItemBackInInventory(stack.copy());
                        if (!first) {
                            output.append(COMMA);
                        }
                        output.append(stack.getDisplayName());
                        first = false;
                    }

                    ctx.getSource().sendSuccess(output, false);
                    return 1;
                }));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> path(final String argumentName) {
        return argument(argumentName, StringArgumentType.string()).suggests((src, builder) -> {
            return CompletableFuture.supplyAsync(() -> {
                try (Stream<Path> files = Files.list(currentDir)) {
                    files.filter(it -> it.toString().startsWith(builder.getRemaining()))
                            .forEach(it -> builder.suggest(it.toString()));
                } catch (final IOException e) {
                    // no-op
                }
                return builder.build();
            });
        });
    }

    private static Path path(final String argumentName, final CommandContext<?> ctx) {
        return FileSystems.getDefault().getPath(StringArgumentType.getString(ctx, argumentName)).toAbsolutePath();
    }

    @ConfigSerializable
    static class DataTest {
        public Block material;
        public EntityType<?> entity;
        public ResourceLocation ident = new ResourceLocation("test", "demo");

        @Override
        public String toString() {
            return "DataTest{"
                    + "material=" + this.material
                    + ", entity=" + this.entity
                    + ", ident=" + this.ident
                    + '}';
        }
    }

    static LiteralArgumentBuilder<CommandSourceStack> parseObjectCommand() {
        return literal("parse").then(argument("json", StringArgumentType.greedyString()).executes(ctx -> {
            final String jsonText = StringArgumentType.getString(ctx, "json");
            final GsonConfigurationLoader loader = GsonConfigurationLoader.builder()
                    .defaultOptions(o -> o.serializers(MinecraftSerializers.collection()))
                    .source(() -> new BufferedReader(new StringReader(jsonText))).build();

            try {
                ctx.getSource().sendSuccess(new TextComponent("Parsed as: " + loader.load().get(DataTest.class)), false);
            } catch (final ConfigurateException ex) {
                throw new RuntimeException(ex);
            }
            return 1;
        }));
    }

    static LiteralArgumentBuilder<CommandSourceStack> dumpCommand() {
        return literal("dump").then(path("file")
                .then(literal("player").then(argument("ply", EntityArgument.player()).executes(ctx -> {
                    try {
                        final ServerPlayer entity = EntityArgument.getPlayer(ctx, "ply");

                        final Component roundtripped = NbtUtils.toPrettyComponent(dumpToFile(withNew(entity::save), path("file", ctx)));
                        ctx.getSource().sendSuccess(roundtripped, false);

                        ctx.getSource().sendSuccess(new TextComponent("Successfully dumped data from player ")
                                .append(entity.getDisplayName().copy().withStyle(s -> s.withColor(ChatFormatting.AQUA))), false);
                    } catch (final Throwable t) {
                        ConfabricateTester.LOGGER.error("Unable to write", t);
                    }
                    return 1;
                })))
                .then(literal("entity").then(argument("ent", EntityArgument.entity()).executes(ctx -> {
                    final Entity entity = EntityArgument.getEntity(ctx, "ent");

                    final Component roundtripped = NbtUtils.toPrettyComponent(dumpToFile(withNew(entity::save), path("file", ctx)));
                    ctx.getSource().sendSuccess(roundtripped, false);

                    ctx.getSource().sendSuccess(new TextComponent("Successfully dumped data from ")
                            .append(entity.getDisplayName().copy().withStyle(s -> s.withColor(ChatFormatting.AQUA))), false);
                    return 1;
                })))
                .then(literal("block").then(argument("pos", BlockPosArgument.blockPos()).executes(ctx -> {
                    final BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
                    final BlockEntity entity = ctx.getSource().getLevel().getBlockEntity(pos);

                    if (entity == null) {
                        throw new CommandRuntimeException(new TextComponent("No block entity found!"));
                    }

                    final Component roundtripped = NbtUtils.toPrettyComponent(dumpToFile(entity::saveWithFullMetadata, path("file", ctx)));
                    ctx.getSource().sendSuccess(roundtripped, false);
                    ctx.getSource().sendSuccess(new TextComponent("Successfully dumped data from ")
                            .append(new TextComponent(pos.toString()).withStyle(s -> s.withColor(ChatFormatting.AQUA))), false);
                    return 1;
                }))));
    }

    static Supplier<CompoundTag> withNew(final Consumer<CompoundTag> consumer) {
        return () -> {
            final var tag = new CompoundTag();
            consumer.accept(tag);
            return tag;
        };
    }

    static Tag dumpToFile(final Supplier<CompoundTag> dumpFunc, final Path file) throws CommandRuntimeException {
        try {
            final CompoundTag out = dumpFunc.get();

            final ConfigurationNode node = BasicConfigurationNode.root();
            NbtNodeAdapter.tagToNode(out, node);

            Files.createDirectories(file.getParent());
            final GsonConfigurationLoader output = GsonConfigurationLoader.builder().path(file).build();
            output.save(node);

            return NbtNodeAdapter.nodeToTag(output.load());
        } catch (final IOException e) {
            e.printStackTrace();
            throw new CommandRuntimeException(new TextComponent(e.getMessage()));
        }
    }

}
