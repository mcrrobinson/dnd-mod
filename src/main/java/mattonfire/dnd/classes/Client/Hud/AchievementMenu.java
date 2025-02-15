package mattonfire.dnd.classes.client.Hud;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

public class AchievementMenu extends Screen {
    private final int totalAchievements = 50;

    public AchievementMenu() {
        super(Text.of("Achievements"));
    }

    @Override
    protected void init() {
        super.init();

        // Get screen size dynamically
        int screenWidth = this.width;
        int screenHeight = this.height;

        // Calculate dynamic spacing based on screen height
        int titleSpacing = (int) (screenHeight * 0.05); // 5% of screen height as spacing

        // Dynamic grid layout calculations
        int columns = 3;
        int cellHeight = 25;

        DndCharacter[] characters = DndCharacter.values();
        int totalCharacters = characters.length;
        int rows = (int) Math.ceil(totalCharacters / (float) columns);

        int totalGridHeight = rows * cellHeight;

        int startGridY = (screenHeight / 5) + titleSpacing; // Shift down by title spacing

        // Position Close Button dynamically below grid
        int closeButtonWidth = 120;
        int closeButtonHeight = 20;
        int closeButtonX = screenWidth / 2 - closeButtonWidth / 2;
        int closeButtonY = startGridY + totalGridHeight + titleSpacing; // Use titleSpacing for consistent gap

        this.addDrawableChild(ButtonWidget.builder(Text.of("Close"), event -> {
            this.client.setScreen(null);
        }).size(closeButtonWidth, closeButtonHeight).position(closeButtonX, closeButtonY).build());
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);

        PlayerEntity player = this.client.player;

        if (player instanceof PlayerEntityExt) {
            PlayerEntityExt playerExt = (PlayerEntityExt) player;

            DndCharacter[] characters = DndCharacter.values();
            int totalCharacters = characters.length;

            int screenWidth = this.width;
            int screenHeight = this.height;

            int columns = 3;
            int cellWidth = screenWidth / columns - 20;
            int cellHeight = 25;

            int titleSpacing = (int) (screenHeight * 0.05); // Scale title spacing dynamically

            int startGridX = (screenWidth - (columns * cellWidth)) / 2;
            int startGridY = (screenHeight / 5) + titleSpacing;

            int barWidth = (int) (screenWidth * 0.2);
            int barHeight = 6;

            // Draw title centered at the top, spaced dynamically
            drawCenteredTextWithShadow(matrices, this.textRenderer, "Character Progress", screenWidth / 2,
                    (screenHeight / 8), 0xFFFFFF);

            // Draw progress grid
            for (int i = 0; i < totalCharacters; i++) {
                DndCharacter character = characters[i];

                int row = i / columns;
                int col = i % columns;

                int cellX = startGridX + (col * cellWidth);
                int cellY = startGridY + (row * cellHeight);

                int achievedCount = playerExt.getProgress(character);
                int filledWidth = (int) ((achievedCount / (float) totalAchievements) * barWidth);

                String text = character + ": " + achievedCount + "/" + totalAchievements;
                int textX = cellX + (cellWidth / 2);
                int textY = cellY;

                drawCenteredTextWithShadow(matrices, this.textRenderer, text, textX, textY, 0xFFFFFF);

                int barX1 = textX - (barWidth / 2);
                int barY1 = textY + 10;
                int barX2 = barX1 + barWidth;
                int barY2 = barY1 + barHeight;

                fill(matrices, barX1, barY1, barX2, barY2, 0xFF000000);
                fill(matrices, barX1, barY1, barX1 + filledWidth, barY2, 0xFF00FF00);
            }
        }

        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}