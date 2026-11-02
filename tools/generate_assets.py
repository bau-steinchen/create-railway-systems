#!/usr/bin/env python3
"""Erzeugt Blockstates, Modelle, Platzhalter-Texturen, Loot-Tables und Tags der Signale.

Aufruf aus dem Projektverzeichnis:  python tools/generate_assets.py
                                    python tools/generate_assets.py --vorlagen
Benötigt Pillow (pip install pillow) für die Texturen.

Eigene Signalköpfe aus Blockbench (Java Block/Item Model) liegen als tools/models/<block>.json
und ersetzen die eingebaute Geometrie; Lampen, Mast/Ausleger und Overlay-Anzeigen ergänzt das Skript.
--vorlagen schreibt die eingebauten Modelle als Startpunkt nach tools/models/vorlagen/.
Namensregeln siehe README, Abschnitt "Eigene Modelle mit Blockbench".
"""
import copy
import json
import os
import re
import shutil
import sys
from pathlib import Path

from PIL import Image, ImageDraw

MOD_ID = "create_railway_systems"
TOOLS = Path(__file__).resolve().parent
ROOT = TOOLS.parent / "src" / "main" / "resources"
ASSETS = ROOT / "assets" / MOD_ID
DATA = ROOT / "data"
BB_DIR = TOOLS / "models"
BB_TEXTURES = BB_DIR / "textures"
BB_TEMPLATES = BB_DIR / "vorlagen"

FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}
HP = ["hp0", "hp1", "hp2", "off"]
MOUNTS = ["none", "back", "left", "right"]
HALVES = ["lower", "upper"]
TALL_BLOCKS = {"hv_hauptsignal", "hv_kombisignal", "ks_hauptsignal", "ks_mehrabschnittssignal"}
VR = ["vr0", "vr1", "vr2"]

# Welche Lampen leuchten bei welchem Begriff
HP_LIT = {"hp0": {"red"}, "hp1": {"green"}, "hp2": {"green", "yellow"}, "off": set()}
VR_LIT = {"vr0": {"y_bl", "y_tr"}, "vr1": {"g_bl", "g_tr"}, "vr2": {"g_bl", "y_tr"}, "dark": set()}
SH_LIT = {"sh0": {"r_l", "r_r"}, "sh1": {"w_bl", "w_tr"}}
# Ks: Lampe -> Textur (blinkendes Grün als animierte Textur)
KS_LIT = {"hp0": {"red": "red"}, "ks1": {"green": "green"}, "ks1_blink": {"green": "green_blink"},
          "ks2": {"yellow": "yellow"}, "off": {}}
SPEEDS = range(1, 10)
LETTERS = range(1, 27)  # Zs2/Zs2v: 1–26 = A–Z wie im SignalLetterBehaviour
LAMP_COLOR = {"red": "red", "green": "green", "yellow": "yellow",
              "r_l": "red", "r_r": "red", "w_bl": "white", "w_tr": "white",
              "y_bl": "yellow", "y_tr": "yellow", "g_bl": "green", "g_tr": "green"}
# Blockname -> BlockbenchModel aus tools/models; leer = eingebaute Geometrie
BLOCKBENCH = {}
WRITTEN = set()


def write_json(path: Path, data):
    WRITTEN.add(path.resolve())
    path.parent.mkdir(parents=True, exist_ok=True)
    # newline: unter Windows sonst CRLF, und jeder Lauf ändert alle Dateien
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def tex(name):
    return f"{MOD_ID}:block/{name}"


def box(frm, to, texture, north=None, name=None):
    """name erscheint nur in den Blockbench-Vorlagen; model() entfernt ihn."""
    faces = {f: {"texture": texture} for f in ("north", "east", "south", "west", "up", "down")}
    if north:
        # Eigene Frontfläche (Lampe, Ziffer, Schild) zeigt immer die ganze Textur. Ohne "uv"
        # schneidet Minecraft den Ausschnitt passend zur Position im Block aus.
        faces["north"] = {"texture": north, "uv": [0, 0, 16, 16]}
    element = {"from": frm, "to": to, "faces": faces}
    if name:
        element["name"] = name
    return element


def glow(element):
    """Fläche leuchtet unabhängig vom Umgebungslicht."""
    element["shade"] = False
    element["neoforge_data"] = {"block_light": 15, "sky_light": 15}
    return element


def mastschild(vx0, vx1, y0, y1, texture="#mastschild"):
    """Schild vor dem Mast; nur die Front trägt das Schildbild, Kanten in Mastfarbe."""
    x0, x1 = mirror_x(vx0, vx1)
    return box([x0, y0, 5.5], [x1, y1, 6.5], "#mast", north=texture, name="mastschild")


def mirror_x(x0, x1):
    """Betrachterkoordinate (x von links, vor dem Schirm stehend) -> Modellraum.
    Der Schirm zeigt nach Norden; der Betrachter blickt nach Süden, sein "links" ist +x."""
    return 16 - x1, 16 - x0


def lamp(vx0, y0, size, color, lit, name=None):
    x0, _ = mirror_x(vx0, vx0 + size)
    element = box([x0, y0, 3.5], [x0 + size, y0 + size, 4], "#screen",
                  north=f"#lamp_{color}" if lit else "#lamp_off", name=name)
    return glow(element) if lit else element


def post(height=16):
    # deckungsgleich mit dem Signalmast-Block, damit kein Versatz am Übergang entsteht
    return box([6.5, 0, 6.5], [9.5, height, 9.5], "#mast", name="mast")


def support(mount, arm_heights, height=16, name="arm"):
    """Senkrechter Mast (frei stehend) oder waagerechte Ausleger zum Nachbarblock (Wandmontage).
    Modellraum: Schirm zeigt nach Norden; "links"/"rechts" aus Sicht des Betrachters vor dem Schirm."""
    if mount == "none":
        return [post(height)]
    arms = {"back": ([7, 6.5], [9, 16]), "left": ([7, 6.5], [16, 8.5]), "right": ([0, 6.5], [9, 8.5])}
    (x0, z0), (x1, z1) = arms[mount]
    return [box([x0, y0, z0], [x1, y1, z1], "#mast", name=name) for y0, y1 in arm_heights]


TEXTURES = {
    "particle": tex("signal_screen"),
    "screen": tex("signal_screen"),
    "mast": tex("signal_mast"),
    "mastschild": tex("mastschild_hp"),
    "lamp_off": tex("lamp_off"),
    "lamp_red": tex("lamp_red"),
    "lamp_green": tex("lamp_green"),
    "lamp_yellow": tex("lamp_yellow"),
    "lamp_white": tex("lamp_white"),
    "lamp_green_blink": tex("lamp_green_blink"),
    "lamp_white_blink": tex("lamp_white_blink"),
    "zs6_on": tex("zs6_on"),
    "zs8_on": tex("zs8_on"),
    "zs_box": tex("zs_box"),
    "ne2": tex("ne2"),
    "mastschild_ms": tex("mastschild_ms"),
    "concrete": tex("concrete"),
    "number_plate": tex("number_plate"),
}


def auto_uv(face, frm, to):
    """UV wie Minecraft sie ohne Angabe aus der Position berechnet."""
    (x0, y0, z0), (x1, y1, z1) = frm, to
    return {"down": [x0, 16 - z1, x1, 16 - z0], "up": [x0, z0, x1, z1],
            "north": [16 - x1, 16 - y1, 16 - x0, 16 - y0], "south": [x0, 16 - y1, x1, 16 - y0],
            "west": [z0, 16 - y1, z1, 16 - y0], "east": [16 - z1, 16 - y1, 16 - z0, 16 - y0]}[face]


def fold(a, b):
    """Bereich in 0..16 verschieben; sonst kleben bei Teilen über 16 px fremde Atlas-Texturen an."""
    while min(a, b) < 0:
        a, b = a + 16, b + 16
    while max(a, b) > 16:
        a, b = a - 16, b - 16
    return max(0, min(16, a)), max(0, min(16, b))


def with_uvs(elements):
    """Jeder Fläche ohne UV eine explizite, gültige UV geben (wichtig für 32 px hohe Modelle)."""
    elements = copy.deepcopy(elements)
    for e in elements:
        for name, face in e["faces"].items():
            if "uv" not in face:
                u0, v0, u1, v1 = auto_uv(name, e["from"], e["to"])
                u0, u1 = fold(u0, u1)
                v0, v1 = fold(v0, v1)
                face["uv"] = [u0, v0, u1, v1]
    return elements


def model(elements, bb=None, **extra_textures):
    """bb: Blockbench-Modell, dessen Elemente enthalten sind (bringt eigene Texturvariablen mit)."""
    result = {"parent": "block/block",
              "textures": {**TEXTURES, **(bb.textures if bb else {}), **extra_textures},
              "elements": [{k: v for k, v in e.items() if k != "name"} for e in with_uvs(elements)]}
    if bb:
        # eigene Texturen dürfen durchsichtige Pixel haben
        result["render_type"] = "minecraft:cutout"
    return result


# --- Zwei Blöcke hohe Signale: Entwurf in 0..32 px, Aufteilung in untere/obere Hälfte ---

TALL_ITEM_DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, -3.2, 0], "scale": [0.4, 0.4, 0.4]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 1, 0], "scale": [0.2, 0.2, 0.2]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, -2.5, 0], "scale": [0.3, 0.3, 0.3]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 0, 0], "scale": [0.25, 0.25, 0.25]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, -2, 0], "scale": [0.3, 0.3, 0.3]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, -2, 0], "scale": [0.3, 0.3, 0.3]},
}


SIDES = ("north", "east", "south", "west")


def clippable(element):
    """Schneiden geht nur ohne Elementdrehung und mit Seitenflächen, deren v senkrecht läuft."""
    return "rotation" not in element and all(
        element["faces"][f].get("rotation", 0) in (0, 180) for f in SIDES if f in element["faces"])


def shift_y(element, dy):
    piece = copy.deepcopy(element)
    piece["from"][1] -= dy
    piece["to"][1] -= dy
    if "rotation" in piece:
        piece["rotation"]["origin"][1] -= dy
    return piece


def clip_y(element, lo, hi):
    """Teil eines Elements zwischen lo und hi (px), um lo nach unten verschoben, oder None."""
    y0, y1 = element["from"][1], element["to"][1]
    if not clippable(element):
        # gedreht: ganz in die Hälfte, in der die Mitte liegt (Modelle dürfen bis -16/32 reichen)
        return shift_y(element, lo) if lo <= (y0 + y1) / 2 < hi else None
    a, b = max(y0, lo), min(y1, hi)
    if b <= a:
        return None
    piece = shift_y(element, 0)
    piece["from"][1], piece["to"][1] = a - lo, b - lo
    if (a, b) != (y0, y1):
        for side in SIDES:
            face = piece["faces"].get(side)
            if face and "uv" in face:
                # explizite UV anteilig mitschneiden (v läuft von oben nach unten, bei 180° umgekehrt)
                u0, v0, u1, v1 = face["uv"]
                top, bottom = (y1 - b) / (y1 - y0), (y1 - a) / (y1 - y0)
                if face.get("rotation", 0) == 180:
                    top, bottom = 1 - bottom, 1 - top
                face["uv"] = [u0, v0 + top * (v1 - v0), u1, v0 + bottom * (v1 - v0)]
    return piece


def split_tall(elements):
    """{"lower": [...], "upper": [...]} aus einem 32 px hohen Entwurf."""
    return {half: [p for p in (clip_y(e, lo, lo + 16) for e in elements) if p]
            for half, lo in (("lower", 0), ("upper", 16))}


def hood(vx0, y0, size):
    """Schute über einer Lampe."""
    x0, x1 = mirror_x(vx0 - 0.25, vx0 + size + 0.25)
    return box([x0, y0 + size, 2.5], [x1, y0 + size + 0.5, 4], "#screen", name="schute")


def main_screen(lit, vx0, vx1, y0, y1, lamps):
    """Schirm mit Lampen; alle x-Werte in Betrachterkoordinaten (siehe mirror_x)."""
    x0, x1 = mirror_x(vx0, vx1)
    elements = [box([x0, y0, 4], [x1, y1, 6.5], "#screen", name="schirm")]
    for name, (lx, ly, size) in lamps.items():
        elements.append(lamp(lx, ly, size, lamp_color(lit, name), name in lit, name=f"lamp_{name}"))
    return elements


def lamp_color(lit, name):
    """lit: Menge leuchtender Lampen oder Lampe -> Textur (Ks)."""
    return lit[name] if isinstance(lit, dict) and name in lit else LAMP_COLOR[name]


HV_HAUPT = {
    "lamps": {"green": (6.375, 25.5, 3.25), "red": (6.375, 21.25, 3.25), "yellow": (6.375, 12.5, 3.25)},
    "screen": (4.5, 11.5, 12, 30.5),
    "arms": [(26, 28), (3, 5)],
}
HV_KOMBI = {
    "lamps": {"green": (6.5, 27.5, 3), "red": (6.5, 24, 3), "yellow": (6.5, 17.25, 3)},
    "vr_lamps": {"y_bl": (3.5, 3.5, 2.5), "g_bl": (6.5, 3.5, 2.5), "g_tr": (7, 7.25, 2.5), "y_tr": (10, 7.25, 2.5)},
    "screen": (4.75, 11.25, 17, 31.5),
    "arms": [(27, 29), (4, 6)],
}
HV_VOR = {
    "lamps": {"y_bl": (3.5, 5, 2.5), "g_bl": (6.5, 5, 2.5), "g_tr": (7, 8.5, 2.5), "y_tr": (10, 8.5, 2.5)},
    "arms": [(7, 9)],
}
SH_LAMPS = {"r_l": (2.75, 7.75, 2.5), "w_tr": (8, 7.75, 2.5), "r_r": (10.75, 7.75, 2.5), "w_bl": (2.75, 4, 2.5)}


def hauptsignal_elements(lit, mount):
    """H/V-Hauptsignal, zwei Blöcke hoch. Kennlicht, Zs1/Zs7 und Zs6 kommen als Overlay dazu."""
    spec = HV_HAUPT
    bb = BLOCKBENCH.get("hv_hauptsignal")
    if bb:
        return bb.assemble(lit, mount, spec["arms"], 32)
    elements = support(mount, spec["arms"], height=32)
    elements.append(mastschild(6.5, 9.5, 1, 6))
    elements += main_screen(lit, *spec["screen"], spec["lamps"])
    elements += [hood(vx, ly, size) for vx, ly, size in spec["lamps"].values()]
    return elements


def vorsignal_elements(lit, mount):
    bb = BLOCKBENCH.get("hv_vorsignal")
    if bb:
        return bb.assemble(lit, mount, HV_VOR["arms"], 16)
    return support(mount, HV_VOR["arms"]) + main_screen(lit, 3, 13, 4, 12, HV_VOR["lamps"])


def kombisignal_elements(lit, mount):
    """H/V-Hauptsignal mit Vorsignal am Mast, zwei Blöcke hoch: Hauptschirm, Mastschild, Vorsignal."""
    spec = HV_KOMBI
    bb = BLOCKBENCH.get("hv_kombisignal")
    if bb:
        return bb.assemble(lit, mount, spec["arms"], 32)
    elements = support(mount, spec["arms"], height=32)
    elements += main_screen(lit, *spec["screen"], spec["lamps"])
    elements += [hood(vx, ly, size) for vx, ly, size in spec["lamps"].values()]
    elements.append(mastschild(6.5, 9.5, 10.75, 16.5))
    elements += main_screen(lit, 3, 13, 2.5, 10.5, spec["vr_lamps"])
    return elements


def sperrsignal_foot(on_mast, top=2.5):
    if on_mast:
        # Maststummel deckungsgleich mit dem Signalmast
        return [box([6.5, 0, 6.5], [9.5, top, 9.5], "#mast")]
    # Betonsockel mit kurzem Halter
    return [box([5, 0, 5], [11, 2, 11], "#concrete", name="sockel"),
            box([7, 2, 7], [9, 2.5, 9], "#mast", name="sockel")]


def sperrsignal_head(lit):
    elements = [box([4.5, 2.5, 4], [11.5, 3, 9.5], "#mast", name="halteplatte")]  # unter dem Schirm
    elements += main_screen(lit, 2, 14, 3, 10.5, SH_LAMPS)
    x0, x1 = mirror_x(2.5, 13.5)
    elements.append(box([x0, 10.5, 4], [x1, 11, 6.5], "#screen", name="oberkante"))  # abgerundet
    for vx, ly, size in SH_LAMPS.values():
        elements.append(hood(vx, ly, size))

    x0, x1 = mirror_x(8, 11.5)
    elements.append(box([x0, 3.75, 3.5], [x1, 6.75, 4], "#screen", north="#number_plate", name="nummernschild"))
    x0, x1 = mirror_x(13.5, 15)
    elements.append(box([x0, 1.5, 3.5], [x1, 7.5, 4], "#mast", north="#mastschild", name="mastschild"))
    return elements


def sperrsignal_model(lit, on_mast):
    """Gleissperrsignal nach DB-Vorbild: breiter Schirm, oben Rot – Lücke – Weiß/Rot, unten links Weiß,
    rechts Nummernschild und weiß-rot-weißes Mastschild."""
    bb = BLOCKBENCH.get("sh_sperrsignal")
    if not bb:
        return model(sperrsignal_foot(on_mast) + sperrsignal_head(lit))
    if not bb.sockel:
        foot = sperrsignal_foot(on_mast)
    elif on_mast:
        # Stummel reicht bis zur Oberkante des eigenen Sockels
        foot = sperrsignal_foot(True, top=max(e["to"][1] for e in bb.sockel))
    else:
        foot = copy.deepcopy(bb.sockel)
    return model(foot + bb.body + bb.lamps_for(lit), bb)


# --- Ks-Signale (Multipart: Träger, Schirm und Zs3/Zs3v-Anzeiger als getrennte Teile) ---

KS_TYPES = {
    "ks_hauptsignal": {
        "tall": True,
        "arms": [(24, 26), (4, 6)],
        "screen": (4.5, 11.5, 16, 27.5),
        "lamps": {"red": (6.5, 22.5, 3), "green": (6.5, 18.75, 3)},
        "aspects": ["hp0", "ks1", "off"],
        "extras": lambda: [mastschild(6.5, 9.5, 3, 9.5)],
    },
    "ks_mehrabschnittssignal": {
        "tall": True,
        "arms": [(24, 26), (3, 5)],
        "screen": (3.5, 12.5, 17.5, 27.5),
        "lamps": {"red": (6.5, 23.5, 3), "green": (4.25, 19.5, 3), "yellow": (8.75, 19.5, 3)},
        "aspects": ["hp0", "ks1", "ks1_blink", "ks2", "off"],
        # Mastschild mit gelbem Dreieck: Hauptsignal mit Vorsignalfunktion
        "extras": lambda: [mastschild(6.5, 9.5, 0.5, 8, "#mastschild_ms")],
    },
    "ks_vorsignal": {
        "arms": [(11, 13), (6, 8)],
        "screen": (3.5, 12.5, 9.5, 14),
        "lamps": {"green": (4.75, 10.5, 2.5), "yellow": (8.75, 10.5, 2.5)},
        "aspects": ["ks1", "ks1_blink", "ks2"],
        "extras": lambda: [mastschild(6.25, 9.75, 0.5, 5, "#ne2")],
    },
}

# Zusatzanzeigen je Signal (Betrachterkoordinaten, 0..32 px bei hohen Signalen), gezeichnet vom
# SignalOverlayRenderer. Lampen: (x, y, Größe); Kästen: (x0, x1, y0, y1). Der Zs8 nutzt den Kasten
# des Zs6. "zs2v_inset": Zs2v sitzt in einer freien Ecke des Vorsignalschirms und braucht keinen Kasten.
OVERLAYS = {
    "hv_hauptsignal": {
        "kennlicht": (4.9, 29, 1.25),
        "zs1": [(7.25, 19.25, 1.5), (5.25, 16.75, 1.5), (9.25, 16.75, 1.5)], "zs1_blink": False,
        "zs7": [(5.25, 19.25, 1.5), (9.25, 19.25, 1.5), (7.25, 16.75, 1.5)],
        "zs2": (3, 7.75, 6.5, 11),
        "zs6": (8.25, 13, 6.5, 11),
    },
    "hv_kombisignal": {
        "kennlicht": (5, 30.25, 1),
        "zs1": [(7.25, 22.25, 1.5), (5.5, 20.5, 1.5), (9, 20.5, 1.5)], "zs1_blink": False,
        "zs7": [(5.5, 22.25, 1.5), (9, 22.25, 1.5), (7.25, 20.5, 1.5)],
        "zs2": (3.25, 6.25, 10.75, 16.5),
        "zs6": (9.75, 12.75, 10.75, 16.5),
        "zs2v": (3.5, 6.5, 7, 10.25), "zs2v_inset": True,
    },
    "hv_vorsignal": {
        "zs2v": (3.5, 6.5, 8.25, 11.75), "zs2v_inset": True,
    },
    "ks_hauptsignal": {
        "kennlicht": (5, 26.25, 1.25),
        "zs1": [(7.25, 16.5, 1.5)], "zs1_blink": True,
        "zs7": [(4.75, 19.75, 1.5), (9.75, 19.75, 1.5), (7.25, 16.5, 1.5)],
        "zs2": (3, 7.75, 10.5, 15.5),
        "zs6": (8.25, 13, 10.5, 15.5),
        "zs3": (5.5, 10.5, 28, 32),
    },
    "ks_mehrabschnittssignal": {
        "kennlicht": (4.25, 26, 1.25),
        "zs1": [(7.25, 17.75, 1.5)], "zs1_blink": True,
        "zs7": [(4.5, 23.75, 1.5), (10, 23.75, 1.5), (7.25, 17.75, 1.5)],
        "zs2": (3, 7.75, 12.75, 17),
        "zs6": (8.25, 13, 12.75, 17),
        "zs3": (5.5, 10.5, 28, 32),
        "zs3v": (3, 7.75, 8.25, 12.5),
        "zs2v": (8.25, 13, 8.25, 12.5),
    },
    "ks_vorsignal": {
        "zs3v": (3.5, 7.5, 5.5, 9),
        "zs2v": (8.5, 12.5, 5.5, 9),
    },
}
LAMP_COLOR.update({"green": "green", "yellow": "yellow", "red": "red"})


def ks_screen(spec, lit):
    vx0, vx1, y0, y1 = spec["screen"]
    elements = main_screen(lit, vx0, vx1, y0, y1, spec["lamps"])
    elements += [hood(vx, ly, size) for vx, ly, size in spec["lamps"].values()]
    return elements


def zs_box(vx0, vx1, y0, y1, texture="#zs_box", name=None):
    x0, x1 = mirror_x(vx0, vx1)
    return [box([x0, y0, 4], [x1, y1, 6.5], "#zs_box", north=texture, name=name)]


def zs_digit(vx0, vx1, y0, y1, texture="#digit"):
    """Leuchtende Anzeige vor dem Kasten (Ziffer über #digit oder eigene Textur)."""
    x0, x1 = mirror_x(vx0 + 0.5, vx1 - 0.5)
    return [glow(box([x0, y0 + 0.5, 3.5], [x1, y1 - 0.5, 4], "#zs_box", north=texture))]


# Overlay-Positionen sind Tupel aus OVERLAYS oder Marker-Elemente aus einem Blockbench-Modell
# (dann gelten from/to des Markers direkt, im Modellraum).

def overlay_places(name):
    """OVERLAYS-Eintrag des Signals; Blockbench-Marker ersetzen die eingebauten Positionen."""
    spec = dict(OVERLAYS.get(name, {}))
    bb = BLOCKBENCH.get(name)
    if bb:
        for key, markers in bb.overlay.items():
            spec[key] = markers if key in ("zs1", "zs7") else markers[0]
    return spec


def overlay_box(place, texture="#zs_box"):
    if isinstance(place, dict):
        return [box(place["from"], place["to"], "#zs_box", north=texture)]
    return zs_box(*place, texture=texture)


def overlay_digit(place, texture="#digit"):
    if isinstance(place, dict):
        frm, to = place["from"], place["to"]
        return [glow(box([frm[0] + 0.5, frm[1] + 0.5, frm[2] - 0.5], [to[0] - 0.5, to[1] - 0.5, frm[2]],
                         "#zs_box", north=texture))]
    return zs_digit(*place, texture=texture)


def overlay_lamp(place, color, lit):
    if isinstance(place, dict):
        element = box(place["from"], place["to"], "#screen", north=f"#lamp_{color}" if lit else "#lamp_off")
        return glow(element) if lit else element
    return lamp(*place, color, lit)


def generate_overlays():
    """Overlay-Modelle für den SignalOverlayRenderer: block/overlay/<signal>/<teil>."""
    shutil.rmtree(ASSETS / "models/block/overlay", ignore_errors=True)
    for name in OVERLAYS:
        spec = overlay_places(name)

        def out(part, elements, **extra):
            write_json(ASSETS / "models/block/overlay" / name / f"{part}.json", model(elements, **extra))

        for key in ("zs3", "zs3v"):
            if key in spec:
                out(f"{key}_box", overlay_box(spec[key]))
                for n in SPEEDS:
                    out(f"{key}_{n}", overlay_digit(spec[key]), digit=tex(f"{key}_{n}"))
        for key in ("zs2", "zs2v"):
            if key in spec:
                out(f"{key}_box", [] if spec.get(f"{key}_inset") else overlay_box(spec[key]))
                for n in LETTERS:
                    out(f"{key}_{n}", overlay_digit(spec[key]), digit=tex(f"{key}_{n}"))
        if "kennlicht" in spec:
            out("kennlicht_off", [overlay_lamp(spec["kennlicht"], "white", False)])
            out("kennlicht_on", [overlay_lamp(spec["kennlicht"], "white", True)])
        if "zs1" in spec:
            color = "white_blink" if spec["zs1_blink"] else "white"
            out("zs1_off", [overlay_lamp(place, "white", False) for place in spec["zs1"]])
            out("zs1_on", [overlay_lamp(place, color, True) for place in spec["zs1"]])
            out("zs7_off", [overlay_lamp(place, "yellow", False) for place in spec["zs7"]])
            out("zs7_on", [overlay_lamp(place, "yellow", True) for place in spec["zs7"]])
        if "zs6" in spec:
            out("zs6_box", overlay_box(spec["zs6"]))
            out("zs6_on", overlay_digit(spec["zs6"], texture="#zs6_on"))
            out("zs8_on", overlay_digit(spec["zs6"], texture="#zs8_on"))


def multipart(parts):
    entries = []
    for facing, rot in FACINGS.items():
        for when, model_name_ in parts:
            apply = {"model": f"{MOD_ID}:block/{model_name_}"}
            if rot:
                apply["y"] = rot
            entries.append({"when": {**when, "facing": facing}, "apply": apply})
    return {"multipart": entries}


def write_part(parts, name, elements, when, tall, bb=None, **extra):
    """Ein Multipart-Teil; hohe Signale bekommen je Hälfte ein eigenes Modell."""
    halves = split_tall(elements) if tall else {None: elements}
    for half, half_elements in halves.items():
        if not half_elements:
            continue
        model_name_ = f"{name}_{half}" if half else name
        write_json(ASSETS / "models/block" / f"{model_name_}.json", {**model(half_elements, bb, **extra), **(
            {"render_type": "minecraft:cutout"} if "mastschild_ms" in json.dumps(half_elements) else {})})
        parts.append(({**when, "half": half} if half else when, model_name_))


def tall_item_model(elements, bb=None):
    return {**model(elements, bb), "display": bb.display if bb and bb.display else TALL_ITEM_DISPLAY}


def generate_ks():
    for name, spec in KS_TYPES.items():
        bb = BLOCKBENCH.get(name)
        tall = spec.get("tall", False)
        height = 32 if tall else 16
        parts = []
        for mount in MOUNTS:
            elements = bb.support(mount, spec["arms"], height) if bb else support(mount, spec["arms"], height)
            write_part(parts, f"{name}_support_{mount}", elements, {"mount": mount}, tall, bb)
        # mit Blockbench-Modell: fester Teil = alles außer Lampen, je Begriff nur die Lampen
        extras = bb.body if bb else spec["extras"]()
        write_part(parts, f"{name}_schild", extras, {}, tall, bb)
        for aspect in spec["aspects"]:
            elements = bb.lamps_for(KS_LIT[aspect]) if bb else ks_screen(spec, KS_LIT[aspect])
            write_part(parts, f"{name}_{aspect}", elements, {"aspect": aspect}, tall, bb)

        write_json(ASSETS / "blockstates" / f"{name}.json", multipart(parts))

        if bb:
            item_elements = bb.assemble(KS_LIT["ks1"], "none", spec["arms"], height)
        else:
            item_elements = support("none", spec["arms"], height) + extras + ks_screen(spec, KS_LIT["ks1"])
        places = overlay_places(name)
        if "zs3v" in places:
            item_elements += overlay_box(places["zs3v"])
        if tall:
            item = tall_item_model(item_elements, bb)
        else:
            item = model(item_elements, bb)
            if bb and bb.display:
                item["display"] = bb.display
        write_json(ASSETS / "models/item" / f"{name}.json",
                   {**item, "render_type": "minecraft:cutout"} if "mastschild_ms" in json.dumps(item_elements) else item)


def variants(props, model_for):
    """props: Liste (name, werte); model_for(dict) -> Modellname"""
    result = {}

    def rec(i, current):
        if i == len(props):
            for facing, rot in FACINGS.items():
                key = ",".join(f"{k}={v}" for k, v in sorted({**current, "facing": facing}.items()))
                entry = {"model": f"{MOD_ID}:block/{model_for(current)}"}
                if rot:
                    entry["y"] = rot
                result[key] = entry
            return
        name, values = props[i]
        for v in values:
            rec(i + 1, {**current, name: v})

    rec(0, {})
    return {"variants": result}


def model_name(base, mount):
    return base if mount == "none" else f"{base}_{mount}"


def generate_models():
    haupt_bb, kombi_bb = BLOCKBENCH.get("hv_hauptsignal"), BLOCKBENCH.get("hv_kombisignal")
    for mount in MOUNTS:
        for a in HP:
            for half, elements in split_tall(hauptsignal_elements(HP_LIT[a], mount)).items():
                write_json(ASSETS / "models/block" / f"{model_name(f'hv_hauptsignal_{a}', mount)}_{half}.json",
                           model(elements, haupt_bb))
        for a in VR:
            write_json(ASSETS / "models/block" / f"{model_name(f'hv_vorsignal_{a}', mount)}.json",
                       model(vorsignal_elements(VR_LIT[a], mount), BLOCKBENCH.get("hv_vorsignal")))
        for hp in HP:
            for vr in VR + ["dark"]:
                for half, elements in split_tall(kombisignal_elements(HP_LIT[hp] | VR_LIT[vr], mount)).items():
                    write_json(ASSETS / "models/block" /
                               f"{model_name(f'hv_kombisignal_{hp}_{vr}', mount)}_{half}.json",
                               model(elements, kombi_bb))
    for a in SH_LIT:
        for on_mast in (False, True):
            write_json(ASSETS / "models/block" / f"sh_sperrsignal_{a}{'_mast' if on_mast else ''}.json",
                       sperrsignal_model(SH_LIT[a], on_mast))
    write_json(ASSETS / "models/block/signal_mast.json",
               {"parent": "block/block", "textures": {"particle": tex("signal_mast"), "mast": tex("signal_mast")},
                "elements": [box([6.5, 0, 6.5], [9.5, 16, 9.5], "#mast")]})

    write_json(ASSETS / "blockstates/hv_hauptsignal.json",
               variants([("aspect", HP), ("mount", MOUNTS), ("half", HALVES)],
                        lambda s: model_name(f"hv_hauptsignal_{s['aspect']}", s["mount"]) + f"_{s['half']}"))
    write_json(ASSETS / "blockstates/hv_vorsignal.json",
               variants([("aspect", VR), ("mount", MOUNTS)],
                        lambda s: model_name(f"hv_vorsignal_{s['aspect']}", s["mount"])))
    write_json(ASSETS / "blockstates/hv_kombisignal.json",
               variants([("aspect", HP), ("distant", VR + ["dark"]), ("mount", MOUNTS), ("half", HALVES)],
                        lambda s: model_name(f"hv_kombisignal_{s['aspect']}_{s['distant']}", s["mount"])
                        + f"_{s['half']}"))
    write_json(ASSETS / "blockstates/sh_sperrsignal.json",
               variants([("aspect", list(SH_LIT)), ("mast", ["false", "true"])],
                        lambda s: f"sh_sperrsignal_{s['aspect']}{'_mast' if s['mast'] == 'true' else ''}"))
    write_json(ASSETS / "blockstates/signal_mast.json",
               {"variants": {"": {"model": f"{MOD_ID}:block/signal_mast"}}})

    write_json(ASSETS / "models/item/hv_hauptsignal.json",
               tall_item_model(hauptsignal_elements(HP_LIT["hp1"], "none"), haupt_bb))
    write_json(ASSETS / "models/item/hv_kombisignal.json",
               tall_item_model(kombisignal_elements(HP_LIT["hp1"] | VR_LIT["vr1"], "none"), kombi_bb))
    items = {"hv_vorsignal": "hv_vorsignal_vr1", "sh_sperrsignal": "sh_sperrsignal_sh0", "signal_mast": "signal_mast"}
    for item, parent in items.items():
        bb = BLOCKBENCH.get(item)
        write_json(ASSETS / "models/item" / f"{item}.json", {"parent": f"{MOD_ID}:block/{parent}", **(
            {"display": bb.display} if bb and bb.display else {})})


def generate_data():
    blocks = ["hv_hauptsignal", "hv_vorsignal", "hv_kombisignal", "sh_sperrsignal", "ks_hauptsignal",
              "ks_vorsignal", "ks_mehrabschnittssignal", "signal_mast"]
    for b in blocks:
        conditions = [{"condition": "minecraft:survives_explosion"}]
        if b in TALL_BLOCKS:
            # nur die untere Hälfte droppt das Signal
            conditions.append({"condition": "minecraft:block_state_property", "block": f"{MOD_ID}:{b}",
                               "properties": {"half": "lower"}})
        write_json(DATA / MOD_ID / "loot_table/blocks" / f"{b}.json", {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{MOD_ID}:{b}"}],
                       "conditions": conditions}],
        })
    write_json(DATA / "minecraft/tags/block/mineable/pickaxe.json",
               {"values": [f"{MOD_ID}:{b}" for b in blocks]})


# --- Eigene Modelle aus Blockbench (tools/models/<block>.json, Format "Java Block/Item") ---

# Elementnamen (oder Gruppennamen) mit Sonderrolle; eine angehängte Zahl ist erlaubt ("arm2", "lamp_red3")
OVERLAY_MARKERS = ("kennlicht", "zs1", "zs7", "zs2v", "zs2", "zs6", "zs3v", "zs3")
ROLE = re.compile(r"^(mast|arm|sockel|" + "|".join(OVERLAY_MARKERS) + r")\d*$")
LAMP = re.compile(r"^lamp_([a-z_]*[a-z])\d*$")
ANGLES = {-45, -22.5, 0, 22.5, 45}


def signal_lamps():
    """Block -> erlaubte Lampennamen."""
    lamps = {"hv_hauptsignal": set(HV_HAUPT["lamps"]), "hv_vorsignal": set(HV_VOR["lamps"]),
             "hv_kombisignal": set(HV_KOMBI["lamps"]) | set(HV_KOMBI["vr_lamps"]), "sh_sperrsignal": set(SH_LAMPS)}
    lamps.update({name: set(spec["lamps"]) for name, spec in KS_TYPES.items()})
    return lamps


def role_of(label):
    """("lamp", key), (rolle, None) oder None."""
    if m := LAMP.match(label):
        return "lamp", m.group(1)
    if m := ROLE.match(label):
        return m.group(1), None
    return None


class BlockbenchModel:
    """Exportiertes Blockbench-Modell. Der Schirm zeigt nach Norden, wie bei den eingebauten Modellen.

    Rollen über den Element- oder Gruppennamen:
      lamp_<name>  Lampe; Nordfläche wird je Begriff zur Lampentextur (an/aus)
      mast         nur frei stehend sichtbar (sonst Ausleger); fehlt er, kommt der eingebaute Mast
      arm          Markierung: Höhe der Wandausleger, wird selbst nicht gezeichnet
      sockel       nur Lichtsperrsignal: Fuß, wenn es nicht auf einem Signalmast steht
      kennlicht, zs1, zs7, zs2, zs2v, zs6, zs3, zs3v
                   Markierung: Lage der Overlay-Anzeige; das Skript zeichnet dort Lampe bzw. Kasten
                   und Zeichen (zs1/zs7: ein Element je Lampe)
    Alles andere ist fester Signalkörper.
    """

    def __init__(self, path, block, lamp_names):
        self.path = path
        data = json.loads(path.read_text(encoding="utf-8"))
        self.display = data.get("display")
        self.textures = {key: self.texture_ref(value) for key, value in data.get("textures", {}).items()}
        self.body, self.lamps, self.mast, self.arm_heights, self.sockel = [], [], [], [], []
        self.overlay = {}  # Marker -> Elemente
        group_role = {}
        self.collect_groups(data.get("groups", []), None, group_role)

        tall = block in TALL_BLOCKS
        for i, raw in enumerate(data.get("elements", [])):
            label = raw.get("name", f"#{i}")
            role = role_of(raw.get("name", "")) or group_role.get(i)
            element = self.clean(raw, label)
            if role is None:
                self.body.append(element)
            elif role[0] == "lamp":
                if role[1] not in lamp_names:
                    self.fail(f"Lampe '{label}' gibt es bei {block} nicht. Erlaubt: "
                              + ", ".join(f"lamp_{n}" for n in sorted(lamp_names)))
                if "north" not in element["faces"]:
                    self.fail(f"Lampe '{label}' braucht eine Nordfläche (die Schirmseite)")
                self.lamps.append((role[1], element))
            elif role[0] == "mast":
                self.mast.append(element)
            elif role[0] == "arm":
                self.arm_heights.append((element["from"][1], element["to"][1]))
            elif role[0] == "sockel":
                self.sockel.append(element)
            else:
                self.overlay.setdefault(role[0], []).append(element)

        missing = lamp_names - {key for key, _ in self.lamps}
        if missing:
            self.warn("keine Lampe für " + ", ".join(f"lamp_{n}" for n in sorted(missing))
                      + " – Begriffe mit diesen Lampen bleiben dort dunkel")
        if block == "sh_sperrsignal" and (self.mast or self.arm_heights):
            self.warn("mast/arm werden beim Lichtsperrsignal nicht verwendet (Fuß: 'sockel')")
        if block != "sh_sperrsignal" and self.sockel:
            self.warn("'sockel' gibt es nur beim Lichtsperrsignal; diese Elemente werden ignoriert")
        allowed = {k for k in OVERLAYS.get(block, {}) if k in OVERLAY_MARKERS}
        for key in sorted(set(self.overlay) - allowed):
            self.warn(f"{block} hat keine Anzeige '{key}'; Markierung wird ignoriert")
            del self.overlay[key]
        for key in sorted(allowed - set(self.overlay)):
            self.warn(f"keine Markierung '{key}' – die Anzeige sitzt an der Stelle des eingebauten Modells")
        for key, markers in self.overlay.items():
            if key not in ("zs1", "zs7") and len(markers) > 1:
                self.warn(f"mehrere Markierungen '{key}' – nur die erste zählt")
        top = 32 if tall else 16
        if any(e["to"][1] > top for e in self.body + self.mast + [e for _, e in self.lamps]):
            self.warn(f"Elemente ragen über {top} px hinaus" + ("" if tall else
                      " – das Signal ist nur einen Block hoch, der Block darüber verdeckt sie evtl."))

    def fail(self, message):
        sys.exit(f"Fehler in {self.path.name}: {message}")

    def warn(self, message):
        print(f"Hinweis zu {self.path.name}: {message}")

    def collect_groups(self, nodes, inherited, result):
        """Gruppennamen vererben ihre Rolle an alle enthaltenen Elemente (innerste Gruppe gewinnt)."""
        for node in nodes:
            if isinstance(node, dict):
                self.collect_groups(node.get("children", []), role_of(node.get("name", "")) or inherited, result)
            elif inherited:
                result[node] = inherited

    def texture_ref(self, value):
        """Texturpfad aus dem Export in eine Modreferenz übersetzen; fremde Pfade -> Dateiname."""
        if isinstance(value, dict):  # {"sprite": ..., "force_translucent": true}
            value = value["sprite"]
        if value.startswith("#") or (":" in value and not re.match(r"^[A-Za-z]:[\\/]", value)):
            ref = value
        else:
            ref = tex(re.split(r"[\\/]", value)[-1].removesuffix(".png"))
        if ref.startswith(f"{MOD_ID}:block/"):
            name = ref.split("/", 1)[1]
            if not any((d / f"{name}.png").exists() for d in (ASSETS / "textures/block", BB_TEXTURES)):
                self.warn(f"Textur '{name}.png' nicht gefunden – nach tools/models/textures/ legen")
        return ref

    def clean(self, raw, label):
        element = {k: copy.deepcopy(v) for k, v in raw.items() if k not in ("name", "color", "light_emission")}
        if raw.get("light_emission"):
            # Vanilla-Feld erst ab 1.21.2; NeoForge 1.21.1 versteht nur neoforge_data
            element.setdefault("neoforge_data", {"block_light": raw["light_emission"],
                                                 "sky_light": raw["light_emission"]})
        rotation = element.get("rotation")
        if rotation is not None:
            if "angle" not in rotation:
                self.fail(f"'{label}' ist um mehrere Achsen gedreht; Minecraft 1.21.1 erlaubt nur eine")
            if rotation["angle"] not in ANGLES:
                self.fail(f"'{label}' ist um {rotation['angle']}° gedreht; erlaubt sind -45, -22.5, 0, 22.5, 45")
        if any(not -16 <= c <= 32 for c in element["from"] + element["to"]):
            self.fail(f"'{label}' liegt außerhalb von -16..32 px")
        for side, face in element.get("faces", {}).items():
            ref = face.get("texture", "")
            if ref.startswith("#") and ref[1:] not in self.textures and ref[1:] not in TEXTURES:
                self.warn(f"'{label}' ({side}) nutzt unbekannte Texturvariable {ref}")
        return element

    def lamps_for(self, lit):
        """Lampen für einen Begriff; lit wie bei main_screen."""
        result = []
        for key, element in self.lamps:
            element = copy.deepcopy(element)
            on = key in lit
            face = element["faces"]["north"]
            face["texture"] = f"#lamp_{lamp_color(lit, key)}" if on else "#lamp_off"
            face["uv"] = [0, 0, 16, 16]  # ganze Lampentextur, unabhängig von der UV in Blockbench
            if not on:
                # dunkle Lampe wird normal schattiert, auch wenn sie in Blockbench leuchtend angelegt ist
                element.pop("shade", None)
                element.pop("neoforge_data", None)
            result.append(glow(element) if on else element)
        return result

    def support(self, mount, arm_heights, height):
        if mount == "none":
            return copy.deepcopy(self.mast) if self.mast else [post(height)]
        return support(mount, self.arm_heights or arm_heights, height)

    def assemble(self, lit, mount, arm_heights, height):
        return self.support(mount, arm_heights, height) + copy.deepcopy(self.body) + self.lamps_for(lit)


def load_blockbench():
    lamps = signal_lamps()
    models = {}
    for path in sorted(BB_DIR.glob("*.json")):
        if path.stem not in lamps:
            print(f"Hinweis: {path.name} passt zu keinem Signal ({', '.join(sorted(lamps))}) – ignoriert")
            continue
        models[path.stem] = BlockbenchModel(path, path.stem, lamps[path.stem])
        print(f"Blockbench-Modell: {path.name}")
    return models


def copy_bb_textures():
    """Eigene Texturen (auch Ersatz für Platzhalter wie lamp_red.png) ins Ressourcenverzeichnis."""
    for path in sorted(BB_TEXTURES.glob("*.png")) + sorted(BB_TEXTURES.glob("*.png.mcmeta")):
        target = ASSETS / "textures/block" / path.name
        if not target.exists() or target.read_bytes() != path.read_bytes():
            shutil.copyfile(path, target)


def template_texture(name):
    # Blockbench lädt absolute Windows-Pfade direkt und exportiert sie als create_railway_systems:block/...
    if os.name == "nt":
        return (ASSETS / "textures/block" / f"{name}.png").resolve().as_posix()
    return tex(name)


def overlay_markers(name):
    """Overlay-Anzeigen eines eingebauten Signals als benannte Markierungen für die Vorlage."""
    elements = []
    for key, place in OVERLAYS.get(name, {}).items():
        if key == "kennlicht":
            elements.append(lamp(*place, "white", True, name=key))
        elif key in ("zs1", "zs7"):
            elements += [lamp(*p, "white" if key == "zs1" else "yellow", True, name=key) for p in place]
        elif key in OVERLAY_MARKERS:
            elements += zs_box(*place, name=key)
    return elements


def write_templates():
    """Eingebaute Signale als Blockbench-Startpunkt, alle Lampen an, Marker benannt."""
    every = {name: set(lamps) for name, lamps in signal_lamps().items()}
    templates = {
        "hv_hauptsignal": hauptsignal_elements(every["hv_hauptsignal"], "none")
        + support("back", HV_HAUPT["arms"], 32),
        "hv_vorsignal": vorsignal_elements(every["hv_vorsignal"], "none") + support("back", HV_VOR["arms"]),
        "hv_kombisignal": kombisignal_elements(every["hv_kombisignal"], "none")
        + support("back", HV_KOMBI["arms"], 32),
        "sh_sperrsignal": sperrsignal_foot(False) + sperrsignal_head(every["sh_sperrsignal"]),
    }
    for name, spec in KS_TYPES.items():
        height = 32 if spec.get("tall") else 16
        elements = support("none", spec["arms"], height) + support("back", spec["arms"], height)
        templates[name] = elements + spec["extras"]() + ks_screen(spec, every[name])

    for name, elements in templates.items():
        elements = elements + overlay_markers(name)
        used = sorted({f["texture"][1:] for e in elements for f in e["faces"].values()})
        template = {"textures": {"particle": template_texture("signal_screen"),
                                 **{key: template_texture(TEXTURES[key].split("/", 1)[1]) for key in used}},
                    "elements": [{k: v for k, v in e.items() if k != "neoforge_data"} for e in elements]}
        if name in TALL_BLOCKS:
            template["display"] = TALL_ITEM_DISPLAY
        write_json(BB_TEMPLATES / f"{name}.json", template)
    print("Vorlagen geschrieben nach", BB_TEMPLATES)


# --- Texturen (16x16 Platzhalter-Pixelart) ---

def save(img, name):
    path = ASSETS / "textures/block" / f"{name}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.exists():
        with Image.open(path) as old:
            # gleiche Pixel nicht neu schreiben: andere Pillow-Versionen kodieren PNGs anders
            if old.size == img.size and old.convert("RGBA").tobytes() == img.convert("RGBA").tobytes():
                return
    img.save(path)


DIGITS = {
    1: ["010", "110", "010", "010", "111"], 2: ["111", "001", "111", "100", "111"],
    3: ["111", "001", "011", "001", "111"], 4: ["101", "101", "111", "001", "001"],
    5: ["111", "100", "111", "001", "111"], 6: ["111", "100", "111", "101", "111"],
    7: ["111", "001", "010", "010", "010"], 8: ["111", "101", "111", "101", "111"],
    9: ["111", "101", "111", "001", "111"],
}


# Buchstaben der Richtungsanzeiger Zs2/Zs2v im selben 3x5-Raster
LETTER_GLYPHS = {
    "A": ["010", "101", "111", "101", "101"], "B": ["110", "101", "110", "101", "110"],
    "C": ["011", "100", "100", "100", "011"], "D": ["110", "101", "101", "101", "110"],
    "E": ["111", "100", "110", "100", "111"], "F": ["111", "100", "110", "100", "100"],
    "G": ["011", "100", "101", "101", "011"], "H": ["101", "101", "111", "101", "101"],
    "I": ["111", "010", "010", "010", "111"], "J": ["001", "001", "001", "101", "010"],
    "K": ["101", "101", "110", "101", "101"], "L": ["100", "100", "100", "100", "111"],
    "M": ["101", "111", "111", "101", "101"], "N": ["110", "101", "101", "101", "101"],
    "O": ["010", "101", "101", "101", "010"], "P": ["110", "101", "110", "100", "100"],
    "Q": ["010", "101", "101", "110", "011"], "R": ["110", "101", "110", "101", "101"],
    "S": ["011", "100", "010", "001", "110"], "T": ["111", "010", "010", "010", "010"],
    "U": ["101", "101", "101", "101", "111"], "V": ["101", "101", "101", "101", "010"],
    "W": ["101", "101", "111", "111", "101"], "X": ["101", "101", "010", "101", "101"],
    "Y": ["101", "101", "010", "010", "010"], "Z": ["111", "001", "010", "100", "111"],
}


def glyph_img(rows, color):
    """Zeichen eines Lichtanzeigers (3x5-Raster, dreifach skaliert) auf dunklem Grund."""
    img = Image.new("RGBA", (16, 16), (18, 18, 20, 255))
    for row, bits in enumerate(rows):
        for col, bit in enumerate(bits):
            if bit == "1":
                x, y = 4 + col * 3, 1 + row * 3
                ImageDraw.Draw(img).rectangle([x, y, x + 2, y + 2], fill=color)
    return img


def generate_textures():
    # randlos, damit an Blockgrenzen geteilte Schirme keine Nahtlinie zeigen
    screen = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            v = 21 + (x * 5 + y * 3) % 3
            screen.putpixel((x, y), (v, v, v + 2, 255))
    save(screen, "signal_screen")

    mast = Image.new("RGBA", (16, 16))
    for x in range(16):
        shade = 120 + int(30 * (1 - abs(x - 6) / 8))
        for y in range(16):
            mast.putpixel((x, y), (shade, shade + 4, shade + 8, 255))
    save(mast, "signal_mast")

    # Mastschild weiß-rot-weiß: drei gleich große Felder
    schild = Image.new("RGBA", (16, 16), (240, 240, 240, 255))
    d = ImageDraw.Draw(schild)
    d.rectangle([0, 5, 15, 10], fill=(200, 20, 30, 255))
    d.rectangle([0, 0, 15, 15], outline=(20, 20, 20, 255))
    save(schild, "mastschild_hp")

    # Mastschild mit gelbem, nach unten zeigendem Dreieck darunter (Mehrabschnittssignal)
    ms = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(ms)
    d.rectangle([0, 0, 15, 11], fill=(240, 240, 240, 255), outline=(20, 20, 20, 255))
    d.rectangle([1, 4, 14, 7], fill=(200, 20, 30, 255))
    d.polygon([(2, 12), (13, 12), (8, 15), (7, 15)], fill=(250, 200, 30, 255))
    save(ms, "mastschild_ms")

    concrete = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            v = 150 + ((x * 7 + y * 13) % 5) * 6 - (8 if (x + y * 3) % 7 == 0 else 0)
            concrete.putpixel((x, y), (v, v, v - 6, 255))
    save(concrete, "concrete")

    plate = Image.new("RGBA", (16, 16), (245, 245, 245, 255))
    d = ImageDraw.Draw(plate)
    d.rectangle([0, 0, 15, 15], outline=(20, 20, 20, 255))
    for x0 in (3, 7, 11):  # stilisierte Ziffern
        d.rectangle([x0, 5, x0 + 2, 10], outline=(20, 20, 20, 255))
    save(plate, "number_plate")

    colors = {"red": (255, 40, 30), "green": (40, 255, 110), "yellow": (255, 200, 40), "white": (225, 230, 255)}

    def lamp_img(core, ring, highlight):
        img = Image.new("RGBA", (16, 16), (21, 21, 23, 255))
        d = ImageDraw.Draw(img)
        d.ellipse([0, 0, 15, 15], fill=ring)
        d.ellipse([2, 2, 13, 13], fill=core)
        if highlight:
            d.ellipse([4, 4, 8, 8], fill=highlight)
        return img

    off = lamp_img((48, 48, 52, 255), (30, 30, 33, 255), None)
    save(off, "lamp_off")

    # blinkendes Grün: zwei Frames (an/aus) als animierte Textur
    r, g, b = colors["green"]
    blink = Image.new("RGBA", (16, 32))
    blink.paste(lamp_img((r, g, b, 255), (r // 2, g // 2, b // 2, 255),
                         (min(255, r + 120), min(255, g + 120), min(255, b + 120), 255)), (0, 0))
    blink.paste(off, (0, 16))
    save(blink, "lamp_green_blink")
    write_json(ASSETS / "textures/block/lamp_green_blink.png.mcmeta", {"animation": {"frametime": 10}})

    r, g, b = colors["white"]
    white_blink = Image.new("RGBA", (16, 32))
    white_blink.paste(lamp_img((r, g, b, 255), (r // 2, g // 2, b // 2, 255), (255, 255, 255, 255)), (0, 0))
    white_blink.paste(off, (0, 16))
    save(white_blink, "lamp_white_blink")
    write_json(ASSETS / "textures/block/lamp_white_blink.png.mcmeta", {"animation": {"frametime": 10}})

    # Zs6: weiße Lichtpunktreihe von links oben nach rechts unten
    zs6 = Image.new("RGBA", (16, 16), (18, 18, 20, 255))
    d = ImageDraw.Draw(zs6)
    for i in range(6):
        x, y = 2 + i * 2, 1 + i * 2.5
        d.rectangle([x, int(y), x + 1, int(y) + 1], fill=(240, 240, 255, 255))
    save(zs6, "zs6_on")

    # Zs8: Lichtpunktreihe des Zs6, blinkend (zwei Frames an/aus)
    zs8 = Image.new("RGBA", (16, 32), (18, 18, 20, 255))
    zs8.paste(zs6, (0, 0))
    save(zs8, "zs8_on")
    write_json(ASSETS / "textures/block/zs8_on.png.mcmeta", {"animation": {"frametime": 10}})

    zs = Image.new("RGBA", (16, 16), (18, 18, 20, 255))
    ImageDraw.Draw(zs).rectangle([0, 0, 15, 15], outline=(50, 50, 54, 255))
    save(zs, "zs_box")
    for n in SPEEDS:
        save(glyph_img(DIGITS[n], (240, 240, 240, 255)), f"zs3_{n}")
        save(glyph_img(DIGITS[n], (255, 200, 40, 255)), f"zs3v_{n}")
    for n in LETTERS:
        rows = LETTER_GLYPHS[chr(ord("A") + n - 1)]
        save(glyph_img(rows, (240, 240, 240, 255)), f"zs2_{n}")
        save(glyph_img(rows, (255, 200, 40, 255)), f"zs2v_{n}")

    ne2 = Image.new("RGBA", (16, 16), (245, 245, 245, 255))
    d = ImageDraw.Draw(ne2)
    d.rectangle([0, 0, 15, 15], outline=(15, 15, 15, 255), width=2)
    d.line([(2, 2), (8, 8), (13, 2)], fill=(15, 15, 15, 255), width=2)
    d.line([(2, 13), (8, 8), (13, 13)], fill=(15, 15, 15, 255), width=2)
    save(ne2, "ne2")
    for name, (r, g, b) in colors.items():
        save(lamp_img((r, g, b, 255), (r // 2, g // 2, b // 2, 255),
                      (min(255, r + 120), min(255, g + 120), min(255, b + 120), 255)), f"lamp_{name}")


if __name__ == "__main__":
    if "--vorlagen" in sys.argv[1:]:
        write_templates()
        sys.exit()
    BLOCKBENCH.update(load_blockbench())
    generate_models()
    generate_ks()
    generate_overlays()
    generate_data()
    generate_textures()
    copy_bb_textures()
    # Modelle früherer Läufe entfernen (z. B. nach Wechsel zwischen Blockbench- und eingebautem Modell);
    # block/overlay räumt generate_overlays() selbst auf
    for folder in ("blockstates", "models/block", "models/item"):
        for path in (ASSETS / folder).glob("*.json"):
            if path.resolve() not in WRITTEN:
                path.unlink()
    print("Assets erzeugt unter", ROOT)
