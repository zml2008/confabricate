package ca.stellardrift.confabricate.test;

import ca.stellardrift.confabricate.Confabricate;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
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
 * A test mod that uses an auto-reloadable configuration
 */
public class ConfabricateTester implements ModInitializer {

    static final Logger LOGGER = LogManager.getLogger();
    private static ConfabricateTester instance;

    private @MonotonicNonNull ConfigurationReference<CommentedConfigurationNode> configFile;
    private @MonotonicNonNull ValueReference<TestmodConfig> config;

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
            configFile = Confabricate.createConfigurationFor(container);
            config = configFile.referenceTo(TestmodConfig.class);
            configFile.save();
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
            items.add(stack);
        }

        public Text getMessage() {
            return this.message;
        }

        public List<ItemStack> getItems() {
            return this.items;
        }
    }

}
