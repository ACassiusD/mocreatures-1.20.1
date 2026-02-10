/*
 * GNU GENERAL PUBLIC LICENSE Version 3
 */
package drzhark.mocreatures.client.gui;

import drzhark.mocreatures.MoCreatures;
import drzhark.mocreatures.config.MoCConfigCategory;
import drzhark.mocreatures.config.MoCConfiguration;
import drzhark.mocreatures.config.MoCProperty;
import drzhark.mocreatures.config.biome.BiomeSpawnConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.TitleScreen;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@OnlyIn(Dist.CLIENT)
public class MoCGUISettings extends Screen {

    private static final int LINE_HEIGHT = 12;
    private static final int PADDING = 10;
    private static final int TITLE_HEIGHT = 30;
    private static final int BOTTOM_BUTTON_HEIGHT = 30;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_PAD = 2;

    private static final int FREQUENCY_MIN = 0;
    private static final int FREQUENCY_MAX = 16;
    private static final int SPAWN_GROUP_MIN = 0;
    private static final int SPAWN_GROUP_MAX_MIN = 2;  // max for minSpawn
    private static final int SPAWN_GROUP_MAX_MAX = 6;  // max for maxSpawn
    private static final int GLOBAL_INT_MIN = 0;
    private static final int GLOBAL_INT_MAX = 100;

    private static final int BG_HEADER = 0x18FFFFFF;
    private static final int BG_ALT = 0x0AFFFFFF;
    private static final int HEADER_TEXT_COLOR = 0xFFE0E0A0;
    private static final int NORMAL_TEXT_COLOR = 0xFFCCCCCC;

    public enum ViewMode {
        GLOBAL_SETTINGS,
        CREATURE_SPAWNS
    }

    public static final class Line {
        public final String text;
        public final boolean isHeader;
        public final MoCProperty property;
        public final String spawnKey;       // "canSpawn", "frequency", "minSpawn", "maxSpawn" or null
        public final String spawnCategoryName; // lowercase entity key for Creature tab

        public Line(String text, boolean isHeader, MoCProperty property, String spawnKey, String spawnCategoryName) {
            this.text = text;
            this.isHeader = isHeader;
            this.property = property;
            this.spawnKey = spawnKey;
            this.spawnCategoryName = spawnCategoryName;
        }
    }

    private ViewMode viewMode = ViewMode.GLOBAL_SETTINGS;
    private final List<Line> lines = new ArrayList<>();
    private int scrollOffset = 0;
    private int listTop;
    private int listBottom;
    private boolean scrollbarDragging = false;
    private Button disableAllSpawnsButton;

    public MoCGUISettings() {
        super(Component.translatable("gui.mocreatures.settings"));
    }

    @Override
    protected void init() {
        super.init();
        listTop = TITLE_HEIGHT + PADDING + 4;
        listBottom = height - BOTTOM_BUTTON_HEIGHT - PADDING;

        int tabY = TITLE_HEIGHT - 4;
        int centerX = width / 2;
        int buttonW = 100;
        int gap = 10;
        int bottomLeftX = centerX - (buttonW * 3 + gap * 2) / 2;

        addRenderableWidget(Button.builder(Component.translatable("gui.mocreatures.global_settings"), b -> switchTab(ViewMode.GLOBAL_SETTINGS))
            .bounds(PADDING, tabY, 120, 20)
            .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.mocreatures.creature_spawns"), b -> switchTab(ViewMode.CREATURE_SPAWNS))
            .bounds(PADDING + 124, tabY, 120, 20)
            .build());

        disableAllSpawnsButton = Button.builder(Component.translatable("gui.mocreatures.disable_all_spawns"), b -> onDisableAllSpawns())
            .bounds(width - PADDING - 130, tabY, 130, 20)
            .build();
        disableAllSpawnsButton.visible = (viewMode == ViewMode.CREATURE_SPAWNS);
        addRenderableWidget(disableAllSpawnsButton);

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
            .bounds(bottomLeftX, height - BOTTOM_BUTTON_HEIGHT - PADDING + 2, buttonW, 20)
            .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.mocreatures.reload"), b -> onReload())
            .bounds(bottomLeftX + buttonW + gap, height - BOTTOM_BUTTON_HEIGHT - PADDING + 2, buttonW, 20)
            .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.mocreatures.save"), b -> onSave())
            .bounds(bottomLeftX + (buttonW + gap) * 2, height - BOTTOM_BUTTON_HEIGHT - PADDING + 2, buttonW, 20)
            .build());

        buildLines();
    }

    private void switchTab(ViewMode mode) {
        viewMode = mode;
        scrollOffset = 0;
        if (disableAllSpawnsButton != null) disableAllSpawnsButton.visible = (viewMode == ViewMode.CREATURE_SPAWNS);
        buildLines();
    }

    private void buildLines() {
        lines.clear();
        if (viewMode == ViewMode.GLOBAL_SETTINGS) {
            buildGlobalLines();
        } else {
            buildCreatureLines();
        }
    }

    private void buildGlobalLines() {
        MoCConfiguration config = MoCreatures.proxy.mocSettingsConfig;
        if (config == null) return;
        Set<String> names = new TreeSet<>(config.getCategoryNames());
        for (String catName : names) {
            MoCConfigCategory cat = config.getCategory(catName);
            if (cat == null || cat.isChild()) continue;
            lines.add(new Line("--- " + catName + " ---", true, null, null, null));
            for (MoCProperty prop : cat.getValues().values()) {
                String value = prop.isList() ? "[list]" : (prop.getString() != null ? prop.getString() : "");
                char typeChar = prop.getTypeMoC() != null ? prop.getTypeMoC().name().charAt(0) : 'S';
                lines.add(new Line("  " + prop.getName() + " = " + value + " (" + typeChar + ")", false, prop, null, null));
            }
        }
    }

    private void buildCreatureLines() {
        for (String creatureName : BiomeSpawnConfig.getAllCreatureNames()) {
            BiomeSpawnConfig.CreatureSpawnData data = BiomeSpawnConfig.getSpawnData(creatureName);
            if (data == null) continue;
            String displayName = toDisplayName(creatureName);
            lines.add(new Line("--- " + displayName + " ---", true, null, null, null));
            lines.add(new Line("  canSpawn = " + data.enabled, false, null, "canSpawn", creatureName));
            lines.add(new Line("  frequency = " + data.weight + " (L/R ±1) [0-16]", false, null, "frequency", creatureName));
            lines.add(new Line("  minSpawn = " + data.minCount + " (L/R ±1) [0-2]", false, null, "minSpawn", creatureName));
            lines.add(new Line("  maxSpawn = " + data.maxCount + " (L/R ±1) [0-6]", false, null, "maxSpawn", creatureName));
        }
    }

    private static String toDisplayName(String creatureName) {
        if (creatureName == null || creatureName.isEmpty()) return creatureName;
        String spaced = creatureName.replace('_', ' ');
        return spaced.substring(0, 1).toUpperCase() + (spaced.length() > 1 ? spaced.substring(1).toLowerCase() : "");
    }

    private int getListRight() {
        int totalContentHeight = lines.size() * LINE_HEIGHT;
        int visibleHeight = listBottom - listTop;
        if (totalContentHeight > visibleHeight) {
            return width - PADDING - (SCROLLBAR_WIDTH + SCROLLBAR_PAD);
        }
        return width - PADDING;
    }

    private int getMaxScroll() {
        int totalContentHeight = lines.size() * LINE_HEIGHT;
        int visibleHeight = listBottom - listTop;
        return Math.max(0, totalContentHeight - visibleHeight);
    }

    private void onReload() {
        if (viewMode == ViewMode.GLOBAL_SETTINGS) {
            MoCreatures.proxy.mocSettingsConfig.load();
            MoCreatures.proxy.readGlobalConfigValues();
        } else {
            BiomeSpawnConfig.reloadConfig();
        }
        buildLines();
    }

    private void onSave() {
        if (viewMode == ViewMode.GLOBAL_SETTINGS) {
            MoCreatures.proxy.mocSettingsConfig.save();
            MoCreatures.proxy.readGlobalConfigValues();
        } else {
            try {
                BiomeSpawnConfig.saveConfig();
            } catch (IOException e) {
                MoCreatures.LOGGER.error("Failed to save MoCreatures.json", e);
            }
        }
        buildLines();
        saveAndReturnToMenu();
    }

    private void onDisableAllSpawns() {
        for (String creatureName : BiomeSpawnConfig.getAllCreatureNames()) {
            BiomeSpawnConfig.CreatureSpawnData data = BiomeSpawnConfig.getSpawnData(creatureName);
            if (data != null) data.enabled = false;
        }
        try {
            BiomeSpawnConfig.saveConfig();
        } catch (IOException e) {
            MoCreatures.LOGGER.error("Failed to save MoCreatures.json", e);
        }
        buildLines();
    }

    private void saveAndReturnToMenu() {
        if (minecraft == null) return;

        // If we're in a world (singleplayer or multiplayer), just close this GUI and
        // show an in-game message. Let the player leave to title / restart manually.
        if (minecraft.level != null) {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(
                    Component.translatable("gui.mocreatures.saved_restart_required"),
                    false
                );
            }
            minecraft.setScreen(null); // back to game, no disconnect / connection-lost screen
        } else {
            // If somehow opened from menus, just go back to title
            minecraft.setScreen(new TitleScreen());
        }
    }

    private void enforceMinMaxSpawnOrder(BiomeSpawnConfig.CreatureSpawnData data, String changedKey, int newValue) {
        if (data == null) return;
        if ("minSpawn".equals(changedKey) && newValue > data.maxCount) {
            data.maxCount = Math.min(newValue, SPAWN_GROUP_MAX_MAX);
        } else if ("maxSpawn".equals(changedKey) && newValue < data.minCount) {
            data.minCount = Math.max(newValue, SPAWN_GROUP_MIN);
        }
    }

    private static Component getSpawnKeyTooltip(String spawnKey) {
        if (spawnKey == null) return Component.literal("");
        return switch (spawnKey) {
            case "canSpawn" -> Component.literal("Whether this creature can spawn. Click to toggle.");
            case "frequency" -> Component.literal("Spawn weight (relative chance vs other mobs). Higher = more common. 0 = disabled.");
            case "minSpawn" -> Component.literal("Minimum group size per spawn attempt.");
            case "maxSpawn" -> Component.literal("Maximum group size per spawn attempt. Must be ≥ minSpawn.");
            default -> Component.literal("");
        };
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, width / 2, 12, 0xFFFFFF);

        int listRight = getListRight();
        int maxScroll = getMaxScroll();
        boolean showScrollbar = maxScroll > 0;

        int headerCount = 0;
        for (int i = 0; i < lines.size(); i++) {
            int y = listTop - scrollOffset + i * LINE_HEIGHT;
            if (y + LINE_HEIGHT < listTop || y > listBottom) {
                if (lines.get(i).isHeader) headerCount++;
                continue;
            }
            Line line = lines.get(i);
            if (line.isHeader) {
                headerCount++;
                graphics.fill(PADDING, y, listRight, y + LINE_HEIGHT, BG_HEADER);
                graphics.drawString(this.font, line.text, PADDING + 2, y + 2, HEADER_TEXT_COLOR, false);
            } else {
                if (viewMode == ViewMode.CREATURE_SPAWNS) {
                    int blockIndex = headerCount - 1;
                    if (blockIndex >= 0 && blockIndex % 2 == 1) {
                        graphics.fill(PADDING, y, listRight, y + LINE_HEIGHT, BG_ALT);
                    }
                }
                graphics.drawString(this.font, line.text, PADDING + 2, y + 2, NORMAL_TEXT_COLOR, false);
            }
        }

        if (showScrollbar) {
            int trackX = width - PADDING - SCROLLBAR_WIDTH;
            int trackTop = listTop;
            int trackBottom = listBottom;
            graphics.fill(trackX, trackTop, trackX + SCROLLBAR_WIDTH, trackBottom, 0xFF404040);
            int totalContentHeight = lines.size() * LINE_HEIGHT;
            int visibleHeight = listBottom - listTop;
            int thumbHeight = Math.max(20, visibleHeight * visibleHeight / totalContentHeight);
            int thumbY = trackTop + (scrollOffset * (trackBottom - trackTop - thumbHeight) / Math.max(1, maxScroll));
            graphics.fill(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumbHeight, 0xFF808080);
        }

        super.render(graphics, mouseX, mouseY, partialTicks);

        if (viewMode == ViewMode.CREATURE_SPAWNS && mouseX >= PADDING && mouseX <= listRight && mouseY >= listTop && mouseY <= listBottom) {
            int lineIndex = (mouseY - listTop + scrollOffset) / LINE_HEIGHT;
            if (lineIndex >= 0 && lineIndex < lines.size()) {
                Line line = lines.get(lineIndex);
                if (line.spawnKey != null) {
                    graphics.renderTooltip(this.font, getSpawnKeyTooltip(line.spawnKey), mouseX, mouseY);
                }
            }
        }
    }

    /** Returns true when the scrollbar is visible and (mouseX, mouseY) is over the scrollbar thumb. */
    private boolean isOverScrollbarThumb(double mouseX, double mouseY) {
        int maxScroll = getMaxScroll();
        if (maxScroll <= 0) return false;
        int trackX = width - PADDING - SCROLLBAR_WIDTH;
        int trackTop = listTop;
        int trackBottom = listBottom;
        int totalContentHeight = lines.size() * LINE_HEIGHT;
        int visibleHeight = listBottom - listTop;
        int thumbHeight = Math.max(20, visibleHeight * visibleHeight / totalContentHeight);
        int thumbY = trackTop + (scrollOffset * (trackBottom - trackTop - thumbHeight) / Math.max(1, maxScroll));
        return mouseX >= trackX && mouseX <= trackX + SCROLLBAR_WIDTH
                && mouseY >= thumbY && mouseY <= thumbY + thumbHeight;
    }

    /** Updates scrollOffset from mouse Y during scrollbar drag (thumb center follows mouse). */
    private void updateScrollFromDrag(double mouseY) {
        int maxScroll = getMaxScroll();
        if (maxScroll <= 0) return;
        int trackTop = listTop;
        int trackBottom = listBottom;
        int visibleHeight = listBottom - listTop;
        int totalContentHeight = lines.size() * LINE_HEIGHT;
        int thumbHeight = Math.max(20, visibleHeight * visibleHeight / totalContentHeight);
        int trackHeight = trackBottom - trackTop - thumbHeight;
        if (trackHeight <= 0) return;
        double thumbY = mouseY - thumbHeight * 0.5;
        thumbY = Math.max(trackTop, Math.min(trackBottom - thumbHeight, thumbY));
        scrollOffset = (int) Math.round((thumbY - trackTop) * maxScroll / trackHeight);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isOverScrollbarThumb(mouseX, mouseY)) {
            scrollbarDragging = true;
            return true;
        }
        int listRight = getListRight();
        if (mouseX >= PADDING && mouseX <= listRight && mouseY >= listTop && mouseY <= listBottom) {
            int lineIndex = (int) ((mouseY - listTop + scrollOffset) / LINE_HEIGHT);
            if (lineIndex >= 0 && lineIndex < lines.size()) {
                Line line = lines.get(lineIndex);
                if (line.spawnKey != null && line.spawnCategoryName != null && line.property == null) {
                    BiomeSpawnConfig.CreatureSpawnData data = BiomeSpawnConfig.getSpawnData(line.spawnCategoryName);
                    if (data != null) {
                        if ("canSpawn".equals(line.spawnKey) && button == 0) {
                            data.enabled = !data.enabled;
                            try { BiomeSpawnConfig.saveConfig(); } catch (IOException e) { MoCreatures.LOGGER.error("Save failed", e); }
                            buildLines();
                            return true;
                        }
                        if ("frequency".equals(line.spawnKey) || "minSpawn".equals(line.spawnKey) || "maxSpawn".equals(line.spawnKey)) {
                            int cur = "frequency".equals(line.spawnKey) ? data.weight : "minSpawn".equals(line.spawnKey) ? data.minCount : data.maxCount;
                            int delta = (button == 0) ? 1 : -1;
                            int minVal = "frequency".equals(line.spawnKey) ? FREQUENCY_MIN : SPAWN_GROUP_MIN;
                            int maxVal = "frequency".equals(line.spawnKey) ? FREQUENCY_MAX : "minSpawn".equals(line.spawnKey) ? SPAWN_GROUP_MAX_MIN : SPAWN_GROUP_MAX_MAX;
                            int next = Math.max(minVal, Math.min(maxVal, cur + delta));
                            if ("frequency".equals(line.spawnKey)) data.weight = next;
                            else if ("minSpawn".equals(line.spawnKey)) { data.minCount = next; enforceMinMaxSpawnOrder(data, "minSpawn", next); }
                            else { data.maxCount = next; enforceMinMaxSpawnOrder(data, "maxSpawn", next); }
                            try { BiomeSpawnConfig.saveConfig(); } catch (IOException e) { MoCreatures.LOGGER.error("Save failed", e); }
                            buildLines();
                            return true;
                        }
                    }
                }
                if (line.property != null) {
                    MoCProperty prop = line.property;
                    if (prop.getTypeMoC() == MoCProperty.Type.BOOLEAN) {
                        if (button == 0) {
                            prop.set(!prop.getBoolean(false));
                            buildLines();
                        }
                        return true;
                    }
                    if (prop.getTypeMoC() == MoCProperty.Type.INTEGER) {
                        int cur = prop.getInt(0);
                        int delta = (button == 0) ? 1 : -1;
                        int next = Math.max(GLOBAL_INT_MIN, Math.min(GLOBAL_INT_MAX, cur + delta));
                        prop.set(next);
                        buildLines();
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) scrollbarDragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrollbarDragging && button == 0) {
            updateScrollFromDrag(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private static final int SCROLL_LINES_PER_STEP = 6;

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollAmount) {
        int listRight = getListRight();
        if (mouseX >= PADDING && mouseX <= listRight && mouseY >= listTop && mouseY <= listBottom) {
            int maxScroll = getMaxScroll();
            int delta = (int) Math.signum(scrollAmount) * SCROLL_LINES_PER_STEP;
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - delta));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollAmount);
    }
}
