package thunder.hack.gui.misc;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.utility.render.Render2DEngine;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static thunder.hack.features.modules.Module.mc;

public class GuiScanner extends Screen {

    public static boolean track = false;
    public static boolean busy = false;

    int radarx, radary, radarx1, radary1;
    int centerx, centery;
    int consolex, consoley, consolex1, consoley1;
    int hoverx, hovery, searchx, searchy, wheely;

    // ===== DUMMY DATA (KHÔNG CÒN NoCommentExploit) =====
    private static final List<Dot> dots = new ArrayList<>();
    private static final List<Cout> consoleout = new ArrayList<>();
    private static int couti = 1;

    public GuiScanner() {
        super(Text.of("GuiScanner"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (mc.player == null) return;

        radarx = mc.getWindow().getScaledWidth() / 8;
        radarx1 = (mc.getWindow().getScaledWidth() * 5) / 8;
        radary = (mc.getWindow().getScaledHeight() / 2) - ((radarx1 - radarx) / 2);
        radary1 = (mc.getWindow().getScaledHeight() / 2) + ((radarx1 - radarx) / 2);

        centerx = (radarx + radarx1) / 2;
        centery = (radary + radary1) / 2;

        consolex = (int) ((mc.getWindow().getScaledWidth() * 5.5f) / 8f);
        consolex1 = mc.getWindow().getScaledWidth() - 50;
        consoley = radary;
        consoley1 = radary1 - 50;

        // ===== CONSOLE =====
        Render2DEngine.drawRectDumbWay(context.getMatrices(), consolex, consoley, consolex1, consoley1, new Color(0xF70C0C0C, true));
        FontRenderers.monsterrat.drawString(
                context.getMatrices(),
                "cursor pos: " + hoverx * 64 + "x " + hovery * 64 + "z",
                consolex + 4, consoley1 + 6, -1
        );

        // ===== RADAR =====
        Render2DEngine.drawRectDumbWay(context.getMatrices(), radarx, radary, radarx1, radary1, new Color(0xE0151515, true));

        for (Dot d : dots) {
            Render2DEngine.drawRectDumbWay(
                    context.getMatrices(),
                    (d.x / 4f) + centerx,
                    (d.z / 4f) + centery,
                    (d.x / 4f) + centerx + 2,
                    (d.z / 4f) + centery + 2,
                    d.color
            );
        }

        Render2DEngine.drawRectDumbWay(
                context.getMatrices(),
                centerx - 1, centery - 1, centerx + 1, centery + 1,
                new Color(0xFF0303)
        );

        // ===== HOVER =====
        if (mouseX > radarx && mouseX < radarx1 && mouseY > radary && mouseY < radary1) {
            hoverx = mouseX - centerx;
            hovery = mouseY - centery;
        }

        // ===== CONSOLE TEXT =====
        Render2DEngine.addWindow(context.getMatrices(), consolex, consoley, consolex1, consoley1 - 10, 1f);
        for (Cout c : consoleout) {
            FontRenderers.monsterrat.drawString(
                    context.getMatrices(),
                    c.text,
                    consolex + 4,
                    consoley + 6 + (c.line * 11) + wheely,
                    -1
            );
        }
        Render2DEngine.popWindow();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX > radarx && mouseX < radarx1 && mouseY > radary && mouseY < radary1) {
            searchx = (int) (mouseX - centerx);
            searchy = (int) (mouseY - centery);

            dots.add(new Dot(searchx, searchy, new Color(0x3CE708)));
            consoleout.add(new Cout(couti++, "Selected pos " + searchx * 64 + "x " + searchy * 64 + "z"));
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        wheely += (int) (v * 5D);
        return super.mouseScrolled(mouseX, mouseY, h, v);
    }

    // ===== INTERNAL CLASSES =====
    private record Dot(int x, int z, Color color) {}
    private record Cout(int line, String text) {}
                        }
