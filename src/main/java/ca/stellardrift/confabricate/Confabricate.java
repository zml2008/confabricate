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

import ca.stellardrift.confabricate.typeserializers.IdentifierSerializer;
import ca.stellardrift.confabricate.typeserializers.RegistrySerializer;
import ca.stellardrift.confabricate.typeserializers.TextSerializer;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.container.ContainerType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.Schedule;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.decoration.painting.PaintingMotive;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.particle.ParticleType;
import net.minecraft.potion.Potion;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.sound.SoundEvent;
import net.minecraft.stat.StatType;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.structure.pool.StructurePoolElementType;
import net.minecraft.structure.processor.StructureProcessorType;
import net.minecraft.structure.rule.RuleTestType;
import net.minecraft.util.registry.MutableRegistry;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.VillagerType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSourceType;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.carver.Carver;
import net.minecraft.world.gen.chunk.ChunkGeneratorType;
import net.minecraft.world.gen.decorator.Decorator;
import net.minecraft.world.gen.decorator.TreeDecoratorType;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.gen.foliage.FoliagePlacerType;
import net.minecraft.world.gen.placer.BlockPlacerType;
import net.minecraft.world.gen.stateprovider.BlockStateProviderType;
import net.minecraft.world.gen.surfacebuilder.SurfaceBuilder;
import net.minecraft.world.poi.PointOfInterestType;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializerCollection;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.Set;

/**
 * Configurate integration holder, providing access to configuration loaders pre-configured to work with Minecraft types
 */
public class Confabricate implements ModInitializer {
    private static boolean initialized;
    static final Logger LOGGER = LogManager.getLogger();

    private static TypeSerializerCollection mcTypeSerializers;
    private static Set<Registry<?>> brokenRegistries = Sets.newHashSet();
    private static Set<Registry<?>> registeredRegistries = Sets.newHashSet();

    public Confabricate() {
        if (initialized) {
            throw new ExceptionInInitializerError("Confabricate can only be initialized by the Fabric mod loader");
        }
        initialized = true;

    }

    @Override
    public void onInitialize() {
        mcTypeSerializers = TypeSerializers.getDefaultSerializers()
                .newChild()
                .registerType(IdentifierSerializer.TOKEN, IdentifierSerializer.INSTANCE)
                .registerType(TextSerializer.TOKEN, TextSerializer.INSTANCE);

        registerRegistry(SoundEvent.class, Registry.SOUND_EVENT);
        registerRegistry(Fluid.class, Registry.FLUID);
        registerRegistry(StatusEffect.class, Registry.STATUS_EFFECT);
        registerRegistry(Block.class, Registry.BLOCK);
        registerRegistry(Enchantment.class, Registry.ENCHANTMENT);
        registerRegistry(new TypeToken<EntityType<?>>() {}, Registry.ENTITY_TYPE);
        registerRegistry(Item.class, Registry.ITEM);
        registerRegistry(Potion.class, Registry.POTION);
        registerRegistry(new TypeToken<Carver<?>>() {}, Registry.CARVER);
        registerRegistry(new TypeToken<SurfaceBuilder<?>>() {}, Registry.SURFACE_BUILDER);
        registerRegistry(new TypeToken<Feature<?>>() {}, Registry.FEATURE);
        registerRegistry(new TypeToken<Decorator<?>>() {}, Registry.DECORATOR);
        registerRegistry(Biome.class, Registry.BIOME);
        registerRegistry(new TypeToken<BlockStateProviderType<?>>() {}, Registry.BLOCK_STATE_PROVIDER_TYPE);
        registerRegistry(new TypeToken<BlockPlacerType<?>>() {}, Registry.BLOCK_PLACER_TYPE);
        registerRegistry(new TypeToken<FoliagePlacerType<?>>() {}, Registry.FOLIAGE_PLACER_TYPE);
        registerRegistry(new TypeToken<TreeDecoratorType<?>>() {}, Registry.TREE_DECORATOR_TYPE);
        registerRegistry(new TypeToken<ParticleType<?>>() {}, Registry.PARTICLE_TYPE);
        registerRegistry(new TypeToken<BiomeSourceType<?, ?>>() {}, Registry.BIOME_SOURCE_TYPE);
        registerRegistry(new TypeToken<BlockEntityType<?>>() {}, Registry.BLOCK_ENTITY_TYPE);
        registerRegistry(new TypeToken<ChunkGeneratorType<?, ?>>() {}, Registry.CHUNK_GENERATOR_TYPE);
        registerRegistry(DimensionType.class, Registry.DIMENSION_TYPE);
        registerRegistry(PaintingMotive.class, Registry.PAINTING_MOTIVE);
        brokenRegistries.add(Registry.CUSTOM_STAT);
        //registerRegistry(Identifier, Registry.CUSTOM_STAT); // can't register -- doesn't have its own type
        registerRegistry(ChunkStatus.class, Registry.CHUNK_STATUS);
        registerRegistry(new TypeToken<StructureFeature<?>>() {}, Registry.STRUCTURE_FEATURE);
        registerRegistry(StructurePieceType.class, Registry.STRUCTURE_PIECE);
        registerRegistry(RuleTestType.class, Registry.RULE_TEST);
        registerRegistry(StructureProcessorType.class, Registry.STRUCTURE_PROCESSOR);
        registerRegistry(StructurePoolElementType.class, Registry.STRUCTURE_POOL_ELEMENT);
        registerRegistry(new TypeToken<ContainerType<?>>() {}, Registry.CONTAINER);
        registerRegistry(new TypeToken<RecipeType<?>>() {}, Registry.RECIPE_TYPE);
        registerRegistry(new TypeToken<RecipeSerializer<?>>() {}, Registry.RECIPE_SERIALIZER);
        registerRegistry(new TypeToken<StatType<?>>() {}, Registry.STAT_TYPE);
        registerRegistry(VillagerType.class, Registry.VILLAGER_TYPE);
        registerRegistry(VillagerProfession.class, Registry.VILLAGER_PROFESSION);
        registerRegistry(PointOfInterestType.class, Registry.POINT_OF_INTEREST_TYPE);
        registerRegistry(new TypeToken<MemoryModuleType<?>>() {}, Registry.MEMORY_MODULE_TYPE);
        registerRegistry(new TypeToken<SensorType<?>>() {}, Registry.SENSOR_TYPE);
        registerRegistry(Schedule.class, Registry.SCHEDULE);
        registerRegistry(Activity.class, Registry.ACTIVITY);

        for (MutableRegistry<?> reg : Registry.REGISTRIES) {
            if (!registeredRegistries.contains(reg) && !brokenRegistries.contains(reg)) {
                LOGGER.warn("Registry " + Registry.REGISTRIES.getId(reg) + " does not have an associated TypeSerializer!");
            }
        }
    }

    private static <T> void registerRegistry(TypeToken<T> token, Registry<T> registry) {
        if (registeredRegistries.add(registry)) {
            mcTypeSerializers.registerType(token, new RegistrySerializer<>(registry));
        }
    }

    private static <T> void registerRegistry(Class<T> registeredType, Registry<T> registry) {
        registerRegistry(TypeToken.of(registeredType), registry);
    }

    public static TypeSerializerCollection getMinecraftTypeSerializers() {
        return mcTypeSerializers;
    }

    public static ConfigurationLoader<CommentedConfigurationNode> createLoaderFor(ModContainer mod) {
        return createLoaderFor(mod, true);
    }

    public static ConfigurationLoader<CommentedConfigurationNode> createLoaderFor(ModContainer mod, boolean ownDirectory) {
        Path configRoot = FabricLoader.getInstance().getConfigDirectory().toPath();
        if (ownDirectory) {
            configRoot = configRoot.resolve(mod.getMetadata().getId());
        }
        Path configFile = configRoot.resolve(mod.getMetadata().getId() + ".conf");
        return HoconConfigurationLoader.builder()
                .setPath(configFile)
                .setDefaultOptions(ConfigurationOptions.defaults().setSerializers(getMinecraftTypeSerializers()))
                .build();
    }
}
