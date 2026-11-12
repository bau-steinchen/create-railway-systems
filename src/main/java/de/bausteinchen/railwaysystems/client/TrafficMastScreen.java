package de.bausteinchen.railwaysystems.client;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import de.bausteinchen.railwaysystems.RailwaySystems;
import de.bausteinchen.railwaysystems.network.MastConfigPayload;
import de.bausteinchen.railwaysystems.traffic.MastConfig;
import de.bausteinchen.railwaysystems.traffic.TrafficMastBlock;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Menü für Höhe, Ausladung und Seite eines Peitschenmastes, dazu beim Ampelmast der Signalgeber am Mast
 * und beim Zebrastreifen-Mast die Höhe des Schildes. Gespeichert wird beim Schließen: für das Item in der
 * Hand oder, mit Position, für einen platzierten Mast.
 */
public class TrafficMastScreen extends Screen {

	private static final int WIDTH = 300;
	private static final int HEIGHT = 150;
	private static final String KEY = RailwaySystems.MOD_ID + ".traffic_mast.screen.";

	@Nullable
	private final BlockPos pos;
	private final MastConfig original;
	private final boolean crosswalk;
	private int mastHeight;
	private int reach;
	private boolean right;
	private boolean mastHead;
	private int signHeight;
	private int left;
	private int top;

	public TrafficMastScreen(@Nullable BlockPos pos, MastConfig config, boolean crosswalk) {
		super(Component.translatable(KEY + (crosswalk ? "title_crosswalk" : "title")));
		this.pos = pos;
		this.original = config;
		this.crosswalk = crosswalk;
		signHeight = config.signHeight();
		mastHeight = config.height();
		reach = config.reach();
		right = config.right();
		mastHead = config.mastHead();
	}

	@Override
	protected void init() {
		left = (width - WIDTH) / 2;
		top = (height - HEIGHT) / 2;
		int x = left + 90;
		spinner(x, top + 26, -1, () -> {
			mastHeight = Mth.clamp(mastHeight - 1, MastConfig.MIN_HEIGHT, MastConfig.MAX_HEIGHT);
			signHeight = Math.min(signHeight, MastConfig.maxSignHeight(mastHeight));
		});
		spinner(x + 46, top + 26, 1, () -> mastHeight = Mth.clamp(mastHeight + 1, MastConfig.MIN_HEIGHT, MastConfig.MAX_HEIGHT));
		spinner(x, top + 48, -1, () -> reach = Mth.clamp(reach - 1, MastConfig.MIN_REACH, MastConfig.MAX_REACH));
		spinner(x + 46, top + 48, 1, () -> reach = Mth.clamp(reach + 1, MastConfig.MIN_REACH, MastConfig.MAX_REACH));
		addRenderableWidget(Button.builder(Component.translatable(KEY + (right ? "right" : "left")), b -> {
			right = !right;
			rebuildWidgets();
		})
			.bounds(x, top + 70, 60, 16)
			.build());
		if (crosswalk) {
			spinner(x, top + 92, -1, () -> signHeight = Mth.clamp(signHeight - 1, MastConfig.MIN_SIGN_HEIGHT,
				MastConfig.maxSignHeight(mastHeight)));
			spinner(x + 46, top + 92, 1, () -> signHeight = Mth.clamp(signHeight + 1, MastConfig.MIN_SIGN_HEIGHT,
				MastConfig.maxSignHeight(mastHeight)));
		} else
			addRenderableWidget(Button.builder(Component.translatable(KEY + (mastHead ? "yes" : "no")), b -> {
				mastHead = !mastHead;
				rebuildWidgets();
			})
				.bounds(x, top + 92, 60, 16)
				.build());
		addRenderableWidget(Button.builder(Component.translatable(KEY + "done"), b -> onClose())
			.bounds(left + 10, top + HEIGHT - 26, 140, 18)
			.build());
	}

	private void spinner(int x, int y, int delta, Runnable action) {
		addRenderableWidget(Button.builder(Component.literal(delta < 0 ? "-" : "+"), b -> action.run())
			.bounds(x, y, 14, 16)
			.build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawString(font, title, left + 10, top + 8, 0xFFFFFF);
		label(graphics, "height", top + 30);
		label(graphics, "reach", top + 52);
		label(graphics, "side", top + 74);
		label(graphics, crosswalk ? "sign_height" : "mast_head", top + 96);
		if (crosswalk)
			value(graphics, signHeight, top + 96);
		value(graphics, mastHeight, top + 30);
		value(graphics, reach, top + 52);
		drawSketch(graphics, left + 165, top + 20, 125, 120);
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(graphics, mouseX, mouseY, partialTick);
		graphics.fill(left, top, left + WIDTH, top + HEIGHT, 0xE0101418);
		graphics.renderOutline(left, top, WIDTH, HEIGHT, 0xFF5A6470);
	}

	private void label(GuiGraphics graphics, String key, int y) {
		graphics.drawString(font, Component.translatable(KEY + key), left + 10, y, 0xC8C8C8);
	}

	private void value(GuiGraphics graphics, int value, int y) {
		String text = String.valueOf(value);
		graphics.drawString(font, text, left + 90 + 30 - font.width(text) / 2, y, 0xFFFFFF);
	}

	/**
	 * Seitenansicht aus Sicht des Fahrers: Mast, Bogen, Ausleger und Signalgeber, maßstäblich.
	 */
	private void drawSketch(GuiGraphics g, int x, int y, int w, int h) {
		g.fill(x, y, x + w, y + h, 0xFF1C2228);
		double radius = TrafficMastBlock.BEND_RADIUS;
		double spanX = reach + 1.5;
		double spanY = mastHeight + radius + 0.5;
		// oben Platz für die Beschriftung lassen
		double scale = Math.min((w - 10) / spanX, (h - 20) / spanY);
		int ground = y + h - 5;
		// Mast steht aus Fahrersicht am Rand; Ausleger zeigt zur Fahrbahnmitte
		int poleX = right ? x + 5 + (int) (0.5 * scale) : x + w - 5 - (int) (0.5 * scale);
		int dir = right ? 1 : -1;
		int thick = Math.max(2, (int) (scale * 0.25));
		g.fill(x, ground, x + w, ground + 1, 0xFF6A6A6A);
		// Fahrbahn unter dem Ausleger
		int roadEnd = poleX + dir * (int) (reach * scale + scale);
		g.fill(Math.min(poleX, roadEnd), ground - 1, Math.max(poleX, roadEnd), ground, 0xFF404448);
		int poleTop = ground - (int) (mastHeight * scale);
		g.fill(poleX - thick / 2, poleTop, poleX - thick / 2 + thick, ground, 0xFFB4BAC0);
		// Bogen als Polygonzug aus kleinen Rechtecken
		int steps = 12;
		for (int i = 0; i <= steps; i++) {
			double t = i / (double) steps * Math.PI / 2;
			int px = poleX + dir * (int) (radius * (1 - Math.cos(t)) * scale);
			int py = poleTop - (int) (radius * Math.sin(t) * scale);
			g.fill(px - thick / 2, py - thick / 2, px - thick / 2 + thick, py - thick / 2 + thick, 0xFFB4BAC0);
		}
		int armY = poleTop - (int) (radius * scale);
		int armStart = poleX + dir * (int) (radius * scale);
		int tip = poleX + dir * (int) (reach * scale);
		g.fill(Math.min(armStart, tip + dir * thick), armY - thick / 2, Math.max(armStart, tip + dir * thick),
			armY - thick / 2 + thick, 0xFFB4BAC0);
		double clearance;
		if (crosswalk) {
			// Leuchte (12 px) am Ausleger, Schild (10 px) vorn am Mast
			int lamp = Math.max(6, (int) (scale * 0.75));
			g.fill(tip - lamp / 2, armY, tip + lamp / 2 + 1, armY + lamp, 0xFF1F5FB4);
			g.fill(tip - lamp / 2, armY + lamp, tip + lamp / 2 + 1, armY + lamp + 1, 0xFFFFF2C0);
			int sign = Math.max(5, (int) (scale * 10 / 16));
			int signBottom = ground - (int) (signHeight * scale);
			g.fill(poleX - sign / 2, signBottom - sign, poleX - sign / 2 + sign, signBottom, 0xFF1F5FB4);
			g.renderOutline(poleX - sign / 2, signBottom - sign, sign, sign, 0xFFFFFFFF);
			clearance = mastHeight + radius - 1;
		} else {
			drawHead(g, tip, armY + thick / 2, scale);
			if (mastHead)
				drawHead(g, poleX, ground - (int) (2 * scale), scale);
			clearance = mastHeight + radius - 1.25;
		}
		String text = Component.translatable(KEY + "clearance", clearance).getString();
		g.drawString(font, text, x + 3, y + 3, 0x9AA4AE);
	}

	private void drawHead(GuiGraphics g, int cx, int topY, double scale) {
		int hw = Math.max(3, (int) (scale * 0.2));
		int hh = Math.max(9, (int) (scale * 1.0));
		g.fill(cx - hw, topY, cx + hw, topY + hh, 0xFF15181B);
		int lamp = Math.max(1, hh / 3 - 1);
		int[] colors = { 0xFFE02020, 0xFFE0B020, 0xFF20C040 };
		for (int i = 0; i < 3; i++) {
			int ly = topY + 1 + i * (hh / 3);
			g.fill(cx - hw + 1, ly, cx + hw - 1, ly + lamp, colors[i]);
		}
	}

	@Override
	public void removed() {
		super.removed();
		MastConfig config = new MastConfig(mastHeight, reach, right, mastHead, signHeight).clamped();
		if (!config.equals(original) || pos == null)
			PacketDistributor.sendToServer(new MastConfigPayload(Optional.ofNullable(pos), config));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

}
