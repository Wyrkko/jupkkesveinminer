package com.jupkke.veinminer.client;

import com.jupkke.veinminer.ActivationMode;
import com.jupkke.veinminer.ClientConfig;
import com.jupkke.veinminer.VeinConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Locale;
import java.util.function.DoubleConsumer;

public class VeinConfigScreen extends Screen {
    private final Screen parent;

    public VeinConfigScreen(Screen parent) {
        super(Component.literal("Veinminer Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int colW = 150;
        int gap = 8;
        int left = this.width / 2 - colW - gap / 2;
        int right = this.width / 2 + gap / 2;
        int top = 30;
        int step = 22;

        // Left column: how it behaves
        addRenderableWidget(toggle(left, top, colW, "Veinminer", ClientConfig.ENABLED,
                "Master switch for your own veinmining."));
        addRenderableWidget(modeButton(left, top + step, colW));
        addRenderableWidget(new ConfigSlider(left, top + step * 2, colW, "Max vein size",
                1, 512, VeinConfig.MAX_VEIN_SIZE.get(), 0,
                v -> VeinConfig.MAX_VEIN_SIZE.set((int) Math.round(v))));
        addRenderableWidget(new ConfigSlider(left, top + step * 3, colW, "Extra hunger",
                0.0, 0.2, VeinConfig.EXTRA_EXHAUSTION.get(), 3,
                v -> VeinConfig.EXTRA_EXHAUSTION.set(v)));
        addRenderableWidget(toggle(left, top + step * 4, colW, "Correct tool needed", VeinConfig.REQUIRE_CORRECT_TOOL,
                "Only veinmine when your tool is right for the block."));
        addRenderableWidget(toggle(left, top + step * 5, colW, "Protect tool", VeinConfig.PREVENT_TOOL_BREAK,
                "Stop veinmining before your tool would break."));
        addRenderableWidget(toggle(left, top + step * 6, colW, "Diagonals", VeinConfig.DIAGONALS,
                "Include diagonally touching blocks."));
        addRenderableWidget(toggle(left, top + step * 7, colW, "Merge ore variants", VeinConfig.MERGE_VARIANTS,
                "Stone and deepslate versions of an ore count as one vein."));

        // Right column: what it affects
        addRenderableWidget(toggle(right, top, colW, "Auto-detect ores", VeinConfig.AUTO_DETECT_TAGS,
                "Every block tagged forge:ores, including modded ores."));
        addRenderableWidget(toggle(right, top + step, colW, "Detect by name", VeinConfig.AUTO_DETECT_NAMES,
                "Also catches modded ores whose id contains 'ore' but aren't tagged."));
        addRenderableWidget(toggle(right, top + step * 2, colW, "Logs", VeinConfig.INCLUDE_LOGS,
                "Veinmine whole trees."));
        addRenderableWidget(toggle(right, top + step * 3, colW, "Leaves", VeinConfig.INCLUDE_LEAVES,
                "Veinmine leaves."));

        Button white = Button.builder(Component.literal("Whitelist..."),
                b -> this.minecraft.setScreen(new BlockListScreen(this, "Whitelist (extra blocks)", VeinConfig.WHITELIST)))
                .bounds(right, top + step * 4, colW, 20).build();
        white.setTooltip(Tooltip.create(Component.literal("Add any block, #tag, or modid:* on top of auto-detect.")));
        addRenderableWidget(white);

        Button black = Button.builder(Component.literal("Blacklist..."),
                b -> this.minecraft.setScreen(new BlockListScreen(this, "Blacklist (never veinmine)", VeinConfig.BLACKLIST)))
                .bounds(right, top + step * 5, colW, 20).build();
        black.setTooltip(Tooltip.create(Component.literal("Blocks that are never veinmined, overrides everything.")));
        addRenderableWidget(black);

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(this.width / 2 - 100, this.height - 26, 200, 20).build());
    }

    private Button toggle(int x, int y, int w, String name, ForgeConfigSpec.BooleanValue value, String tip) {
        Button button = Button.builder(toggleText(name, value.get()), b -> {
            boolean now = !value.get();
            value.set(now);
            b.setMessage(toggleText(name, now));
        }).bounds(x, y, w, 20).build();
        button.setTooltip(Tooltip.create(Component.literal(tip)));
        return button;
    }

    private static Component toggleText(String name, boolean on) {
        return Component.literal(name + ": ")
                .append(Component.literal(on ? "ON" : "OFF").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    private Button modeButton(int x, int y, int w) {
        Button button = Button.builder(modeText(ClientConfig.MODE.get()), b -> {
            ActivationMode next = ClientConfig.MODE.get().next();
            ClientConfig.MODE.set(next);
            b.setMessage(modeText(next));
        }).bounds(x, y, w, 20).build();
        button.setTooltip(Tooltip.create(Component.literal(
                "Sneak, Always On, or hold the Veinmine key (set it in Controls, default V).")));
        return button;
    }

    private static Component modeText(ActivationMode mode) {
        return Component.literal("Activation: " + mode.label());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        g.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void removed() {
        try {
            ClientConfig.SPEC.save();
            VeinConfig.SPEC.save();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    private static final class ConfigSlider extends AbstractSliderButton {
        private final String label;
        private final double min;
        private final double max;
        private final int decimals;
        private final DoubleConsumer onChange;

        ConfigSlider(int x, int y, int w, String label, double min, double max,
                     double current, int decimals, DoubleConsumer onChange) {
            super(x, y, w, 20, Component.empty(), Mth.clamp((current - min) / (max - min), 0.0, 1.0));
            this.label = label;
            this.min = min;
            this.max = max;
            this.decimals = decimals;
            this.onChange = onChange;
            updateMessage();
        }

        private double real() {
            double v = min + this.value * (max - min);
            double scale = Math.pow(10, decimals);
            return Math.round(v * scale) / scale;
        }

        @Override
        protected void updateMessage() {
            String text = decimals == 0
                    ? String.valueOf((long) real())
                    : String.format(Locale.ROOT, "%." + decimals + "f", real());
            setMessage(Component.literal(label + ": " + text));
        }

        @Override
        protected void applyValue() {
            onChange.accept(real());
        }
    }
}
