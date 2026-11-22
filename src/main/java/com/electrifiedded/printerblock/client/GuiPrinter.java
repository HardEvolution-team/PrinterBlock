package com.electrifiedded.printerblock.client;

import com.electrifiedded.printerblock.PrintPlane;
import com.electrifiedded.printerblock.PrinterBlock;
import com.electrifiedded.printerblock.net.MessagePrintLayer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.block.BlockColored;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/**
 * Pixel editor: 44x44 cells per layer, 100 layers, 16 wool colours + "empty".
 * Tools: brush (sizes 1-5), replace colour, rectangle (two clicks), flood fill; copy/paste, undo/redo.
 * Layout follows the original GUI sheet (tlprinterblock:textures/gui/printerblock.png, served from the original jar).
 */
public class GuiPrinter extends GuiScreen {
    private static final ResourceLocation TEX = new ResourceLocation("tlprinterblock", "textures/gui/printerblock.png");
    private static final int W = PrintPlane.W, H = PrintPlane.H, N = PrintPlane.LAYERS, EMPTY = PrintPlane.EMPTY;
    private static final int X_SIZE = 230, Y_SIZE = 230, SIDE_W = 26, CELL = 5, GRID_OFF = 5, UNDO = 50;
    private static final int T_BRUSH = 0, T_REPLACE = 1, T_RECT = 2, T_RECT2 = 3, T_FILL = 4;

    private final BlockPos printerPos;
    private final byte[][] layers = new byte[N][W * H];
    private final boolean[] touched = new boolean[N];
    private final Deque<Snapshot> undo = new ArrayDeque<>(), redo = new ArrayDeque<>();
    private byte[] clipboard;
    private int color = 0, brush = 1, layer = 0, tool = T_BRUSH, rectX, rectY, hoverX = -1, hoverY = -1;

    private static final class Snapshot {
        final int layer; final byte[] data;
        Snapshot(int layer, byte[] data) { this.layer = layer; this.data = data; }
    }

    public GuiPrinter(World world, BlockPos printerPos, PrintPlane plane) {
        this.printerPos = printerPos;
        touched[0] = true;
        for (int l = 0; l < N; l++) {
            byte[] px = layers[l];
            for (int r = 0; r < H; r++) for (int c = 0; c < W; c++) {
                IBlockState s = world.getBlockState(plane.pos(c, r, l));
                px[r * W + c] = (byte) (s.getBlock() == Blocks.WOOL ? s.getValue(BlockColored.COLOR).getMetadata() : EMPTY);
            }
        }
    }

    private int left() { return (width - X_SIZE) / 2; }
    private int top() { return (height - Y_SIZE) / 4; }

    @Override
    public void initGui() {
        buttonList.clear();
        int x = left(), y = top(), r = x + X_SIZE + SIDE_W + 8;
        buttonList.add(new GuiButton(1, x - 52, y + 18, 50, 20, "Print"));
        buttonList.add(new GuiButton(2, x - 52, y + 43, 50, 20, "Clear"));
        buttonList.add(tool == T_BRUSH ? new GuiButton(12, x - 52, y + 68, 50, 20, "Replace") : new GuiButton(13, x - 52, y + 68, 50, 20, "Draw"));
        buttonList.add(new GuiButton(4, x - 48, y + 119, 20, 20, "-"));
        buttonList.add(new GuiButton(5, x - 27, y + 119, 20, 20, "+"));
        buttonList.add(new GuiButton(6, x - 48, y + 180, 20, 20, "-"));
        buttonList.add(new GuiButton(7, x - 27, y + 180, 20, 20, "+"));
        buttonList.add(new GuiButton(8, x - 48, y + 201, 20, 20, "--"));
        buttonList.add(new GuiButton(9, x - 27, y + 201, 20, 20, "++"));
        buttonList.add(new GuiButton(10, r, y + 50, 50, 20, "Copy"));
        buttonList.add(new GuiButton(11, r, y + 75, 50, 20, "Paste"));
        buttonList.add(new GuiButton(14, r, y + 100, 50, 20, "Undo"));
        buttonList.add(new GuiButton(15, r, y + 125, 50, 20, "Redo"));
        buttonList.add(new GuiButton(16, r, y + 150, 50, 20, "Rectangle"));
        buttonList.add(new GuiButton(17, r, y + 175, 50, 20, "Fill"));
    }

    private void setTool(int t) { tool = t; initGui(); }

    @Override
    protected void actionPerformed(GuiButton b) {
        switch (b.id) {
            case 1: print(); mc.displayGuiScreen(null); break;
            case 2: snapshot(); Arrays.fill(layers[layer], (byte) EMPTY); touched[layer] = true; break;
            case 4: brush = Math.max(1, brush - 1); break;
            case 5: brush = Math.min(5, brush + 1); break;
            case 6: layer = Math.max(0, layer - 1); break;
            case 7: layer = Math.min(N - 1, layer + 1); break;
            case 8: layer = Math.max(0, layer - 10); break;
            case 9: layer = Math.min(N - 1, layer + 10); break;
            case 10: copy(); break;
            case 11: paste(); break;
            case 12: setTool(T_REPLACE); break;
            case 13: setTool(T_BRUSH); break;
            case 14: step(undo, redo); break;
            case 15: step(redo, undo); break;
            case 16: setTool(T_RECT); break;
            case 17: setTool(T_FILL); break;
            default: break;
        }
    }

    private void snapshot() {
        undo.push(new Snapshot(layer, layers[layer].clone()));
        while (undo.size() > UNDO) undo.removeLast();
        redo.clear();
    }

    private void step(Deque<Snapshot> from, Deque<Snapshot> to) {
        if (from.isEmpty()) return;
        Snapshot s = from.pop();
        to.push(new Snapshot(s.layer, layers[s.layer].clone()));
        System.arraycopy(s.data, 0, layers[s.layer], 0, s.data.length);
        touched[s.layer] = true;
        layer = s.layer;
    }

    private void copy() { clipboard = layers[layer].clone(); }

    private void paste() {
        if (clipboard == null) return;
        snapshot();
        System.arraycopy(clipboard, 0, layers[layer], 0, clipboard.length);
        touched[layer] = true;
    }

    private void print() {
        for (int l = 0; l < N; l++) {
            if (touched[l]) PrinterBlock.NET.sendToServer(new MessagePrintLayer(printerPos, l, layers[l].clone()));
        }
    }

    private int cellAt(int mx, int my) {
        int cx = Math.floorDiv(mx - left() - GRID_OFF, CELL), cy = Math.floorDiv(my - top() - GRID_OFF, CELL);
        return cx >= 0 && cx < W && cy >= 0 && cy < H ? cy * W + cx : -1;
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        super.mouseClicked(mx, my, button);
        if (button != 0) return;
        int cell = cellAt(mx, my);
        if (cell >= 0) paint(cell % W, cell / W);
        int px = left() + X_SIZE + 13;
        if (mx > px && mx < px + 10) {
            if (my > top() + 35 && my < top() + 45) color = EMPTY;
            for (int c = 0; c < 16; c++) {
                int y0 = top() + 46 + c * 11;
                if (my > y0 && my < y0 + 10) color = c;
            }
        }
    }

    private void paint(int cx, int cy) {
        byte[] px = layers[layer];
        byte col = (byte) color;
        switch (tool) {
            case T_BRUSH:
                snapshot();
                for (int dy = 1 - brush; dy < brush; dy++) for (int dx = 1 - brush; dx < brush; dx++) {
                    int x = cx + dx, y = cy + dy;
                    if (x >= 0 && x < W && y >= 0 && y < H) px[y * W + x] = col;
                }
                break;
            case T_REPLACE:
                snapshot();
                byte from = px[cy * W + cx];
                for (int i = 0; i < px.length; i++) if (px[i] == from) px[i] = col;
                break;
            case T_RECT:
                rectX = cx; rectY = cy; tool = T_RECT2;
                return;
            case T_RECT2:
                snapshot();
                for (int y = Math.min(cy, rectY); y <= Math.max(cy, rectY); y++)
                    for (int x = Math.min(cx, rectX); x <= Math.max(cx, rectX); x++) px[y * W + x] = col;
                tool = T_RECT;
                break;
            case T_FILL:
                if (px[cy * W + cx] == col) return;
                snapshot();
                flood(px, cx, cy, px[cy * W + cx], col);
                break;
            default: return;
        }
        touched[layer] = true;
    }

    private static void flood(byte[] px, int sx, int sy, byte from, byte to) {
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(sy * W + sx);
        while (!stack.isEmpty()) {
            int i = stack.pop();
            if (px[i] != from) continue;
            px[i] = to;
            int x = i % W, y = i / W;
            if (x > 0) stack.push(i - 1);
            if (x < W - 1) stack.push(i + 1);
            if (y > 0) stack.push(i - W);
            if (y < H - 1) stack.push(i + W);
        }
    }

    private void cell(int x0, int y0, int cx, int cy, int c, int row) {
        drawTexturedModalRect(x0 + GRID_OFF + cx * CELL, y0 + GRID_OFF + cy * CELL, c * CELL, Y_SIZE + row, CELL, CELL);
    }

    private void preview(int x0, int y0, int cx, int cy) {
        cell(x0, y0, cx, cy, color, 10);
        cell(x0, y0, cx, cy, 16, 10); // highlight frame
    }

    @Override
    public void drawScreen(int mx, int my, float partial) {
        drawDefaultBackground();
        GlStateManager.color(1F, 1F, 1F, 1F);
        mc.getTextureManager().bindTexture(TEX);
        int x0 = left(), y0 = top();
        drawTexturedModalRect(x0, y0, 0, 0, X_SIZE, Y_SIZE);
        byte[] px = layers[layer], below = layer > 0 ? layers[layer - 1] : null;
        for (int cy = 0; cy < H; cy++) for (int cx = 0; cx < W; cx++) {
            int v = px[cy * W + cx];
            // empty cells show the layer below as a faint ghost
            if (v == EMPTY && below != null && below[cy * W + cx] != EMPTY) cell(x0, y0, cx, cy, below[cy * W + cx], 15);
            cell(x0, y0, cx, cy, v, 10);
        }
        int hover = cellAt(mx, my);
        if (hover >= 0) {
            int hx = hover % W, hy = hover / W;
            if (tool == T_BRUSH) {
                for (int dy = 1 - brush; dy < brush; dy++) for (int dx = 1 - brush; dx < brush; dx++) {
                    int x = hx + dx, y = hy + dy;
                    if (x >= 0 && x < W && y >= 0 && y < H) preview(x0, y0, x, y);
                }
            } else if (tool == T_RECT2) {
                for (int y = Math.min(hy, rectY); y <= Math.max(hy, rectY); y++)
                    for (int x = Math.min(hx, rectX); x <= Math.max(hx, rectX); x++) preview(x0, y0, x, y);
            }
        }
        // palette column
        drawTexturedModalRect(x0 + X_SIZE + 5, y0, X_SIZE, 0, SIDE_W, Y_SIZE);
        for (int k = 0; k < 17; k++) drawTexturedModalRect(x0 + X_SIZE + 13, y0 + 35 + k * 11, k * 10, Y_SIZE, 10, 10);
        drawTexturedModalRect(x0 + X_SIZE + 13, y0 + 8, color == EMPTY ? 0 : 10 + 10 * color, Y_SIZE, 10, 10);
        // left panel backgrounds
        int lp = X_SIZE - 56;
        drawTexturedModalRect(x0 - 55, y0 + 89, lp, Y_SIZE, 56, 22);
        drawTexturedModalRect(x0 - 55, y0 + 111, lp, Y_SIZE + 4, 56, 15);
        drawTexturedModalRect(x0 - 55, y0 + 126, lp, Y_SIZE + 4, 56, 22);
        drawTexturedModalRect(x0 - 55, y0 + 150, lp, Y_SIZE, 56, 22);
        drawTexturedModalRect(x0 - 55, y0 + 172, lp, Y_SIZE + 4, 56, 15);
        drawTexturedModalRect(x0 - 55, y0 + 187, lp, Y_SIZE + 4, 56, 18);
        drawTexturedModalRect(x0 - 55, y0 + 205, lp, Y_SIZE + 4, 56, 22);
        drawTexturedModalRect(x0 - 40, y0 + 164, X_SIZE, Y_SIZE, 26, 18);
        int ink = 0x101010;
        fontRenderer.drawString(String.valueOf(brush), x0 + X_SIZE + 15, y0 + 20, ink);
        fontRenderer.drawString("Brush", x0 - 41, y0 + 96, ink);
        fontRenderer.drawString("Size", x0 - 38, y0 + 106, ink);
        fontRenderer.drawString(String.valueOf(layer), x0 - 33, y0 + 168, ink);
        fontRenderer.drawString("Layer", x0 - 41, y0 + 155, ink);
        super.drawScreen(mx, my, partial);
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        super.keyTyped(c, key);
        switch (key) {
            case Keyboard.KEY_E: setTool(T_RECT); break;
            case Keyboard.KEY_R: setTool(T_REPLACE); break;
            case Keyboard.KEY_D: setTool(T_BRUSH); break;
            case Keyboard.KEY_C: copy(); break;
            case Keyboard.KEY_V: paste(); break;
            default: break;
        }
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
