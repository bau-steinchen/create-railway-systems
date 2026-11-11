package de.bausteinchen.railwaysystems.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import de.bausteinchen.railwaysystems.RailwaySystems;
import de.bausteinchen.railwaysystems.network.ControllerPayload;
import de.bausteinchen.railwaysystems.traffic.PedestrianLightBlock;
import de.bausteinchen.railwaysystems.traffic.SignalGroup;
import de.bausteinchen.railwaysystems.traffic.SignalPlan;
import de.bausteinchen.railwaysystems.traffic.TrafficAspect;
import de.bausteinchen.railwaysystems.traffic.TrafficControllerBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Menü des Ampel-Steuergeräts mit drei Reitern:
 * <ul>
 * <li>Zeitenplan: Umlaufzeit, Gelb-/Rot-Gelb-/Zwischenzeit und je Signalgruppe das Grünfenster mit
 * Zeitbalken (Linksklick auf den Balken setzt den Grünbeginn, Rechtsklick das Grünende)</li>
 * <li>Konflikte: Matrix der feindlichen Gruppen</li>
 * <li>Ampeln: verbundene Ampeln, ihre Gruppe und Trennen</li>
 * </ul>
 * Änderungen gelten erst mit „Speichern“.
 */
public class TrafficControllerScreen extends Screen {

	private static final int WIDTH = 372;
	private static final int HEIGHT = 236;
	private static final int ROW = 18;
	private static final int VISIBLE_ROWS = 6;
	/** Abstand der ersten Gruppenzeile vom oberen Rand */
	private static final int ROWS_TOP = 74;
	/** Oberkante der Konfliktmatrix */
	private static final int CONFLICTS_TOP = 56;
	private static final String KEY = RailwaySystems.MOD_ID + ".traffic_controller.screen.";

	private static final int COLOR_RED = 0xFFC02424;
	private static final int COLOR_RED_YELLOW = 0xFFE07A1E;
	private static final int COLOR_YELLOW = 0xFFE8C020;
	private static final int COLOR_GREEN = 0xFF22B045;

	private enum Tab {
		PLAN, CONFLICTS, LIGHTS
	}

	/** Zahlenfeld zwischen zwei Knöpfen; reagiert auch auf das Mausrad */
	private record ValueSlot(int x, int y, int w, IntSupplier get, IntConsumer set, int min, int max) {
		boolean contains(double mx, double my) {
			return mx >= x && mx < x + w && my >= y && my < y + 16;
		}
	}

	private final BlockPos pos;
	private SignalPlan plan;
	private final Map<BlockPos, Integer> lights;
	private Tab tab = Tab.PLAN;
	private int scroll;
	private int left;
	private int top;
	private final List<ValueSlot> slots = new ArrayList<>();

	public TrafficControllerScreen(BlockPos pos) {
		super(Component.translatable(KEY + "title"));
		this.pos = pos;
		TrafficControllerBlockEntity be = controller();
		plan = be == null ? SignalPlan.DEFAULT : be.getPlan();
		lights = be == null ? new LinkedHashMap<>() : new LinkedHashMap<>(be.getLights());
	}

	private TrafficControllerBlockEntity controller() {
		return Minecraft.getInstance().level == null ? null
			: TrafficControllerBlockEntity.get(Minecraft.getInstance().level, pos);
	}

	private static Component tr(String key, Object... args) {
		return Component.translatable(KEY + key, args);
	}

	// --- Aufbau ---

	@Override
	protected void init() {
		left = (width - WIDTH) / 2;
		top = (height - HEIGHT) / 2;
		slots.clear();

		int tabX = left + WIDTH - 3 * 74 - 6;
		for (Tab t : Tab.values()) {
			Button button = Button.builder(tr("tab." + t.name()
				.toLowerCase()), b -> {
					tab = t;
					scroll = 0;
					rebuildWidgets();
				})
				.bounds(tabX, top + 4, 72, 16)
				.build();
			button.active = tab != t;
			addRenderableWidget(button);
			tabX += 74;
		}

		switch (tab) {
			case PLAN -> initPlan();
			case CONFLICTS -> initConflicts();
			case LIGHTS -> initLights();
		}

		addRenderableWidget(Button.builder(tr("save"), b -> save())
			.bounds(left + WIDTH - 168, top + HEIGHT - 22, 78, 18)
			.build());
		addRenderableWidget(Button.builder(tr("cancel"), b -> onClose())
			.bounds(left + WIDTH - 86, top + HEIGHT - 22, 78, 18)
			.build());
	}

	private void initPlan() {
		timeSpinner(left + 96, top + 24, plan::cycle,
			v -> plan = plan.withTimes(v, plan.yellow(), plan.redYellow(), plan.clearance()), SignalPlan.MIN_CYCLE,
			SignalPlan.MAX_CYCLE);
		timeSpinner(left + 290, top + 24, plan::yellow,
			v -> plan = plan.withTimes(plan.cycle(), v, plan.redYellow(), plan.clearance()), 1, 10);
		timeSpinner(left + 96, top + 42, plan::redYellow,
			v -> plan = plan.withTimes(plan.cycle(), plan.yellow(), v, plan.clearance()), 0, 5);
		timeSpinner(left + 290, top + 42, plan::clearance,
			v -> plan = plan.withTimes(plan.cycle(), plan.yellow(), plan.redYellow(), v), 0, 30);

		List<SignalGroup> groups = plan.groups();
		int rowY = top + ROWS_TOP;
		for (int row = 0; row < VISIBLE_ROWS && scroll + row < groups.size(); row++, rowY += ROW) {
			int index = scroll + row;
			SignalGroup group = groups.get(index);
			EditBox name = new EditBox(font, left + 8, rowY, 34, 14, tr("name"));
			name.setMaxLength(SignalGroup.MAX_NAME_LENGTH);
			name.setValue(group.name());
			name.setResponder(text -> updateGroup(index, plan.groups()
				.get(index)
				.withName(text)));
			addRenderableWidget(name);

			addRenderableWidget(Button.builder(tr(group.pedestrian() ? "pedestrian" : "vehicle"), b -> {
				updateGroup(index, plan.groups()
					.get(index)
					.withPedestrian(!plan.groups()
						.get(index)
						.pedestrian()));
				rebuildWidgets();
			})
				.bounds(left + 46, rowY - 1, 30, 16)
				.build());

			spinner(left + 80, rowY - 1, () -> plan.groups()
				.get(index)
				.greenStart(), v -> updateGroup(index, plan.groups()
					.get(index)
					.withGreen(v, plan.groups()
						.get(index)
						.greenEnd())),
				0, plan.cycle() - 1);
			spinner(left + 136, rowY - 1, () -> plan.groups()
				.get(index)
				.greenEnd(), v -> updateGroup(index, plan.groups()
					.get(index)
					.withGreen(plan.groups()
						.get(index)
						.greenStart(), v)),
				0, plan.cycle());

			addRenderableWidget(Button.builder(Component.literal("x"), b -> {
				removeGroup(index);
				rebuildWidgets();
			})
				.bounds(left + WIDTH - 22, rowY - 1, 14, 16)
				.tooltip(net.minecraft.client.gui.components.Tooltip.create(tr("delete")))
				.build());
		}

		if (groups.size() < SignalPlan.MAX_GROUPS)
			addRenderableWidget(Button.builder(tr("add_group"), b -> {
				addGroup();
				rebuildWidgets();
			})
				.bounds(left + 8, top + HEIGHT - 22, 90, 18)
				.build());
	}

	private void timeSpinner(int x, int y, IntSupplier get, IntConsumer set, int min, int max) {
		spinner(x, y, get, v -> {
			set.accept(v);
			// Umlaufzeit begrenzt die Grünzeiten: Knöpfe neu aufbauen
			rebuildWidgets();
		}, min, max);
	}

	/**
	 * Zahlenfeld: [-] Wert [+], 54 px breit. Umschalt-Klick ändert um 5.
	 */
	private void spinner(int x, int y, IntSupplier get, IntConsumer set, int min, int max) {
		addRenderableWidget(Button.builder(Component.literal("-"), b -> step(get, set, -1, min, max))
			.bounds(x, y, 14, 16)
			.build());
		addRenderableWidget(Button.builder(Component.literal("+"), b -> step(get, set, 1, min, max))
			.bounds(x + 40, y, 14, 16)
			.build());
		slots.add(new ValueSlot(x + 14, y, 26, get, set, min, max));
	}

	private static void step(IntSupplier get, IntConsumer set, int direction, int min, int max) {
		int amount = Screen.hasShiftDown() ? 5 : 1;
		set.accept(Mth.clamp(get.getAsInt() + direction * amount, min, max));
	}

	private void initConflicts() {
		List<SignalGroup> groups = plan.groups();
		int cell = 22;
		int x0 = left + 70;
		int y0 = top + CONFLICTS_TOP;
		for (int i = 0; i < groups.size(); i++)
			for (int j = 0; j < groups.size(); j++) {
				if (i == j)
					continue;
				int a = i;
				int b = j;
				boolean conflict = groups.get(i)
					.conflictsWith(j);
				Button button = Button.builder(Component.literal(conflict ? "X" : ""), btn -> {
					toggleConflict(a, b);
					rebuildWidgets();
				})
					.bounds(x0 + j * cell, y0 + i * 14, cell - 2, 13)
					.build();
				addRenderableWidget(button);
			}
	}

	private void initLights() {
		List<BlockPos> list = new ArrayList<>(lights.keySet());
		int rowY = top + 46;
		for (int row = 0; row < 8 && scroll + row < list.size(); row++, rowY += ROW) {
			BlockPos lightPos = list.get(scroll + row);
			int group = lights.get(lightPos);
			String name = plan.groups()
				.isEmpty() ? "-"
					: plan.groups()
						.get(Mth.clamp(group, 0, plan.groups()
							.size() - 1))
						.name();
			addRenderableWidget(Button.builder(tr("group", name), b -> {
				if (!plan.groups()
					.isEmpty())
					lights.put(lightPos, (lights.get(lightPos) + (Screen.hasShiftDown() ? -1 : 1) + plan.groups()
						.size()) % plan.groups()
							.size());
				rebuildWidgets();
			})
				.bounds(left + 220, rowY - 1, 70, 16)
				.build());
			addRenderableWidget(Button.builder(tr("unlink"), b -> {
				lights.remove(lightPos);
				scroll = Math.max(0, Math.min(scroll, lights.size() - 8));
				rebuildWidgets();
			})
				.bounds(left + 294, rowY - 1, 70, 16)
				.build());
		}
	}

	// --- Änderungen am Arbeitsstand ---

	private void updateGroup(int index, SignalGroup group) {
		List<SignalGroup> list = new ArrayList<>(plan.groups());
		list.set(index, group);
		plan = plan.withGroups(list);
	}

	private void addGroup() {
		List<SignalGroup> list = new ArrayList<>(plan.groups());
		long vehicles = list.stream()
			.filter(g -> !g.pedestrian())
			.count();
		list.add(new SignalGroup("K" + (vehicles + 1), false, 0, 0, 0));
		plan = plan.withGroups(list);
		scroll = Math.max(0, list.size() - VISIBLE_ROWS);
	}

	private void removeGroup(int index) {
		plan = plan.withoutGroup(index);
		lights.replaceAll((p, g) -> g == index ? 0 : g > index ? g - 1 : g);
		scroll = Math.max(0, Math.min(scroll, plan.groups()
			.size() - VISIBLE_ROWS));
	}

	private void toggleConflict(int a, int b) {
		SignalGroup ga = plan.groups()
			.get(a);
		SignalGroup gb = plan.groups()
			.get(b);
		boolean set = !ga.conflictsWith(b);
		updateGroup(a, ga.withConflictMask(set ? ga.conflictMask() | 1 << b : ga.conflictMask() & ~(1 << b)));
		updateGroup(b, gb.withConflictMask(set ? gb.conflictMask() | 1 << a : gb.conflictMask() & ~(1 << a)));
	}

	private void save() {
		PacketDistributor.sendToServer(ControllerPayload.of(pos, plan.sanitized(), lights));
		onClose();
	}

	// --- Zeichnen ---

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(graphics, mouseX, mouseY, partialTick);
		graphics.fill(left, top, left + WIDTH, top + HEIGHT, 0xE0101418);
		graphics.renderOutline(left, top, WIDTH, HEIGHT, 0xFF5A6470);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawString(font, title, left + 8, top + 8, 0xFFFFFF);
		for (ValueSlot slot : slots) {
			String text = String.valueOf(slot.get()
				.getAsInt());
			graphics.drawString(font, text, slot.x() + slot.w() / 2 - font.width(text) / 2, slot.y() + 4, 0xFFFFFF);
		}
		switch (tab) {
			case PLAN -> renderPlan(graphics, mouseX, mouseY);
			case CONFLICTS -> renderConflicts(graphics);
			case LIGHTS -> renderLights(graphics);
		}
	}

	private void renderPlan(GuiGraphics g, int mouseX, int mouseY) {
		g.drawString(font, tr("cycle"), left + 8, top + 28, 0xC8C8C8);
		g.drawString(font, tr("yellow"), left + 202, top + 28, 0xC8C8C8);
		g.drawString(font, tr("red_yellow"), left + 8, top + 46, 0xC8C8C8);
		g.drawString(font, tr("clearance"), left + 202, top + 46, 0xC8C8C8);

		int headerY = top + ROWS_TOP - 12;
		g.drawString(font, tr("col.group"), left + 8, headerY, 0x9AA4AE);
		g.drawString(font, tr("col.green_from"), left + 82, headerY, 0x9AA4AE);
		g.drawString(font, tr("col.green_to"), left + 136, headerY, 0x9AA4AE);
		g.drawString(font, tr("col.timeline"), timelineX(), headerY, 0x9AA4AE);

		List<SignalGroup> groups = plan.groups();
		int rowY = top + ROWS_TOP;
		TrafficControllerBlockEntity be = controller();
		for (int row = 0; row < VISIBLE_ROWS && scroll + row < groups.size(); row++, rowY += ROW)
			renderTimeline(g, scroll + row, rowY, be);

		if (groups.size() > VISIBLE_ROWS)
			g.drawString(font, tr("scroll", scroll + 1, Math.min(groups.size(), scroll + VISIBLE_ROWS), groups.size()),
				timelineX(), top + ROWS_TOP + VISIBLE_ROWS * ROW, 0x9AA4AE);

		renderStatus(g, be, "hint");
	}

	private int timelineX() {
		return left + 192;
	}

	private int timelineWidth() {
		return WIDTH - 192 - 28;
	}

	private void renderTimeline(GuiGraphics g, int index, int rowY, TrafficControllerBlockEntity be) {
		int x0 = timelineX();
		int w = timelineWidth();
		int cycle = plan.cycle();
		for (int t = 0; t < cycle; t++) {
			int a = x0 + t * w / cycle;
			int b = x0 + (t + 1) * w / cycle;
			g.fill(a, rowY, Math.max(a + 1, b), rowY + 12, color(plan.aspectAt(index, t)));
		}
		g.renderOutline(x0 - 1, rowY - 1, w + 2, 14, 0xFF30363C);
		if (be != null && !be.isNight()) {
			int marker = x0 + be.getTime() % cycle * w / cycle;
			g.fill(marker, rowY - 2, marker + 1, rowY + 14, 0xFFFFFFFF);
		}
	}

	private static int color(TrafficAspect aspect) {
		return switch (aspect) {
			case GREEN -> COLOR_GREEN;
			case YELLOW, YELLOW_BLINK -> COLOR_YELLOW;
			case RED_YELLOW -> COLOR_RED_YELLOW;
			case RED -> COLOR_RED;
			case OFF -> 0xFF303030;
		};
	}

	private void renderStatus(GuiGraphics g, TrafficControllerBlockEntity be, String hintKey) {
		int y = top + HEIGHT - 40;
		List<int[]> conflicts = plan.findConflicts();
		if (!conflicts.isEmpty()) {
			StringBuilder pairs = new StringBuilder();
			for (int[] pair : conflicts) {
				if (!pairs.isEmpty())
					pairs.append(", ");
				pairs.append(plan.groups()
					.get(pair[0])
					.name())
					.append("/")
					.append(plan.groups()
						.get(pair[1])
						.name());
			}
			g.drawString(font, tr("warning", pairs.toString()), left + 8, y, 0xFF6060);
		} else if (be != null && be.isNight())
			g.drawString(font, tr("night"), left + 8, y, COLOR_YELLOW);
		else
			g.drawString(font, tr(hintKey), left + 8, y, 0x9AA4AE);
	}

	private void renderConflicts(GuiGraphics g) {
		List<SignalGroup> groups = plan.groups();
		int cell = 22;
		int x0 = left + 70;
		int y0 = top + CONFLICTS_TOP;
		g.drawString(font, tr("conflicts.title"), left + 8, top + 28, 0xC8C8C8);
		for (int i = 0; i < groups.size(); i++) {
			String name = groups.get(i)
				.name();
			g.drawString(font, name, x0 - 6 - font.width(name), y0 + i * 14 + 3, 0xFFFFFF);
			String shortName = font.plainSubstrByWidth(name, cell - 2);
			g.drawString(font, shortName, x0 + i * cell + (cell - 2 - font.width(shortName)) / 2, y0 - 10, 0xFFFFFF);
			g.fill(x0 + i * cell, y0 + i * 14, x0 + i * cell + cell - 2, y0 + i * 14 + 13, 0xFF30363C);
		}
		renderStatus(g, controller(), "conflicts.hint");
	}

	private void renderLights(GuiGraphics g) {
		g.drawString(font, tr("lights.title", lights.size()), left + 8, top + 30, 0xC8C8C8);
		List<BlockPos> list = new ArrayList<>(lights.keySet());
		int rowY = top + 46;
		var level = Minecraft.getInstance().level;
		for (int row = 0; row < 8 && scroll + row < list.size(); row++, rowY += ROW) {
			BlockPos lightPos = list.get(scroll + row);
			boolean pedestrian = level != null && level.getBlockState(lightPos)
				.getBlock() instanceof PedestrianLightBlock;
			g.drawString(font, tr(pedestrian ? "lights.pedestrian" : "lights.mast"), left + 8, rowY + 3, 0xFFFFFF);
			g.drawString(font, lightPos.getX() + " " + lightPos.getY() + " " + lightPos.getZ(), left + 120, rowY + 3,
				0x9AA4AE);
		}
		if (list.isEmpty())
			g.drawString(font, tr("lights.empty"), left + 8, top + 50, 0x9AA4AE);
		else if (list.size() > 8)
			g.drawString(font, tr("scroll", scroll + 1, Math.min(list.size(), scroll + 8), list.size()), left + 8,
				top + 46 + 8 * ROW, 0x9AA4AE);
	}

	// --- Eingabe ---

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (tab == Tab.PLAN && clickTimeline(mouseX, mouseY, button))
			return true;
		return super.mouseClicked(mouseX, mouseY, button);
	}

	/**
	 * Klick in einen Zeitbalken: links setzt den Grünbeginn, rechts das Grünende auf die Sekunde unter der Maus.
	 */
	private boolean clickTimeline(double mouseX, double mouseY, int button) {
		int x0 = timelineX();
		int w = timelineWidth();
		if (mouseX < x0 || mouseX >= x0 + w || (button != 0 && button != 1))
			return false;
		int row = (int) Math.floor((mouseY - (top + ROWS_TOP)) / ROW);
		int index = scroll + row;
		if (row < 0 || row >= VISIBLE_ROWS || index >= plan.groups()
			.size() || mouseY - (top + ROWS_TOP) - row * ROW > 12)
			return false;
		int t = Mth.clamp((int) ((mouseX - x0) * plan.cycle() / w), 0, plan.cycle() - 1);
		SignalGroup group = plan.groups()
			.get(index);
		updateGroup(index, button == 0 ? group.withGreen(t, group.greenEnd()) : group.withGreen(group.greenStart(), t + 1));
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		for (ValueSlot slot : slots)
			if (slot.contains(mouseX, mouseY)) {
				step(slot.get(), slot.set(), scrollY > 0 ? 1 : -1, slot.min(), slot.max());
				return true;
			}
		int count = tab == Tab.LIGHTS ? lights.size() : plan.groups()
			.size();
		int visible = tab == Tab.LIGHTS ? 8 : VISIBLE_ROWS;
		int newScroll = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, Math.max(0, count - visible));
		if (newScroll != scroll && tab != Tab.CONFLICTS) {
			scroll = newScroll;
			rebuildWidgets();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

}
