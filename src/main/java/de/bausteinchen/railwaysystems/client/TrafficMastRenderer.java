package de.bausteinchen.railwaysystems.client;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import de.bausteinchen.railwaysystems.RailwaySystems;
import de.bausteinchen.railwaysystems.traffic.AbstractMastBlock;
import de.bausteinchen.railwaysystems.traffic.CrosswalkMastBlock;
import de.bausteinchen.railwaysystems.traffic.MastConfig;
import de.bausteinchen.railwaysystems.traffic.TrafficAspect;
import de.bausteinchen.railwaysystems.traffic.TrafficLightBlockEntity;
import de.bausteinchen.railwaysystems.traffic.TrafficMastBlock;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Zeichnet Bogen, Ausleger und Anbauteile eines Peitschenmastes: beim Ampelmast die Signalgeber, beim
 * Zebrastreifen-Mast Leuchte und Schild. Der Mastschaft selbst besteht aus Blöcken mit eigenem Modell.
 * Die Frequenzfelder der Funkverbindung zeichnet der {@link SmartBlockEntityRenderer}.
 * <p>
 * Modellraum wie bei den Signalen: Signalgeber zeigen nach Norden; aus Sicht des Fahrers (blickt nach
 * Süden) ist links +x.
 */
public class TrafficMastRenderer<T extends TrafficLightBlockEntity> extends SmartBlockEntityRenderer<T> {

	public static final ResourceLocation BEAM = RailwaySystems.asResource("block/traffic/beam");
	public static final ResourceLocation END_CAP = RailwaySystems.asResource("block/traffic/end_cap");
	public static final ResourceLocation HEAD = RailwaySystems.asResource("block/traffic/kfz_head");
	public static final ResourceLocation ZEBRA_LAMP = RailwaySystems.asResource("block/traffic/zebra_lamp");
	public static final ResourceLocation ZEBRA_LAMP_ON = RailwaySystems.asResource("block/traffic/zebra_lamp_on");
	public static final ResourceLocation ZEBRA_SIGN = RailwaySystems.asResource("block/traffic/zebra_sign");
	private static final String[] LAMPS = { "red", "yellow", "green" };

	/** Höhe des Halters über der Oberkante des Signalgebers, in Blöcken */
	private static final double HEAD_HANGER = 4 / 16.0;
	private static final int BEND_SEGMENTS = 8;

	public TrafficMastRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	/**
	 * Alle Modelle des Renderers, zum Registrieren beim Laden der Modelle.
	 */
	public static List<ResourceLocation> getModels() {
		List<ResourceLocation> models = new java.util.ArrayList<>(List.of(BEAM, END_CAP, HEAD, ZEBRA_LAMP, ZEBRA_LAMP_ON, ZEBRA_SIGN));
		for (String lamp : LAMPS) {
			models.add(lamp(lamp, false));
			models.add(lamp(lamp, true));
		}
		models.add(RailwaySystems.asResource("block/traffic/kfz_yellow_blink"));
		return models;
	}

	private static ResourceLocation lamp(String color, boolean on) {
		return RailwaySystems.asResource("block/traffic/kfz_" + color + (on ? "_on" : "_off"));
	}

	@Override
	protected void renderSafe(T be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
		super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
		BlockState state = be.getBlockState();
		if (!(state.getBlock() instanceof AbstractMastBlock) || be.getLevel() == null)
			return;
		MastConfig config = be.getConfig();
		Direction facing = state.getValue(AbstractMastBlock.FACING);
		VertexConsumer consumer = buffer.getBuffer(RenderType.cutout());

		ms.pushPose();
		ms.translate(0.5, 0, 0.5);
		ms.mulPose(Axis.YP.rotationDegrees(-yRotation(facing)));
		ms.translate(-0.5, 0, -0.5);

		double side = config.right() ? -1 : 1;
		double radius = AbstractMastBlock.BEND_RADIUS;
		double poleTop = config.height();
		double armY = poleTop + radius;

		// Bogen: Viertelkreis vom Mastende in die Waagerechte
		double prevX = 0.5;
		double prevY = poleTop - 0.1;
		for (int i = 1; i <= BEND_SEGMENTS; i++) {
			double t = i / (double) BEND_SEGMENTS * Math.PI / 2;
			double x = 0.5 + side * radius * (1 - Math.cos(t));
			double y = poleTop + radius * Math.sin(t);
			beam(be, ms, consumer, state, facing, prevX, prevY, x, y, overlay);
			prevX = x;
			prevY = y;
		}

		// Ausleger bis kurz hinter den Signalgeber
		double tipX = 0.5 + side * config.reach();
		double endX = tipX + side * 3 / 16.0;
		beam(be, ms, consumer, state, facing, prevX, prevY, endX, armY, overlay);
		ms.pushPose();
		ms.translate(endX, armY, 0.5);
		if (side < 0)
			ms.mulPose(Axis.YP.rotationDegrees(180));
		draw(ms, consumer, state, END_CAP, lightAt(be, facing, endX, armY), overlay);
		ms.popPose();

		if (state.getBlock() instanceof CrosswalkMastBlock) {
			// Leuchte hängt mit dem Halter am Ausleger, Schild sitzt vorn am Schaft
			boolean lit = state.getValue(CrosswalkMastBlock.LIT);
			ms.pushPose();
			ms.translate(tipX - 0.5, armY - 1, 0);
			draw(ms, consumer, state, ZEBRA_LAMP, lightAt(be, facing, tipX, armY - 0.5), overlay);
			if (lit)
				draw(ms, consumer, state, ZEBRA_LAMP_ON, LightTexture.FULL_BRIGHT, overlay);
			ms.popPose();
			ms.pushPose();
			ms.translate(0, config.signHeight(), 0);
			draw(ms, consumer, state, ZEBRA_SIGN, lightAt(be, facing, 0.5, config.signHeight() + 0.3), overlay);
			ms.popPose();
		} else if (state.getBlock() instanceof TrafficMastBlock) {
			TrafficAspect aspect = state.getValue(TrafficMastBlock.ASPECT);
			// Signalgeber hängt mit dem Halter am Ausleger
			head(be, ms, consumer, state, facing, aspect, tipX - 0.5, armY - 1 - HEAD_HANGER, 0, overlay);
			if (config.mastHead())
				// am Mast, etwas vor den Schaft gesetzt
				head(be, ms, consumer, state, facing, aspect, 0, 1.25, -1.5 / 16, overlay);
		}

		ms.popPose();
	}

	/**
	 * Gerades Mastrohr von (x0, y0) nach (x1, y1) in der Ebene z = Blockmitte. Das Modell ist ein Rohr von
	 * einem Block Länge entlang +x, das hier gedreht und gestreckt wird.
	 */
	private static void beam(TrafficLightBlockEntity be, PoseStack ms, VertexConsumer consumer, BlockState state,
		Direction facing, double x0, double y0, double x1, double y1, int overlay) {
		double dx = x1 - x0;
		double dy = y1 - y0;
		double length = Math.sqrt(dx * dx + dy * dy);
		if (length < 1e-4)
			return;
		// etwas länger, damit an den Knicken des Bogens keine Lücken entstehen
		double overlap = 1.5 / 16;
		ms.pushPose();
		ms.translate(x0, y0, 0.5);
		ms.mulPose(Axis.ZP.rotation((float) Math.atan2(dy, dx)));
		ms.translate(-overlap / 2, 0, 0);
		ms.scale((float) (length + overlap), 1, 1);
		draw(ms, consumer, state, BEAM, lightAt(be, facing, (x0 + x1) / 2, (y0 + y1) / 2), overlay);
		ms.popPose();
	}

	private static void head(TrafficLightBlockEntity be, PoseStack ms, VertexConsumer consumer, BlockState state,
		Direction facing, TrafficAspect aspect, double x, double y, double z, int overlay) {
		int light = lightAt(be, facing, x + 0.5, y + 0.5);
		ms.pushPose();
		ms.translate(x, y, z);
		draw(ms, consumer, state, HEAD, light, overlay);
		boolean[] lit = { aspect.isRedLit(), aspect.isYellowLit(), aspect.isGreenLit() };
		for (int i = 0; i < LAMPS.length; i++) {
			ResourceLocation model = lit[i] && aspect == TrafficAspect.YELLOW_BLINK
				? RailwaySystems.asResource("block/traffic/kfz_yellow_blink") : lamp(LAMPS[i], lit[i]);
			draw(ms, consumer, state, model, lit[i] ? LightTexture.FULL_BRIGHT : light, overlay);
		}
		ms.popPose();
	}

	/**
	 * Licht am Punkt (x, y) des Modellraums (z = Blockmitte), damit Ausleger und Signalgeber nicht das
	 * Licht des Fußblocks übernehmen.
	 */
	private static int lightAt(TrafficLightBlockEntity be, Direction facing, double x, double y) {
		double cx = x - 0.5;
		// Modell +x zeigt bei Signalgebern nach Norden nach Osten, allgemein im Uhrzeigersinn neben FACING
		BlockPos pos = be.getBlockPos()
			.relative(facing.getClockWise(), Mth.floor(cx + 0.5))
			.above(Mth.floor(y));
		return LevelRenderer.getLightColor(be.getLevel(), pos);
	}

	private static float yRotation(Direction facing) {
		return switch (facing) {
			case EAST -> 90;
			case SOUTH -> 180;
			case WEST -> 270;
			default -> 0;
		};
	}

	private static void draw(PoseStack ms, VertexConsumer consumer, BlockState state, ResourceLocation location,
		int light, int overlay) {
		SignalOverlayRenderer.draw(ms, consumer, state, location, light, overlay);
	}

	@Override
	public int getViewDistance() {
		return 128;
	}

}
