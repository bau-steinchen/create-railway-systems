package de.bausteinchen.railwaysystems.signal;

import java.util.Locale;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;

import de.bausteinchen.railwaysystems.RailwaySystems;

/**
 * Einstellungen eines Hauptsignals, die über Create-Wertefelder gewählt werden.
 */
public final class SignalOptions {

	private SignalOptions() {}

	private static String key(String group, Enum<?> value) {
		return RailwaySystems.MOD_ID + ".signal.option." + group + "." + value.name()
			.toLowerCase(Locale.ROOT);
	}

	/** Ausstattung mit Ersatzsignal (Zs1), Vorsichtsignal (Zs7) oder Gegengleis-Ersatzsignal (Zs8) */
	public enum Zusatz implements INamedIconOptions {
		NONE(AllIcons.I_NONE),
		ZS1(AllIcons.I_CONFIRM),
		ZS7(AllIcons.I_PRIORITY_LOW),
		ZS8(AllIcons.I_TUNNEL_SPLIT);

		private final AllIcons icon;

		Zusatz(AllIcons icon) {
			this.icon = icon;
		}

		@Override
		public AllIcons getIcon() {
			return icon;
		}

		@Override
		public String getTranslationKey() {
			return key("zusatz", this);
		}
	}

	/** Gegengleisanzeiger Zs6 */
	public enum Gegengleis implements INamedIconOptions {
		NONE(AllIcons.I_NONE),
		ZS6(AllIcons.I_FLIP);

		private final AllIcons icon;

		Gegengleis(AllIcons icon) {
			this.icon = icon;
		}

		@Override
		public AllIcons getIcon() {
			return icon;
		}

		@Override
		public String getTranslationKey() {
			return key("gegengleis", this);
		}
	}

	/** Betriebszustand: abgeschaltete Signale zeigen nur das Kennlicht */
	public enum Betrieb implements INamedIconOptions {
		ACTIVE(AllIcons.I_PLAY),
		OFF(AllIcons.I_STOP);

		private final AllIcons icon;

		Betrieb(AllIcons icon) {
			this.icon = icon;
		}

		@Override
		public AllIcons getIcon() {
			return icon;
		}

		@Override
		public String getTranslationKey() {
			return key("betrieb", this);
		}
	}

}
