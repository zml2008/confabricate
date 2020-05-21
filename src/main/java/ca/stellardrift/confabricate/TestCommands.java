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

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
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
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

class TestCommands {
    private static final Path currentDir = FileSystems.getDefault().getPath(".");

    public static void register(CommandDispatcher<ServerCommandSource> src) {
        LiteralCommandNode<ServerCommandSource> root = src.register(literal("confabricate")
                .requires(scs -> scs.hasPermissionLevel(4))
                .then(dumpCommand())
        .then(parseObjectCommand()));
        src.register(literal("confab").redirect(root ));
    }

    private static RequiredArgumentBuilder<ServerCommandSource, String> path(String argumentName) {
        return argument(argumentName, StringArgumentType.string()).suggests((src, builder) -> {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Files.list(currentDir)
                            .filter(it -> it.toString().startsWith(builder.getRemaining()))
                            .forEach(it -> builder.suggest(it.toString()));
                } catch (IOException e) {
                    // no-op
                }
                return builder.build();
            });
        });
    }

    private static Path getPath(String argumentName, CommandContext<?> ctx) {
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
            return "DataTest{" +
                    "material=" + material +
                    ", entity=" + entity +
                    ", ident=" + ident +
                    '}';
        }
    }

    static LiteralArgumentBuilder<ServerCommandSource> parseObjectCommand() {
        return literal("parse").then(argument("json", StringArgumentType.greedyString()).executes(ctx -> {
            String jsonText = StringArgumentType.getString(ctx, "json");
            GsonConfigurationLoader loader = GsonConfigurationLoader.builder()
                    .setDefaultOptions(o -> o.withSerializers(Confabricate.getMinecraftTypeSerializers()))
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
                        ServerPlayerEntity entity = EntityArgumentType.getPlayer(ctx, "ply");

                        Text roundtripped = dumpToFile(entity::toTag, getPath("file", ctx)).toText();
                        ctx.getSource().sendFeedback(roundtripped, false);

                        ctx.getSource().sendFeedback(new LiteralText("Successfully dumped data from player ")
                                .append(entity.getNameAndUuid().copy().styled(s -> s.withColor(Formatting.AQUA))), false);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                    return 1;
                })))
                .then(literal("entity").then(argument("ent", EntityArgumentType.entity()).executes(ctx -> {
                    Entity entity = EntityArgumentType.getEntity(ctx, "ent");

                    Text roundtripped = dumpToFile(entity::toTag, getPath("file", ctx)).toText();
                    ctx.getSource().sendFeedback(roundtripped, false);

                    ctx.getSource().sendFeedback(new LiteralText("Successfully dumped data from ")
                            .append(entity.getDisplayName().copy().styled(s -> s.withColor(Formatting.AQUA))), false);
                    return 1;
                })))
                .then(literal("block").then(argument("pos", BlockPosArgumentType.blockPos()).executes(ctx -> {
                    BlockPos pos = BlockPosArgumentType.getBlockPos(ctx, "pos");
                    BlockEntity entity = ctx.getSource().getWorld().getBlockEntity(pos);

                    if (entity == null) {
                        throw new CommandException(new LiteralText("No block entity found!"));
                    }

                    Text roundtripped = dumpToFile(entity::toTag, getPath("file", ctx)).toText();
                    ctx.getSource().sendFeedback(roundtripped, false);
                    ctx.getSource().sendFeedback(new LiteralText("Successfully dumped data from ")
                            .append(new LiteralText(pos.toString()).styled(s -> s.withColor(Formatting.AQUA))), false);
                    return 1;
                }))));
    }

    static Tag dumpToFile(Consumer<CompoundTag> dumpFunc, Path file) throws CommandException {
        try {
            CompoundTag out = new CompoundTag();
            dumpFunc.accept(out);

            ConfigurationNode node = ConfigurationNode.root();
            NbtNodeAdapter.tagToNode(out, node);

            Files.createDirectories(file.getParent());
            GsonConfigurationLoader output = GsonConfigurationLoader.builder().setPath(file).build();
            output.save(node);

            return NbtNodeAdapter.nodeToTag(output.load());
        } catch (Throwable e) {
            e.printStackTrace();
            throw new CommandException(new LiteralText(e.getMessage()));

        }
    }

}
