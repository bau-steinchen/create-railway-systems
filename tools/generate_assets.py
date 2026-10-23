#!/usr/bin/env python3
"""Erzeugt Blockstates, Modelle, Platzhalter-Texturen, Loot-Tables und Tags der Signale.

Aufruf aus dem Projektverzeichnis:  python3 tools/generate_assets.py
Benötigt Pillow (pip install pillow) für die Texturen.
"""
import copy
import json
from pathlib import Path

from PIL import Image, ImageDraw

MOD_ID = "create_railway_systems"
ROOT = Path(__file__).resolve().parent.parent / "src" / "main" / "resources"
ASSETS = ROOT / "assets" / MOD_ID
DATA = ROOT / "data"

FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}
HP = ["hp0", "hp1", "hp2"]
MOUNTS = ["none", "back", "left", "right"]
HALVES = ["lower", "upper"]
TALL_BLOCKS = {"hv_hauptsignal", "hv_kombisignal", "ks_hauptsignal", "ks_mehrabschnittssignal"}
VR = ["vr0", "vr1", "vr2"]

# Welche Lampen leuchten bei welchem Begriff
HP_LIT = {"hp0": {"red"}, "hp1": {"green"}, "hp2": {"green", "yellow"}}
VR_LIT = {"vr0": {"y_bl", "y_tr"}, "vr1": {"g_bl", "g_tr"}, "vr2": {"g_bl", "y_tr"}, "dark": set()}
SH_LIT = {"sh0": {"r_l", "r_r"}, "sh1": {"w_bl", "w_tr"}}
# Ks: Lampe -> Textur (blinkendes Grün als animierte Textur)
KS_LIT = {"hp0": {"red": "red"}, "ks1": {"green": "green"}, "ks1_blink": {"green": "green_blink"},
          "ks2": {"yellow": "yellow"}}
SPEEDS = range(1, 10)
LAMP_COLOR = {"red": "red", "green": "green", "yellow": "yellow",
              "r_l": "red", "r_r": "red", "w_bl": "white", "w_tr": "white",
              "y_bl": "yellow", "y_tr": "yellow", "g_bl": "green", "g_tr": "green"}


def write_json(path: Path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def tex(name):
    return f"{MOD_ID}:block/{name}"


def box(frm, to, texture, north=None):
    faces = {f: {"texture": texture} for f in ("north", "east", "south", "west", "up", "down")}
    if north:
        # Eigene Frontfläche (Lampe, Ziffer, Schild) zeigt immer die ganze Textur. Ohne "uv"
        # schneidet Minecraft den Ausschnitt passend zur Position im Block aus.
        faces["north"] = {"texture": north, "uv": [0, 0, 16, 16]}
    return {"from": frm, "to": to, "faces": faces}


def mastschild(vx0, vx1, y0, y1, texture="#mastschild"):
    """Schild vor dem Mast; nur die Front trägt das Schildbild, Kanten in Mastfarbe."""
    x0, x1 = mirror_x(vx0, vx1)
    return box([x0, y0, 5.5], [x1, y1, 6.5], "#mast", north=texture)


def mirror_x(x0, x1):
    """Betrachterkoordinate (x von links, vor dem Schirm stehend) -> Modellraum.
    Der Schirm zeigt nach Norden; der Betrachter blickt nach Süden, sein "links" ist +x."""
    return 16 - x1, 16 - x0


def lamp(vx0, y0, size, color, lit):
    x0, _ = mirror_x(vx0, vx0 + size)
    element = box([x0, y0, 3.5], [x0 + size, y0 + size, 4], "#screen",
                  north=f"#lamp_{color}" if lit else "#lamp_off")
    if lit:
        element["shade"] = False
        element["neoforge_data"] = {"block_light": 15, "sky_light": 15}
    return element


def post(height=16):
    # deckungsgleich mit dem Signalmast-Block, damit kein Versatz am Übergang entsteht
    return box([6.5, 0, 6.5], [9.5, height, 9.5], "#mast")


def support(mount, arm_heights, height=16):
    """Senkrechter Mast (frei stehend) oder waagerechte Ausleger zum Nachbarblock (Wandmontage).
    Modellraum: Schirm zeigt nach Norden; "links"/"rechts" aus Sicht des Betrachters vor dem Schirm."""
    if mount == "none":
        return [post(height)]
    arms = {"back": ([7, 6.5], [9, 16]), "left": ([7, 6.5], [16, 8.5]), "right": ([0, 6.5], [9, 8.5])}
    (x0, z0), (x1, z1) = arms[mount]
    return [box([x0, y0, z0], [x1, y1, z1], "#mast") for y0, y1 in arm_heights]


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
    "zs_box": tex("zs_box"),
    "ne2": tex("ne2"),
    "mastschild_ms": tex("mastschild_ms"),
    "concrete": tex("concrete"),
    "number_plate": tex("number_plate"),
}


def model(elements, **extra_textures):
    return {"parent": "block/block", "textures": {**TEXTURES, **extra_textures}, "elements": elements}


# --- Zwei Blöcke hohe Signale: Entwurf in 0..32 px, Aufteilung in untere/obere Hälfte ---

TALL_ITEM_DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, -3.2, 0], "scale": [0.4, 0.4, 0.4]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 1, 0], "scale": [0.2, 0.2, 0.2]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, -2.5, 0], "scale": [0.3, 0.3, 0.3]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 0, 0], "scale": [0.25, 0.25, 0.25]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, -2, 0], "scale": [0.3, 0.3, 0.3]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, -2, 0], "scale": [0.3, 0.3, 0.3]},
}


def clip_y(element, lo, hi):
    """Teil eines Elements zwischen lo und hi (px), um lo nach unten verschoben, oder None."""
    y0, y1 = element["from"][1], element["to"][1]
    a, b = max(y0, lo), min(y1, hi)
    if b <= a:
        return None
    piece = copy.deepcopy(element)
    piece["from"][1], piece["to"][1] = a - lo, b - lo
    north = piece["faces"].get("north")
    if north and "uv" in north and (a, b) != (y0, y1):
        # explizite UV der Frontfläche anteilig mitschneiden (v läuft von oben nach unten)
        u0, v0, u1, v1 = element["faces"]["north"]["uv"]
        north["uv"] = [u0, v0 + (y1 - b) / (y1 - y0) * (v1 - v0), u1, v0 + (y1 - a) / (y1 - y0) * (v1 - v0)]
    return piece


def split_tall(elements):
    """{"lower": [...], "upper": [...]} aus einem 32 px hohen Entwurf."""
    return {half: [p for p in (clip_y(e, lo, lo + 16) for e in elements) if p]
            for half, lo in (("lower", 0), ("upper", 16))}


def hood(vx0, y0, size):
    """Schute über einer Lampe."""
    x0, x1 = mirror_x(vx0 - 0.25, vx0 + size + 0.25)
    return box([x0, y0 + size, 2.5], [x1, y0 + size + 0.5, 4], "#screen")


def main_screen(lit, vx0, vx1, y0, y1, lamps):
    """Schirm mit Lampen; alle x-Werte in Betrachterkoordinaten (siehe mirror_x)."""
    x0, x1 = mirror_x(vx0, vx1)
    elements = [box([x0, y0, 4], [x1, y1, 6.5], "#screen")]
    for name, (lx, ly, size) in lamps.items():
        color = lit[name] if isinstance(lit, dict) and name in lit else LAMP_COLOR[name]
        elements.append(lamp(lx, ly, size, color, name in lit))
    return elements


def hauptsignal_elements(aspect, mount):
    """H/V-Hauptsignal, zwei Blöcke hoch."""
    lamps = {"green": (6.25, 24.5, 3.5), "red": (6.25, 20, 3.5), "yellow": (6.25, 12.5, 3.5)}
    elements = support(mount, [(26, 28), (5, 7)], height=32)
    elements.append(mastschild(6.5, 9.5, 3, 9.5))
    elements += main_screen(HP_LIT[aspect], 4.5, 11.5, 11.5, 29, lamps)
    elements += [hood(vx, ly, size) for vx, ly, size in lamps.values()]
    return elements


def vorsignal_model(aspect, mount):
    elements = support(mount, [(7, 9)])
    elements += main_screen(VR_LIT[aspect], 3, 13, 4, 12, {
        "y_bl": (3.5, 5, 2.5), "g_bl": (6.5, 5, 2.5), "g_tr": (7, 8.5, 2.5), "y_tr": (10, 8.5, 2.5)})
    return model(elements)


def kombisignal_elements(hp, vr, mount):
    """H/V-Hauptsignal mit Vorsignal am Mast, zwei Blöcke hoch: Hauptschirm, Mastschild, Vorsignal."""
    lamps = {"green": (6.5, 27, 3), "red": (6.5, 23.25, 3), "yellow": (6.5, 19.5, 3)}
    elements = support(mount, [(27, 29), (5, 7)], height=32)
    elements += main_screen(HP_LIT[hp], 4.75, 11.25, 18.75, 31, lamps)
    elements += [hood(vx, ly, size) for vx, ly, size in lamps.values()]
    elements.append(mastschild(6.5, 9.5, 11.5, 17.5))
    elements += main_screen(VR_LIT[vr], 3, 13, 2.5, 10.5, {
        "y_bl": (3.5, 3.5, 2.5), "g_bl": (6.5, 3.5, 2.5), "g_tr": (7, 7.25, 2.5), "y_tr": (10, 7.25, 2.5)})
    return elements


def sperrsignal_model(aspect, on_mast):
    """Gleissperrsignal nach DB-Vorbild: breiter Schirm, oben Rot – Lücke – Weiß/Rot, unten links Weiß,
    rechts Nummernschild und weiß-rot-weißes Mastschild."""
    if on_mast:
        # Maststummel deckungsgleich mit dem Signalmast
        elements = [box([6.5, 0, 6.5], [9.5, 2.5, 9.5], "#mast")]
    else:
        # Betonsockel mit kurzem Halter
        elements = [box([5, 0, 5], [11, 2, 11], "#concrete"), box([7, 2, 7], [9, 2.5, 9], "#mast")]
    elements.append(box([4.5, 2.5, 4], [11.5, 3, 9.5], "#mast"))  # Halteplatte unter dem Schirm

    lamps = {"r_l": (2.75, 7.75, 2.5), "w_tr": (8, 7.75, 2.5), "r_r": (10.75, 7.75, 2.5), "w_bl": (2.75, 4, 2.5)}
    elements += main_screen(SH_LIT[aspect], 2, 14, 3, 10.5, lamps)
    x0, x1 = mirror_x(2.5, 13.5)
    elements.append(box([x0, 10.5, 4], [x1, 11, 6.5], "#screen"))  # abgerundete Oberkante
    for vx, ly, size in lamps.values():
        elements.append(hood(vx, ly, size))

    x0, x1 = mirror_x(8, 11.5)
    elements.append(box([x0, 3.75, 3.5], [x1, 6.75, 4], "#screen", north="#number_plate"))
    x0, x1 = mirror_x(13.5, 15)
    elements.append(box([x0, 1.5, 3.5], [x1, 7.5, 4], "#mast", north="#mastschild"))
    return model(elements)


# --- Ks-Signale (Multipart: Träger, Schirm und Zs3/Zs3v-Anzeiger als getrennte Teile) ---

KS_TYPES = {
    "ks_hauptsignal": {
        "tall": True,
        "arms": [(24, 26), (5, 7)],
        "screen": (5, 11, 16.5, 25),
        "lamps": {"red": (6.5, 21.25, 3), "green": (6.5, 17.25, 3)},
        "aspects": ["hp0", "ks1"],
        "zs3": (26, 30.5),
        "extras": lambda: [mastschild(6.5, 9.5, 7, 13.5)],
    },
    "ks_mehrabschnittssignal": {
        "tall": True,
        "arms": [(24, 26), (5, 7)],
        "screen": (3.5, 12.5, 16.5, 26),
        "lamps": {"red": (6.5, 22, 3), "green": (4.25, 17.5, 3), "yellow": (8.75, 17.5, 3)},
        "aspects": ["hp0", "ks1", "ks1_blink", "ks2"],
        "zs3": (27, 31.5),
        "zs3v": (11.5, 15.75),
        # Mastschild mit gelbem Dreieck: Hauptsignal mit Vorsignalfunktion
        "extras": lambda: [mastschild(6.5, 9.5, 2, 10.5, "#mastschild_ms")],
    },
    "ks_vorsignal": {
        "arms": [(11, 13), (6, 8)],
        "screen": (3.5, 12.5, 9.5, 14),
        "lamps": {"green": (4.75, 10.5, 2.5), "yellow": (8.75, 10.5, 2.5)},
        "aspects": ["ks1", "ks1_blink", "ks2"],
        "zs3v": (5.5, 9),
        "zs_x": (6, 10),
        "extras": lambda: [mastschild(6.25, 9.75, 0.5, 5, "#ne2")],
    },
}
LAMP_COLOR.update({"green": "green", "yellow": "yellow", "red": "red"})


def ks_screen(spec, aspect):
    vx0, vx1, y0, y1 = spec["screen"]
    elements = main_screen(KS_LIT[aspect], vx0, vx1, y0, y1, spec["lamps"])
    elements += [hood(vx, ly, size) for vx, ly, size in spec["lamps"].values()]
    return elements


def zs_box(y0, y1, vx0=6, vx1=10):
    x0, x1 = mirror_x(vx0, vx1)
    return [box([x0, y0, 4], [x1, y1, 6.5], "#zs_box", north="#zs_box")]


def zs_digit(y0, y1, vx0=6, vx1=10):
    """Leuchtende Ziffer; die Textur kommt über die Modellvariable #digit."""
    x0, x1 = mirror_x(vx0 + 0.5, vx1 - 0.5)
    element = box([x0, y0 + 0.5, 3.5], [x1, y1 - 0.5, 4], "#zs_box", north="#digit")
    element["shade"] = False
    element["neoforge_data"] = {"block_light": 15, "sky_light": 15}
    return [element]


def multipart(parts):
    entries = []
    for facing, rot in FACINGS.items():
        for when, model_name_ in parts:
            apply = {"model": f"{MOD_ID}:block/{model_name_}"}
            if rot:
                apply["y"] = rot
            entries.append({"when": {**when, "facing": facing}, "apply": apply})
    return {"multipart": entries}


def write_part(parts, name, elements, when, tall, **extra):
    """Ein Multipart-Teil; hohe Signale bekommen je Hälfte ein eigenes Modell."""
    halves = split_tall(elements) if tall else {None: elements}
    for half, half_elements in halves.items():
        if not half_elements:
            continue
        model_name_ = f"{name}_{half}" if half else name
        write_json(ASSETS / "models/block" / f"{model_name_}.json", {**model(half_elements, **extra), **(
            {"render_type": "minecraft:cutout"} if "mastschild_ms" in json.dumps(half_elements) else {})})
        parts.append(({**when, "half": half} if half else when, model_name_))


def tall_item_model(elements):
    return {**model(elements), "display": TALL_ITEM_DISPLAY}


def generate_ks():
    any_speed = "|".join(str(n) for n in SPEEDS)
    for name, spec in KS_TYPES.items():
        tall = spec.get("tall", False)
        height = 32 if tall else 16
        zx0, zx1 = spec.get("zs_x", (5.5, 10.5))
        parts = []
        for mount in MOUNTS:
            write_part(parts, f"{name}_support_{mount}", support(mount, spec["arms"], height), {"mount": mount}, tall)
        extras = spec["extras"]()
        write_part(parts, f"{name}_schild", extras, {}, tall)
        for aspect in spec["aspects"]:
            write_part(parts, f"{name}_{aspect}", ks_screen(spec, aspect), {"aspect": aspect}, tall)

        if "zs3" in spec:
            y0, y1 = spec["zs3"]
            lit_aspects = "|".join(a for a in spec["aspects"] if a != "hp0")
            write_part(parts, f"{name}_zs3", zs_box(y0, y1, zx0, zx1), {"zs3": any_speed}, tall)
            for n in SPEEDS:
                write_part(parts, f"{name}_zs3_{n}", zs_digit(y0, y1, zx0, zx1),
                           {"zs3": str(n), "aspect": lit_aspects}, tall, digit=tex(f"zs3_{n}"))
        if "zs3v" in spec:
            y0, y1 = spec["zs3v"]
            write_part(parts, f"{name}_zs3v", zs_box(y0, y1, zx0, zx1), {}, tall)
            for n in SPEEDS:
                write_part(parts, f"{name}_zs3v_{n}", zs_digit(y0, y1, zx0, zx1), {"zs3v": str(n)}, tall,
                           digit=tex(f"zs3v_{n}"))

        write_json(ASSETS / "blockstates" / f"{name}.json", multipart(parts))

        item_elements = support("none", spec["arms"], height) + extras + ks_screen(spec, "ks1")
        if "zs3v" in spec:
            item_elements += zs_box(*spec["zs3v"], zx0, zx1)
        item = tall_item_model(item_elements) if tall else model(item_elements)
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
    for mount in MOUNTS:
        for a in HP:
            for half, elements in split_tall(hauptsignal_elements(a, mount)).items():
                write_json(ASSETS / "models/block" / f"{model_name(f'hv_hauptsignal_{a}', mount)}_{half}.json",
                           model(elements))
        for a in VR:
            write_json(ASSETS / "models/block" / f"{model_name(f'hv_vorsignal_{a}', mount)}.json",
                       vorsignal_model(a, mount))
        for hp in HP:
            for vr in VR + ["dark"]:
                for half, elements in split_tall(kombisignal_elements(hp, vr, mount)).items():
                    write_json(ASSETS / "models/block" /
                               f"{model_name(f'hv_kombisignal_{hp}_{vr}', mount)}_{half}.json", model(elements))
    for a in SH_LIT:
        for on_mast in (False, True):
            write_json(ASSETS / "models/block" / f"sh_sperrsignal_{a}{'_mast' if on_mast else ''}.json",
                       sperrsignal_model(a, on_mast))
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

    write_json(ASSETS / "models/item/hv_hauptsignal.json", tall_item_model(hauptsignal_elements("hp1", "none")))
    write_json(ASSETS / "models/item/hv_kombisignal.json", tall_item_model(kombisignal_elements("hp1", "vr1", "none")))
    items = {"hv_vorsignal": "hv_vorsignal_vr1", "sh_sperrsignal": "sh_sperrsignal_sh0", "signal_mast": "signal_mast"}
    for item, parent in items.items():
        write_json(ASSETS / "models/item" / f"{item}.json", {"parent": f"{MOD_ID}:block/{parent}"})


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


# --- Texturen (16x16 Platzhalter-Pixelart) ---

def save(img, name):
    path = ASSETS / "textures/block" / f"{name}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)


DIGITS = {
    1: ["010", "110", "010", "010", "111"], 2: ["111", "001", "111", "100", "111"],
    3: ["111", "001", "011", "001", "111"], 4: ["101", "101", "111", "001", "001"],
    5: ["111", "100", "111", "001", "111"], 6: ["111", "100", "111", "101", "111"],
    7: ["111", "001", "010", "010", "010"], 8: ["111", "101", "111", "101", "111"],
    9: ["111", "101", "111", "001", "111"],
}


def digit_img(n, color):
    """Ziffer eines Lichtanzeigers (3x5-Raster, dreifach skaliert) auf dunklem Grund."""
    img = Image.new("RGBA", (16, 16), (18, 18, 20, 255))
    for row, bits in enumerate(DIGITS[n]):
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

    zs = Image.new("RGBA", (16, 16), (18, 18, 20, 255))
    ImageDraw.Draw(zs).rectangle([0, 0, 15, 15], outline=(50, 50, 54, 255))
    save(zs, "zs_box")
    for n in SPEEDS:
        save(digit_img(n, (240, 240, 240, 255)), f"zs3_{n}")
        save(digit_img(n, (255, 200, 40, 255)), f"zs3v_{n}")

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
    generate_models()
    generate_ks()
    generate_data()
    generate_textures()
    print("Assets erzeugt unter", ROOT)
