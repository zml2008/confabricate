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

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import ca.stellardrift.confabricate.NbtNodeAdapter;
import ca.stellardrift.confabricate.typeserializers.MinecraftSerializers;
import com.google.common.reflect.TypeToken;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.CommandException;
import net.minecraft.command.arguments.BlockPosArgumentType;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.gson.GsonConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

final class TestCommands {

    private TestCommands() {}

    private static final Path currentDir = FileSystems.getDefault().getPath(".");
    private static final LiteralText COMMA = new LiteralText(", ");
    private static final TextColor MESSAGE_COLOR = TextColor.fromRgb(0x2268ab);

    public static void register(final ConfabricateTester mod, final CommandDispatcher<ServerCommandSource> src) {
        src.register(literal("confabricate")
                .requires(scs -> scs.hasPermissionLevel(4))
                .then(dumpCommand())
                .then(parseObjectCommand()));
        //src.register(literal("confab").redirect(root));

        src.register(literal("test-kit")
                .requires(scs -> scs.hasPermissionLevel(2))
                .executes(ctx -> {
                    final List<ItemStack> itemsToGive = mod.getConfiguration().getItems();
                    if (itemsToGive == null || itemsToGive.isEmpty()) {
                        ctx.getSource().sendError(new LiteralText("No items are defined in the kit!"));
                        return 0;
                    }

                    final ServerPlayerEntity target = ctx.getSource().getPlayer();
                    final MutableText output = new LiteralText("You have been given: ");
                    output.styled(style -> style.withColor(MESSAGE_COLOR));

                    boolean first = true;
                    for (ItemStack stack : itemsToGive) {
                        target.inventory.offerOrDrop(target.world, stack.copy());
                        if (!first) {
                            output.append(COMMA);
                        }
                        output.append(stack.toHoverableText());
                        first = false;
                    }

                    target.sendSystemMessage(output, Util.field_25140);
                    return 1;
                }));
    }

    private static RequiredArgumentBuilder<ServerCommandSource, String> path(final String argumentName) {
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

    private static Path getPath(final String argumentName, final CommandContext<?> ctx) {
        return FileSystems.getDefault().getPath(StringArgumentType.getString(ctx, argumentName)).toAbsolutePath();
    }

    @ConfigSerializable
    static class DataTest {
        @Setting
        public Block material;

        @Setting
        public EntityType<?> entity;

        @Setting
        public Identifier ident = new Identifier("test", "demo");

        @Override
        public String toString() {
            return "DataTest{"
                    + "material=" + this.material
                    + ", entity=" + this.entity
                    + ", ident=" + this.ident
                    + '}';
        }
    }

    static LiteralArgumentBuilder<ServerCommandSource> parseObjectCommand() {
        return literal("parse").then(argument("json", StringArgumentType.greedyString()).executes(ctx -> {
            final String jsonText = StringArgumentType.getString(ctx, "json");
            final GsonConfigurationLoader loader = GsonConfigurationLoader.builder()
                    .setDefaultOptions(o -> o.withSerializers(MinecraftSerializers.collection()))
                    .setSource(() -> new BufferedReader(new StringReader(jsonText))).build();

            try {
                ctx.getSource().sendFeedback(new LiteralText("Parsed as: " + loader.load().getValue(TypeToken.of(DataTest.class))), false);
            } catch (ObjectMappingException | IOException e) {
                throw new RuntimeException(e);
            }
            return 1;
        }));
    }

    static LiteralArgumentBuilder<ServerCommandSource> dumpCommand() {
        return literal("dump").then(path("file")
                .then(literal("player").then(argument("ply", EntityArgumentType.player()).executes(ctx -> {
                    try {
                        final ServerPlayerEntity entity = EntityArgumentType.getPlayer(ctx, "ply");

                        final Text roundtripped = dumpToFile(entity::toTag, getPath("file", ctx)).toText();
                        ctx.getSource().sendFeedback(roundtripped, false);

                        ctx.getSource().sendFeedback(new LiteralText("Successfully dumped data from player ")
                                .append(entity.getNameAndUuid().copy().styled(s -> s.withColor(Formatting.AQUA))), false);
                    } catch (final Throwable t) {
                        ConfabricateTester.LOGGER.error("Unable to write", t);
                    }
                    return 1;
                })))
                .then(literal("entity").then(argument("ent", EntityArgumentType.entity()).executes(ctx -> {
                    final Entity entity = EntityArgumentType.getEntity(ctx, "ent");

                    final Text roundtripped = dumpToFile(entity::toTag, getPath("file", ctx)).toText();
                    ctx.getSource().sendFeedback(roundtripped, false);

                    ctx.getSource().sendFeedback(new LiteralText("Successfully dumped data from ")
                            .append(entity.getDisplayName().copy().styled(s -> s.withColor(Formatting.AQUA))), false);
                    return 1;
                })))
                .then(literal("block").then(argument("pos", BlockPosArgumentType.blockPos()).executes(ctx -> {
                    final BlockPos pos = BlockPosArgumentType.getBlockPos(ctx, "pos");
                    final BlockEntity entity = ctx.getSource().getWorld().getBlockEntity(pos);

                    if (entity == null) {
                        throw new CommandException(new LiteralText("No block entity found!"));
                    }

                    final Text roundtripped = dumpToFile(entity::toTag, getPath("file", ctx)).toText();
                    ctx.getSource().sendFeedback(roundtripped, false);
                    ctx.getSource().sendFeedback(new LiteralText("Successfully dumped data from ")
                            .append(new LiteralText(pos.toString()).styled(s -> s.withColor(Formatting.AQUA))), false);
                    return 1;
                }))));
    }

    static Tag dumpToFile(final Consumer<CompoundTag> dumpFunc, final Path file) throws CommandException {
        try {
            final CompoundTag out = new CompoundTag();
            dumpFunc.accept(out);

            final ConfigurationNode node = ConfigurationNode.root();
            NbtNodeAdapter.tagToNode(out, node);

            Files.createDirectories(file.getParent());
            final GsonConfigurationLoader output = GsonConfigurationLoader.builder().setPath(file).build();
            output.save(node);

            return NbtNodeAdapter.nodeToTag(output.load());
        } catch (final Throwable e) {
            e.printStackTrace();
            throw new CommandException(new LiteralText(e.getMessage()));
        }
    }

}
