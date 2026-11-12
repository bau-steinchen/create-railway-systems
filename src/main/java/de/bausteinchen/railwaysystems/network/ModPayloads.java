package de.bausteinchen.railwaysystems.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModPayloads {

	public static void register(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar("1");
		registrar.playToServer(MastConfigPayload.TYPE, MastConfigPayload.STREAM_CODEC, MastConfigPayload::handle);
		registrar.playToServer(ControllerPayload.TYPE, ControllerPayload.STREAM_CODEC, ControllerPayload::handle);
	}

}
