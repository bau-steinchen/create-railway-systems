"""
Werkbank-Rezepte aller Blöcke und Items der Mod.

Zum Anpassen hier die Zutaten ändern und danach `python3 tools/generate_assets.py` ausführen; der
Generator schreibt daraus die Rezepte nach src/main/resources/data/create_railway_systems/recipe/ und die
Rezeptbuch-Freischaltungen nach .../advancement/recipes/. Ein Rezept hier zu löschen entfernt auch seine
Dateien, der Generator verlangt aber für jedes Item der Mod genau ein Rezept.

Format
------
shaped(ergebnis, muster, **zutaten)
    muster: bis zu 3 Zeilen mit bis zu 3 Zeichen, Leerzeichen = leeres Feld
    zutaten: ein Buchstabe je Zutat, z. B. T=TUBE

shapeless(ergebnis, [zutat, zutat, ...])
    formlos, jede Zutat einmal (doppelte Zutat = zweimal eintragen)

Optionale Angaben für beide:
    count=4          Anzahl, die herauskommt (Standard 1)
    unlock="..."     Zutat, deren Besitz das Rezept im Rezeptbuch freischaltet
                     (Standard: erste Create-Zutat, sonst erste Zutat)

Zutaten
-------
    "create:electron_tube"   Item mit Namespace (Create, Minecraft, ...)
    "#c:dyes/red"            Tag: jedes Item mit diesem Tag passt (# davor)
    "signal_mast"            Item dieser Mod (ohne Namespace)
"""

# --- häufige Zutaten ---
TUBE = "create:electron_tube"
TRACK_SIGNAL = "create:track_signal"
TRACK_OBSERVER = "create:track_observer"
LINK = "create:redstone_link"
IRON_SHEET = "create:iron_sheet"
BRASS_SHEET = "create:brass_sheet"
ANDESITE_ALLOY = "create:andesite_alloy"
COGWHEEL = "create:cogwheel"
RAILWAY_CASING = "create:railway_casing"
BRASS_CASING = "create:brass_casing"
ANDESITE_CASING = "create:andesite_casing"
IRON_BARS = "minecraft:iron_bars"
STICK = "#c:rods/wooden"
REDSTONE = "#c:dusts/redstone"
COMPARATOR = "minecraft:comparator"
RED = "#c:dyes/red"
YELLOW = "#c:dyes/yellow"
GREEN = "#c:dyes/green"
WHITE = "#c:dyes/white"
BLUE = "#c:dyes/blue"


def shaped(result, pattern, count=1, unlock=None, **key):
    return {"kind": "shaped", "result": result, "pattern": pattern, "key": key, "count": count, "unlock": unlock}


def shapeless(result, ingredients, count=1, unlock=None):
    return {"kind": "shapeless", "result": result, "ingredients": ingredients, "count": count, "unlock": unlock}


RECIPES = [
    # --- Masten ---
    shaped("signal_mast", ["I", "I", "I"], count=4, I=IRON_SHEET),
    shaped("signal_mast_light", ["N", "I", "N"], count=4, N=IRON_BARS, I=IRON_SHEET),
    shaped("signal_mast_lattice", ["N", "N", "N"], count=4, N=IRON_BARS),

    # --- H/V und Sh (Eisenblech) ---
    shaped("hv_hauptsignal", ["RTG", "ISI", " M "],
           R=RED, T=TUBE, G=GREEN, I=IRON_SHEET, S=TRACK_SIGNAL, M="signal_mast"),
    shaped("hv_vorsignal", ["YTG", "ISI", " M "],
           Y=YELLOW, T=TUBE, G=GREEN, I=IRON_SHEET, S=TRACK_SIGNAL, M="signal_mast"),
    shapeless("hv_kombisignal", ["hv_hauptsignal", "hv_vorsignal"], unlock="hv_hauptsignal"),
    shaped("sh_sperrsignal", ["WTR", "ISI"],
           W=WHITE, T=TUBE, R=RED, I=IRON_SHEET, S=TRACK_SIGNAL),

    # --- Ks (Messingblech) ---
    shaped("ks_hauptsignal", ["RTG", "BSB", " M "],
           R=RED, T=TUBE, G=GREEN, B=BRASS_SHEET, S=TRACK_SIGNAL, M="signal_mast"),
    shaped("ks_vorsignal", ["YTG", "BSB", " M "],
           Y=YELLOW, T=TUBE, G=GREEN, B=BRASS_SHEET, S=TRACK_SIGNAL, M="signal_mast"),
    shapeless("ks_mehrabschnittssignal", ["ks_hauptsignal", "ks_vorsignal"], unlock="ks_hauptsignal"),

    # --- Formsignale ---
    shaped("form_hauptsignal_1", ["RIW", " S ", "AMA"],
           R=RED, I=IRON_SHEET, W=WHITE, S=TRACK_SIGNAL, A=ANDESITE_ALLOY, M="signal_mast"),
    shapeless("form_hauptsignal_2", ["form_hauptsignal_1", IRON_SHEET, RED, WHITE], unlock="form_hauptsignal_1"),

    # --- Straßenampeln ---
    shaped("ampel_mast", ["RTI", "YTI", "GTI"],
           R=RED, Y=YELLOW, G=GREEN, T=TUBE, I=IRON_SHEET),
    shaped("fussgaengerampel", ["RTI", "GTI"],
           R=RED, G=GREEN, T=TUBE, I=IRON_SHEET),
    shaped("zebrastreifen_mast", ["CTC", "ILI", " I "],
           C=BLUE, T=TUBE, I=IRON_SHEET, L=LINK),
    shaped("ampel_steuergeraet", ["RTR", "KXK"],
           R=REDSTONE, T=TUBE, K=COMPARATOR, X=BRASS_CASING),
    shaped("ampel_programmierer", ["T", "B", "R"],
           T=TUBE, B=BRASS_SHEET, R=REDSTONE),

    # --- Bahnübergänge ---
    shaped("andreaskreuz", ["R W", " I ", "WKR"], count=2,
           R=RED, W=WHITE, I=IRON_SHEET, K=STICK),
    shaped("bue_lichtzeichen", ["ITY", "ITR", "ILI"],
           I=IRON_SHEET, T=TUBE, Y=YELLOW, R=RED, L=LINK),
    shaped("bue_blinklicht", [" T ", "IRI", " L "],
           T=TUBE, I=IRON_SHEET, R=RED, L=LINK),
    shaped("bue_schranke", ["RKW", "CXL"],
           R=RED, K=STICK, W=WHITE, C=COGWHEEL, X=ANDESITE_CASING, L=LINK),
    shaped("bue_steuerung", ["O", "X", "L"],
           O=TRACK_OBSERVER, X=RAILWAY_CASING, L=LINK),
]
