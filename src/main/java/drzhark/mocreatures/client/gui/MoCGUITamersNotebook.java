/*
 * GNU GENERAL PUBLIC LICENSE Version 3
 */
package drzhark.mocreatures.client.gui;

import drzhark.mocreatures.MoCreatures;
import drzhark.mocreatures.network.message.MoCMessageTamersNotebook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
public class MoCGUITamersNotebook extends Screen {

    private static final int LINE_HEIGHT = 12;
    private static final int MARGIN = 20;
    private static final int TITLE_MARGIN_BOTTOM = 20;
    private static final int MAX_LINES_VISIBLE = 18;

    private final List<MoCMessageTamersNotebook.PetEntry> entries;
    private int scrollOffset;

    public MoCGUITamersNotebook(List<MoCMessageTamersNotebook.PetEntry> entries) {
        super(Component.translatable("gui.mocreatures.tamers_notebook.title"));
        this.entries = entries;
        this.scrollOffset = 0;
    }

    @Override
    protected void init() {
        super.init();
        int listTop = MARGIN + TITLE_MARGIN_BOTTOM;
        int listBottom = height - 40;
        int listHeight = Math.max(0, listBottom - listTop);
        // Invisible widget over the list area so it receives clicks first
        addRenderableWidget(new AbstractWidget(0, listTop, width, listHeight, Component.empty()) {
            @Override
            public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button != 0 || entries.isEmpty()) return false;
                int lineIndex = (int) ((mouseY - listTop) / LINE_HEIGHT);
                int index = scrollOffset + lineIndex;
                if (index < 0 || index >= entries.size()) return false;
                MoCMessageTamersNotebook.PetEntry e = entries.get(index);
                if (e.inAmulet) return false;
                String coords = e.blockX + ", " + e.blockY + ", " + e.blockZ;
                copyToClipboard(coords);
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.displayClientMessage(
                            Component.translatable("gui.mocreatures.tamers_notebook.copied"), true);
                }
                return true;
            }
            @Override
            protected void updateWidgetNarration(NarrationElementOutput output) {}
        });
        int buttonW = 100;
        int buttonX = (width - buttonW) / 2;
        addRenderableWidget(
                Button.builder(Component.translatable("gui.done"), b -> onClose())
                        .bounds(buttonX, height - 28, buttonW, 20)
                        .build());
    }

    private static void copyToClipboard(String text) {
        Minecraft.getInstance().execute(() -> {
            try {
                long windowHandle = getGlfwWindowHandle();
                if (windowHandle != 0L) {
                    GLFW.glfwSetClipboardString(windowHandle, text);
                } else {
                    MoCreatures.LOGGER.warn("Could not get GLFW window handle for clipboard");
                }
            } catch (Throwable t) {
                MoCreatures.LOGGER.warn("Could not copy to clipboard: {}", t.getMessage());
            }
        });
    }

    /** Get the GLFW window handle from Minecraft's Window. Uses reflection for compatibility. */
    private static long getGlfwWindowHandle() {
        try {
            Object window = Minecraft.getInstance().getWindow();
            if (window == null) return 0L;
            // Mojang mappings: field is "window" (long). Obfuscated/srg may differ.
            for (String fieldName : new String[] { "window", "handle" }) {
                try {
                    Field f = window.getClass().getDeclaredField(fieldName);
                    f.setAccessible(true);
                    Object value = f.get(window);
                    if (value instanceof Long) return (Long) value;
                    if (value instanceof Number) return ((Number) value).longValue();
                } catch (NoSuchFieldException ignored) {}
            }
            // Fallback: getWindow() method on Window (some mappings)
            Method getter = window.getClass().getMethod("getWindow");
            Object value = getter.invoke(window);
            if (value instanceof Long) return (Long) value;
            if (value instanceof Number) return ((Number) value).longValue();
        } catch (Throwable t) {
            MoCreatures.LOGGER.debug("Failed to get GLFW window handle: {}", t.getMessage());
        }
        return 0L;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        int centerX = width / 2;
        int y = MARGIN;
        graphics.drawCenteredString(font, title, centerX, y, 0xFFFFFF);
        y += TITLE_MARGIN_BOTTOM;

        if (entries.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("gui.mocreatures.tamers_notebook.no_pets"), centerX, y + 20, 0xAAAAAA);
            return;
        }

        int listTop = y;
        int listBottom = height - 40;
        int visibleLines = (listBottom - listTop) / LINE_HEIGHT;
        int maxScroll = Math.max(0, entries.size() - visibleLines);

        for (int i = 0; i < visibleLines; i++) {
            int index = scrollOffset + i;
            if (index >= entries.size()) break;
            MoCMessageTamersNotebook.PetEntry e = entries.get(index);
            int lineY = listTop + i * LINE_HEIGHT;
            String name = e.name.isEmpty() ? "—" : e.name;
            String type = e.typeDisplay.isEmpty() ? "" : e.typeDisplay;
            String loc;
            if (e.inAmulet) {
                loc = "(" + Component.translatable("gui.mocreatures.tamers_notebook.in_amulet").getString() + ")";
            } else {
                loc = String.format("%s  %d, %d, %d", shortDimension(e.dimension), e.blockX, e.blockY, e.blockZ);
            }
            graphics.drawString(font, name, MARGIN, lineY, 0xFFFF00, false);
            int typeX = MARGIN + font.width(name) + 8;
            if (typeX < width - 200) {
                graphics.drawString(font, type, typeX, lineY, 0xAAAAAA, false);
            }
            int locX = width - MARGIN - font.width(loc);
            if (locX > typeX + 4) {
                graphics.drawString(font, loc, locX, lineY, 0x88FF88, false);
            }
        }

        if (maxScroll > 0) {
            graphics.drawString(font, "↑↓ " + (scrollOffset + 1) + "/" + entries.size(), width - MARGIN - 60, MARGIN + 2, 0x888888, false);
        }

        // Hint for copy
        if (!entries.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.mocreatures.tamers_notebook.click_to_copy").getString(), MARGIN, height - 42, 0x888888, false);
        }
    }

    /** Returns a short dimension label (avoids "O" being confused with zero). */
    private static String shortDimension(String dimension) {
        if (dimension == null || dimension.isEmpty()) return "?";
        if (dimension.contains("overworld")) return "OW";  // Overworld
        if (dimension.contains("the_nether")) return "N"; // Nether
        if (dimension.contains("the_end")) return "E";     // End
        int slash = dimension.lastIndexOf('/');
        if (slash >= 0 && slash < dimension.length() - 1) {
            return dimension.substring(slash + 1);
        }
        return dimension;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollAmount) {
        int listTop = MARGIN + TITLE_MARGIN_BOTTOM;
        int listBottom = height - 40;
        int visibleLines = (listBottom - listTop) / LINE_HEIGHT;
        int maxScroll = Math.max(0, entries.size() - visibleLines);
        if (scrollAmount > 0) {
            scrollOffset = Math.max(0, scrollOffset - 1);
        } else if (scrollAmount < 0) {
            scrollOffset = Math.min(maxScroll, scrollOffset + 1);
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
