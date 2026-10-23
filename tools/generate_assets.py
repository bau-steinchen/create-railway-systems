#!/usr/bin/env python3
"""Erzeugt Blockstates, Modelle, Platzhalter-Texturen, Loot-Tables und Tags der H/V-Signale.

Aufruf aus dem Projektverzeichnis:  python3 tools/generate_assets.py
Benötigt Pillow (pip install pillow) für die Texturen.
"""
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
VR = ["vr0", "vr1", "vr2"]

# Welche Lampen leuchten bei welchem Begriff
HP_LIT = {"hp0": {"red"}, "hp1": {"green"}, "hp2": {"green", "yellow"}}
VR_LIT = {"vr0": {"y_bl", "y_tr"}, "vr1": {"g_bl", "g_tr"}, "vr2": {"g_bl", "y_tr"}, "dark": set()}
SH_LIT = {"sh0": {"r_l", "r_r"}, "sh1": {"w_bl", "w_tr"}}
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
        faces["north"] = {"texture": north}
    return {"from": frm, "to": to, "faces": faces}


def lamp(x0, y0, size, color, lit):
    element = box([x0, y0, 3.5], [x0 + size, y0 + size, 4], "#screen",
                  north=f"#lamp_{color}" if lit else "#lamp_off")
    element["faces"]["north"]["uv"] = [0, 0, 16, 16]
    if lit:
        element["shade"] = False
        element["neoforge_data"] = {"block_light": 15, "sky_light": 15}
    return element


def post():
    # deckungsgleich mit dem Signalmast-Block, damit kein Versatz am Übergang entsteht
    return box([6.5, 0, 6.5], [9.5, 16, 9.5], "#mast")


def support(mount, arm_heights):
    """Senkrechter Mast (frei stehend) oder waagerechte Ausleger zum Nachbarblock (Wandmontage).
    Modellraum: Schirm zeigt nach Norden; "links"/"rechts" aus Sicht des Betrachters vor dem Schirm."""
    if mount == "none":
        return [post()]
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
}


def model(elements):
    return {"parent": "block/block", "textures": TEXTURES, "elements": elements}


def main_screen(lit, x0, x1, y0, y1, lamps):
    elements = [box([x0, y0, 4], [x1, y1, 6.5], "#screen")]
    for name, (lx, ly, size) in lamps.items():
        elements.append(lamp(lx, ly, size, LAMP_COLOR[name], name in lit))
    return elements


def hauptsignal_model(aspect, mount):
    elements = support(mount, [(13, 15), (1.5, 3.5)])
    elements.append(box([6, 0.5, 5.5], [10, 4.5, 6.5], "#mastschild", north="#mastschild"))
    elements += main_screen(HP_LIT[aspect], 4.5, 11.5, 5, 16, {
        "green": (6.5, 12.5, 3), "red": (6.5, 9, 3), "yellow": (6.5, 5.5, 3)})
    return model(elements)


def vorsignal_model(aspect, mount):
    elements = support(mount, [(7, 9)])
    elements += main_screen(VR_LIT[aspect], 3, 13, 4, 12, {
        "y_bl": (3.5, 5, 2.5), "g_bl": (6.5, 5, 2.5), "g_tr": (7, 8.5, 2.5), "y_tr": (10, 8.5, 2.5)})
    return model(elements)


def kombisignal_model(hp, vr, mount):
    elements = support(mount, [(12, 14), (3, 5)])
    elements += main_screen(HP_LIT[hp], 5, 11, 8, 16, {
        "green": (7, 13.5, 2), "red": (7, 11, 2), "yellow": (7, 8.5, 2)})
    elements += main_screen(VR_LIT[vr], 4, 12.5, 1, 7, {
        "y_bl": (4.5, 1.5, 2), "g_bl": (7, 1.5, 2), "g_tr": (7.5, 4.5, 2), "y_tr": (10, 4.5, 2)})
    return model(elements)


def sperrsignal_model(aspect, on_mast):
    # Maststummel deckungsgleich mit dem Signalmast; frei stehend zusätzlich eine Fußplatte
    elements = [box([6.5, 0, 6.5], [9.5, 3, 9.5], "#mast")]
    if not on_mast:
        elements.append(box([4, 0, 4], [12, 1, 12], "#mast"))
    elements += main_screen(SH_LIT[aspect], 4, 12, 3, 11, {
        "w_tr": (9.5, 8.5, 2), "r_l": (4.5, 6, 2), "r_r": (9.5, 6, 2), "w_bl": (4.5, 3.5, 2)})
    return model(elements)


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
            write_json(ASSETS / "models/block" / f"{model_name(f'hv_hauptsignal_{a}', mount)}.json",
                       hauptsignal_model(a, mount))
        for a in VR:
            write_json(ASSETS / "models/block" / f"{model_name(f'hv_vorsignal_{a}', mount)}.json",
                       vorsignal_model(a, mount))
        for hp in HP:
            for vr in VR + ["dark"]:
                write_json(ASSETS / "models/block" / f"{model_name(f'hv_kombisignal_{hp}_{vr}', mount)}.json",
                           kombisignal_model(hp, vr, mount))
    for a in SH_LIT:
        for on_mast in (False, True):
            write_json(ASSETS / "models/block" / f"sh_sperrsignal_{a}{'_mast' if on_mast else ''}.json",
                       sperrsignal_model(a, on_mast))
    write_json(ASSETS / "models/block/signal_mast.json",
               {"parent": "block/block", "textures": {"particle": tex("signal_mast"), "mast": tex("signal_mast")},
                "elements": [box([6.5, 0, 6.5], [9.5, 16, 9.5], "#mast")]})

    write_json(ASSETS / "blockstates/hv_hauptsignal.json",
               variants([("aspect", HP), ("mount", MOUNTS)],
                        lambda s: model_name(f"hv_hauptsignal_{s['aspect']}", s["mount"])))
    write_json(ASSETS / "blockstates/hv_vorsignal.json",
               variants([("aspect", VR), ("mount", MOUNTS)],
                        lambda s: model_name(f"hv_vorsignal_{s['aspect']}", s["mount"])))
    write_json(ASSETS / "blockstates/hv_kombisignal.json",
               variants([("aspect", HP), ("distant", VR + ["dark"]), ("mount", MOUNTS)],
                        lambda s: model_name(f"hv_kombisignal_{s['aspect']}_{s['distant']}", s["mount"])))
    write_json(ASSETS / "blockstates/sh_sperrsignal.json",
               variants([("aspect", list(SH_LIT)), ("mast", ["false", "true"])],
                        lambda s: f"sh_sperrsignal_{s['aspect']}{'_mast' if s['mast'] == 'true' else ''}"))
    write_json(ASSETS / "blockstates/signal_mast.json",
               {"variants": {"": {"model": f"{MOD_ID}:block/signal_mast"}}})

    items = {"hv_hauptsignal": "hv_hauptsignal_hp1", "hv_vorsignal": "hv_vorsignal_vr1",
             "hv_kombisignal": "hv_kombisignal_hp1_vr1", "sh_sperrsignal": "sh_sperrsignal_sh0",
             "signal_mast": "signal_mast"}
    for item, parent in items.items():
        write_json(ASSETS / "models/item" / f"{item}.json", {"parent": f"{MOD_ID}:block/{parent}"})


def generate_data():
    blocks = ["hv_hauptsignal", "hv_vorsignal", "hv_kombisignal", "sh_sperrsignal", "signal_mast"]
    for b in blocks:
        write_json(DATA / MOD_ID / "loot_table/blocks" / f"{b}.json", {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{MOD_ID}:{b}"}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}],
        })
    write_json(DATA / "minecraft/tags/block/mineable/pickaxe.json",
               {"values": [f"{MOD_ID}:{b}" for b in blocks]})


# --- Texturen (16x16 Platzhalter-Pixelart) ---

def save(img, name):
    path = ASSETS / "textures/block" / f"{name}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)


def generate_textures():
    screen = Image.new("RGBA", (16, 16), (21, 21, 23, 255))
    ImageDraw.Draw(screen).rectangle([0, 0, 15, 15], outline=(44, 44, 48, 255))
    save(screen, "signal_screen")

    mast = Image.new("RGBA", (16, 16))
    for x in range(16):
        shade = 120 + int(30 * (1 - abs(x - 6) / 8))
        for y in range(16):
            mast.putpixel((x, y), (shade, shade + 4, shade + 8, 255))
    save(mast, "signal_mast")

    schild = Image.new("RGBA", (16, 16), (240, 240, 240, 255))
    d = ImageDraw.Draw(schild)
    d.rectangle([0, 5, 15, 10], fill=(200, 20, 30, 255))
    d.rectangle([0, 0, 15, 15], outline=(20, 20, 20, 255))
    save(schild, "mastschild_hp")

    colors = {"red": (255, 40, 30), "green": (40, 255, 110), "yellow": (255, 200, 40), "white": (225, 230, 255)}

    def lamp_img(core, ring, highlight):
        img = Image.new("RGBA", (16, 16), (21, 21, 23, 255))
        d = ImageDraw.Draw(img)
        d.ellipse([0, 0, 15, 15], fill=ring)
        d.ellipse([2, 2, 13, 13], fill=core)
        if highlight:
            d.ellipse([4, 4, 8, 8], fill=highlight)
        return img

    save(lamp_img((48, 48, 52, 255), (30, 30, 33, 255), None), "lamp_off")
    for name, (r, g, b) in colors.items():
        save(lamp_img((r, g, b, 255), (r // 2, g // 2, b // 2, 255),
                      (min(255, r + 120), min(255, g + 120), min(255, b + 120), 255)), f"lamp_{name}")


if __name__ == "__main__":
    generate_models()
    generate_data()
    generate_textures()
    print("Assets erzeugt unter", ROOT)
