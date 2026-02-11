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
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.TitleScreen;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@OnlyIn(Dist.CLIENT)
public class MoCGUISettings extends Screen {

    private static final int LINE_HEIGHT = 12;
    private static final int PADDING = 10;
    private static final int TITLE_HEIGHT = 30;
    /** Extra height for the creature-spawn action row + search + summary line (below tabs). */
    private static final int TOP_ACTION_ROW_HEIGHT = 72;
    /** On Global tab we only have one button row (no search), so list starts lower. */
    private static final int TOP_ACTION_ROW_HEIGHT_GLOBAL = 44;
    private static final int BOTTOM_BUTTON_HEIGHT = 30;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_PAD = 2;

    private static final int FREQUENCY_MIN = 0;
    private static final int FREQUENCY_MAX = 16;
    // Vanilla-ish spawn group sizes: 1–8
    private static final int SPAWN_GROUP_MIN = 1;
    private static final int SPAWN_GROUP_MAX_MIN = 8;  // upper bound for minSpawn
    private static final int SPAWN_GROUP_MAX_MAX = 8;  // upper bound for maxSpawn
    private static final int GLOBAL_INT_MIN = 0;
    private static final int GLOBAL_INT_MAX = 100;

    private static final int BG_HEADER = 0x18FFFFFF;
    private static final int BG_ALT = 0x0AFFFFFF;
    private static final int HEADER_TEXT_COLOR = 0xFFE0E0A0;
    private static final int NORMAL_TEXT_COLOR = 0xFFCCCCCC;
    private static final int SPAWN_ENABLED_COLOR = 0xFF55FF55;   // green
    private static final int SPAWN_DISABLED_COLOR = 0xFFAA5555; // muted red
    /** Column X end positions for creature table (aligned values). Tameable is last to avoid overlap. */
    private static final int COL_CREATURE_END = 118;
    private static final int COL_SPAWN_END = 162;   // wide enough for "Spawn"
    private static final int COL_FREQ_END = 192;    // wide enough for "Freq"
    private static final int COL_MIN_END = 218;
    private static final int COL_MAX_END = 244;
    private static final int COL_TAMEABLE_END = 270; // last column: ✓/✖ for tameable

    public enum ViewMode {
        GLOBAL_SETTINGS,
        CREATURE_SPAWNS
    }

    public enum CreatureLineType {
        GLOBAL,
        TABLE_HEADER,
        CATEGORY_HEADER,
        CREATURE_ROW,
        CREATURE_DETAIL
    }

    public static final class Line {
        public final String text;
        public final boolean isHeader;
        public final MoCProperty property;
        public final String spawnKey;       // "canSpawn", "frequency", "minSpawn", "maxSpawn" or null
        public final String spawnCategoryName; // lowercase entity key for Creature tab
        /** When spawnKey is "canSpawn", true = enabled (green), false = disabled (red). Null otherwise. */
        public final Boolean canSpawnValue;
        /** Creature tab: type of line for column layout and expand. */
        public final CreatureLineType creatureLineType;
        /** Creature tab: category for section header (e.g. "CREATURE", "MONSTER"). */
        public final String categoryName;
        /** Creature tab row: display name, weight, min/max, tameable for columns. */
        public final String displayName;
        public final Integer rowWeight;
        public final Integer rowMin;
        public final Integer rowMax;
        /** Creature tab row: true = tameable, false = not. Null for non-row lines. */
        public final Boolean rowTameable;

        public Line(String text, boolean isHeader, MoCProperty property, String spawnKey, String spawnCategoryName) {
            this(text, isHeader, property, spawnKey, spawnCategoryName, null, CreatureLineType.GLOBAL, null, null, null, null, null, null);
        }

        public Line(String text, boolean isHeader, MoCProperty property, String spawnKey, String spawnCategoryName, Boolean canSpawnValue) {
            this(text, isHeader, property, spawnKey, spawnCategoryName, canSpawnValue, CreatureLineType.GLOBAL, null, null, null, null, null, null);
        }

        public Line(String text, boolean isHeader, MoCProperty property, String spawnKey, String spawnCategoryName, Boolean canSpawnValue,
                    CreatureLineType creatureLineType, String categoryName, String displayName, Integer rowWeight, Integer rowMin, Integer rowMax) {
            this(text, isHeader, property, spawnKey, spawnCategoryName, canSpawnValue, creatureLineType, categoryName, displayName, rowWeight, rowMin, rowMax, null);
        }

        public Line(String text, boolean isHeader, MoCProperty property, String spawnKey, String spawnCategoryName, Boolean canSpawnValue,
                    CreatureLineType creatureLineType, String categoryName, String displayName, Integer rowWeight, Integer rowMin, Integer rowMax, Boolean rowTameable) {
            this.text = text;
            this.isHeader = isHeader;
            this.property = property;
            this.spawnKey = spawnKey;
            this.spawnCategoryName = spawnCategoryName;
            this.canSpawnValue = canSpawnValue;
            this.creatureLineType = creatureLineType != null ? creatureLineType : CreatureLineType.GLOBAL;
            this.categoryName = categoryName;
            this.displayName = displayName;
            this.rowWeight = rowWeight;
            this.rowMin = rowMin;
            this.rowMax = rowMax;
            this.rowTameable = rowTameable;
        }
    }

    private ViewMode viewMode = ViewMode.GLOBAL_SETTINGS;
    private final List<Line> lines = new ArrayList<>();
    private int scrollOffset = 0;
    private int listTop;
    private int listBottom;
    /** On Creature spawns tab: number of creatures with spawn enabled/disabled (for summary line). */
    private int spawnEnabledCount = 0;
    private int spawnDisabledCount = 0;
    private boolean scrollbarDragging = false;
    /** Creature tab: which rows are expanded to show full details. */
    private final Set<String> expandedCreatures = new HashSet<>();
    /** Creature tab: search. */
    private String creatureSearch = "";
    private EditBox searchBox;
    private Button disableAllSpawnsButton;
    private Button enableAllSpawnsButton;
    private Button enableAllTameablesButton;
    private Button resetToDefaultButton;

    public MoCGUISettings() {
        super(Component.translatable("gui.mocreatures.settings"));
    }

    @Override
    protected void init() {
        super.init();
        listBottom = height - BOTTOM_BUTTON_HEIGHT - PADDING;
        updateListTop();

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

        // Second row: creature-spawn actions, left-aligned with tabs and content for a clean column
        int actionRowY = tabY + 24;
        int actionBtnW = 130;
        int actionGap = 8;
        int actionLeftX = PADDING;
        enableAllSpawnsButton = Button.builder(Component.translatable("gui.mocreatures.enable_all_spawns"), b -> onEnableAllSpawns())
            .bounds(actionLeftX, actionRowY, actionBtnW, 20)
            .build();
        enableAllSpawnsButton.visible = (viewMode == ViewMode.CREATURE_SPAWNS);
        addRenderableWidget(enableAllSpawnsButton);
        enableAllTameablesButton = Button.builder(Component.translatable("gui.mocreatures.enable_all_tameables"), b -> onEnableAllTameables())
            .bounds(actionLeftX + actionBtnW + actionGap, actionRowY, actionBtnW, 20)
            .build();
        enableAllTameablesButton.visible = (viewMode == ViewMode.CREATURE_SPAWNS);
        addRenderableWidget(enableAllTameablesButton);
        disableAllSpawnsButton = Button.builder(Component.translatable("gui.mocreatures.disable_all_spawns"), b -> onDisableAllSpawns())
            .bounds(actionLeftX + (actionBtnW + actionGap) * 2, actionRowY, actionBtnW, 20)
            .build();
        disableAllSpawnsButton.visible = (viewMode == ViewMode.CREATURE_SPAWNS);
        addRenderableWidget(disableAllSpawnsButton);

        resetToDefaultButton = Button.builder(Component.translatable("gui.mocreatures.reset_to_default"), b -> onResetToDefault())
            .bounds(actionLeftX, actionRowY, actionBtnW, 20)
            .build();
        resetToDefaultButton.visible = (viewMode == ViewMode.GLOBAL_SETTINGS);
        addRenderableWidget(resetToDefaultButton);

        int searchY = actionRowY + 22;
        searchBox = new EditBox(this.font, PADDING, searchY, Math.min(280, width - PADDING * 2 - 20), 18, Component.translatable("gui.mocreatures.search"));
        searchBox.setHint(Component.translatable("gui.mocreatures.search_hint"));
        searchBox.setValue(creatureSearch);
        searchBox.setResponder(s -> { creatureSearch = s; buildLines(); });
        searchBox.setVisible(viewMode == ViewMode.CREATURE_SPAWNS);
        addRenderableWidget(searchBox);

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

    private void updateListTop() {
        listTop = TITLE_HEIGHT + (viewMode == ViewMode.GLOBAL_SETTINGS ? TOP_ACTION_ROW_HEIGHT_GLOBAL : TOP_ACTION_ROW_HEIGHT) + PADDING;
    }

    private void switchTab(ViewMode mode) {
        viewMode = mode;
        scrollOffset = 0;
        updateListTop();
        boolean creatureTab = (viewMode == ViewMode.CREATURE_SPAWNS);
        if (disableAllSpawnsButton != null) disableAllSpawnsButton.visible = creatureTab;
        if (enableAllSpawnsButton != null) enableAllSpawnsButton.visible = creatureTab;
        if (enableAllTameablesButton != null) enableAllTameablesButton.visible = creatureTab;
        if (resetToDefaultButton != null) resetToDefaultButton.visible = (viewMode == ViewMode.GLOBAL_SETTINGS);
        if (searchBox != null) {
            searchBox.visible = creatureTab;
            if (creatureTab) searchBox.setValue(creatureSearch);
        }
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
        lines.clear();
        spawnEnabledCount = 0;
        spawnDisabledCount = 0;
        Set<String> tameable = BiomeSpawnConfig.getTameableCreatureNames();
        String search = (creatureSearch != null ? creatureSearch : "").trim().toLowerCase();

        List<String> creatureNames = new ArrayList<>(BiomeSpawnConfig.getAllCreatureNames());
        List<String> filtered = new ArrayList<>();
        for (String creatureName : creatureNames) {
            BiomeSpawnConfig.CreatureSpawnData data = BiomeSpawnConfig.getSpawnData(creatureName);
            if (data == null) continue;
            if (!search.isEmpty() && !creatureName.toLowerCase().contains(search) && !toDisplayName(creatureName).toLowerCase().contains(search)) continue;
            filtered.add(creatureName);
            if (data.enabled) spawnEnabledCount++; else spawnDisabledCount++;
        }

        lines.add(new Line("", false, null, null, null, null, CreatureLineType.TABLE_HEADER, null, null, null, null, null));

        for (String creatureName : filtered) {
            BiomeSpawnConfig.CreatureSpawnData data = BiomeSpawnConfig.getSpawnData(creatureName);
            if (data == null) continue;
            String displayName = toDisplayName(creatureName);
            boolean isTameable = tameable.contains(creatureName);
            lines.add(new Line(displayName, false, null, null, creatureName, data.enabled, CreatureLineType.CREATURE_ROW, null, displayName, data.weight, data.minCount, data.maxCount, isTameable));
            if (expandedCreatures.contains(creatureName)) {
                lines.add(new Line("  canSpawn [click to toggle]", false, null, "canSpawn", creatureName, data.enabled, CreatureLineType.CREATURE_DETAIL, null, null, null, null, null));
                lines.add(new Line("  frequency [0-16] \u2190 \u2192", false, null, "frequency", creatureName, null, CreatureLineType.CREATURE_DETAIL, null, null, null, null, null));
                lines.add(new Line("  minSpawn [1-8] \u2190 \u2192", false, null, "minSpawn", creatureName, null, CreatureLineType.CREATURE_DETAIL, null, null, null, null, null));
                lines.add(new Line("  maxSpawn [1-8] \u2190 \u2192", false, null, "maxSpawn", creatureName, null, CreatureLineType.CREATURE_DETAIL, null, null, null, null, null));
            }
        }
    }

    private String formatDetailLine(String spawnKey, BiomeSpawnConfig.CreatureSpawnData data) {
        if (spawnKey == null) return "";
        if (data == null) return "  " + spawnKey;
        return switch (spawnKey) {
            case "canSpawn" -> "  canSpawn = " + data.enabled + " [click to toggle]";
            case "frequency" -> "  frequency = " + data.weight + " [0-16] \u2190 \u2192";
            case "minSpawn" -> "  minSpawn = " + data.minCount + " [1-8] \u2190 \u2192";
            case "maxSpawn" -> "  maxSpawn = " + data.maxCount + " [1-8] \u2190 \u2192";
            default -> "  " + spawnKey;
        };
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

    private void onResetToDefault() {
        if (viewMode != ViewMode.GLOBAL_SETTINGS) return;
        MoCreatures.proxy.resetGlobalConfigToDefaults();
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

    private void onEnableAllSpawns() {
        for (String creatureName : BiomeSpawnConfig.getAllCreatureNames()) {
            BiomeSpawnConfig.CreatureSpawnData data = BiomeSpawnConfig.getSpawnData(creatureName);
            if (data != null) data.enabled = true;
        }
        try {
            BiomeSpawnConfig.saveConfig();
        } catch (IOException e) {
            MoCreatures.LOGGER.error("Failed to save MoCreatures.json", e);
        }
        buildLines();
    }

    private void onEnableAllTameables() {
        for (String creatureName : BiomeSpawnConfig.getTameableCreatureNames()) {
            BiomeSpawnConfig.CreatureSpawnData data = BiomeSpawnConfig.getSpawnData(creatureName);
            if (data != null) data.enabled = true;
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

        // Creature spawns tab: draw enabled/disabled count and keyboard hint
        if (viewMode == ViewMode.CREATURE_SPAWNS) {
            int summaryY = listTop - LINE_HEIGHT - 2;
            String summary = Component.translatable("gui.mocreatures.spawn_summary", spawnEnabledCount, spawnDisabledCount).getString();
            graphics.drawString(this.font, summary, PADDING, summaryY, NORMAL_TEXT_COLOR, false);
            int hintY = height - BOTTOM_BUTTON_HEIGHT - PADDING - LINE_HEIGHT - 2;
            graphics.drawString(this.font, Component.translatable("gui.mocreatures.keyboard_hint").getString(), PADDING, hintY, 0xFF888888, false);
        }

        int headerCount = 0;
        int creatureRowIndex = 0;
        for (int i = 0; i < lines.size(); i++) {
            int y = listTop - scrollOffset + i * LINE_HEIGHT;
            if (y + LINE_HEIGHT < listTop || y > listBottom) {
                if (i < lines.size() && lines.get(i).isHeader) headerCount++;
                continue;
            }
            Line line = lines.get(i);
            if (line.creatureLineType == CreatureLineType.TABLE_HEADER) {
                graphics.fill(PADDING, y, listRight, y + LINE_HEIGHT, BG_HEADER);
                graphics.drawString(this.font, Component.translatable("gui.mocreatures.column_creature").getString(), PADDING + 2, y + 2, HEADER_TEXT_COLOR, false);
                graphics.drawString(this.font, Component.translatable("gui.mocreatures.column_spawn").getString(), COL_CREATURE_END + 2, y + 2, HEADER_TEXT_COLOR, false);
                graphics.drawString(this.font, Component.translatable("gui.mocreatures.column_freq").getString(), COL_SPAWN_END + 2, y + 2, HEADER_TEXT_COLOR, false);
                graphics.drawString(this.font, Component.translatable("gui.mocreatures.column_min").getString(), COL_FREQ_END + 2, y + 2, HEADER_TEXT_COLOR, false);
                graphics.drawString(this.font, Component.translatable("gui.mocreatures.column_max").getString(), COL_MIN_END + 2, y + 2, HEADER_TEXT_COLOR, false);
                graphics.drawString(this.font, Component.translatable("gui.mocreatures.column_tameable").getString(), COL_MAX_END + 2, y + 2, HEADER_TEXT_COLOR, false);
            } else if (line.creatureLineType == CreatureLineType.CREATURE_ROW) {
                if (creatureRowIndex % 2 == 1) graphics.fill(PADDING, y, listRight, y + LINE_HEIGHT, BG_ALT);
                creatureRowIndex++;
                graphics.drawString(this.font, line.displayName != null ? line.displayName : line.text, PADDING + 2, y + 2, NORMAL_TEXT_COLOR, false);
                String spawnChar = (line.canSpawnValue != null && line.canSpawnValue) ? "\u2713" : "\u2717";
                int spawnColor = (line.canSpawnValue != null && line.canSpawnValue) ? SPAWN_ENABLED_COLOR : SPAWN_DISABLED_COLOR;
                graphics.drawString(this.font, spawnChar, COL_CREATURE_END + 2, y + 2, spawnColor, false);
                if (line.rowWeight != null) graphics.drawString(this.font, String.valueOf(line.rowWeight), COL_SPAWN_END + 2, y + 2, NORMAL_TEXT_COLOR, false);
                if (line.rowMin != null) graphics.drawString(this.font, String.valueOf(line.rowMin), COL_FREQ_END + 2, y + 2, NORMAL_TEXT_COLOR, false);
                if (line.rowMax != null) graphics.drawString(this.font, String.valueOf(line.rowMax), COL_MIN_END + 2, y + 2, NORMAL_TEXT_COLOR, false);
                if (line.rowTameable != null) {
                    String tameableChar = line.rowTameable ? "\u2713" : "\u2717";
                    graphics.drawString(this.font, tameableChar, COL_MAX_END + 2, y + 2, NORMAL_TEXT_COLOR, false);
                }
            } else if (line.creatureLineType == CreatureLineType.CREATURE_DETAIL) {
                graphics.fill(PADDING, y, listRight, y + LINE_HEIGHT, BG_ALT);
                BiomeSpawnConfig.CreatureSpawnData data = line.spawnCategoryName != null ? BiomeSpawnConfig.getSpawnData(line.spawnCategoryName) : null;
                String detailText = formatDetailLine(line.spawnKey, data);
                graphics.drawString(this.font, detailText, PADDING + 2, y + 2, NORMAL_TEXT_COLOR, false);
            } else if (line.isHeader) {
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
                int textColor = NORMAL_TEXT_COLOR;
                if (line.canSpawnValue != null) {
                    textColor = line.canSpawnValue ? SPAWN_ENABLED_COLOR : SPAWN_DISABLED_COLOR;
                }
                graphics.drawString(this.font, line.text, PADDING + 2, y + 2, textColor, false);
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
                    Component tip = line.creatureLineType == CreatureLineType.CREATURE_DETAIL && !"canSpawn".equals(line.spawnKey)
                        ? Component.translatable("gui.mocreatures.adjust_lr_tooltip")
                        : getSpawnKeyTooltip(line.spawnKey);
                    graphics.renderTooltip(this.font, tip, mouseX, mouseY);
                }
                if (line.creatureLineType == CreatureLineType.CREATURE_ROW && line.spawnCategoryName != null) {
                    if (mouseX < COL_CREATURE_END) graphics.renderTooltip(this.font, Component.translatable("gui.mocreatures.expand_tooltip"), mouseX, mouseY);
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
                if (viewMode == ViewMode.CREATURE_SPAWNS && line.creatureLineType == CreatureLineType.CREATURE_ROW && line.spawnCategoryName != null) {
                    if (button == 0) {
                        if (mouseX >= COL_CREATURE_END && mouseX < COL_SPAWN_END) {
                            BiomeSpawnConfig.CreatureSpawnData data = BiomeSpawnConfig.getSpawnData(line.spawnCategoryName);
                            if (data != null) {
                                data.enabled = !data.enabled;
                                try { BiomeSpawnConfig.saveConfig(); } catch (IOException e) { MoCreatures.LOGGER.error("Save failed", e); }
                                buildLines();
                                return true;
                            }
                        } else {
                            if (expandedCreatures.contains(line.spawnCategoryName)) expandedCreatures.remove(line.spawnCategoryName);
                            else expandedCreatures.add(line.spawnCategoryName);
                            buildLines();
                            return true;
                        }
                    }
                }
                if (line.creatureLineType == CreatureLineType.CREATURE_DETAIL && line.spawnKey != null && line.spawnCategoryName != null) {
                    BiomeSpawnConfig.CreatureSpawnData data = BiomeSpawnConfig.getSpawnData(line.spawnCategoryName);
                    if (data != null && button == 0) {
                        if ("canSpawn".equals(line.spawnKey)) {
                            data.enabled = !data.enabled;
                            try { BiomeSpawnConfig.saveConfig(); } catch (IOException e) { MoCreatures.LOGGER.error("Save failed", e); }
                            buildLines();
                            return true;
                        }
                        if ("frequency".equals(line.spawnKey) || "minSpawn".equals(line.spawnKey) || "maxSpawn".equals(line.spawnKey)) {
                            int cur = "frequency".equals(line.spawnKey) ? data.weight : "minSpawn".equals(line.spawnKey) ? data.minCount : data.maxCount;
                            int delta = 1;
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
                    if (data != null && button == 1) {
                        if ("frequency".equals(line.spawnKey) || "minSpawn".equals(line.spawnKey) || "maxSpawn".equals(line.spawnKey)) {
                            int cur = "frequency".equals(line.spawnKey) ? data.weight : "minSpawn".equals(line.spawnKey) ? data.minCount : data.maxCount;
                            int delta = -1;
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
                if (line.spawnKey != null && line.spawnCategoryName != null && line.property == null && line.creatureLineType == CreatureLineType.GLOBAL) {
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
