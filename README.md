# Create: Railway Systems

**English** | [Deutsch](README.de.md)

An addon for [Create](https://github.com/Creators-of-Create/Create) that brings modern railway systems to Minecraft. It starts with **German light signals** (H/V, Ks and Sh) and their supplementary signals (Zs1, Zs2, Zs2v, Zs3, Zs3v, Zs6, Zs7, Zs8), which display the state of Create's train signals the way the real ones do, plus **traffic lights** with a configurable arm and a programmable controller.

## Requirements

| | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.215 or newer |
| Create | 6.0.9 or newer |

## Installation

1. Install NeoForge for Minecraft 1.21.1.
2. Put Create (NeoForge 1.21.1) into the `mods` folder.
3. Put `create_railway_systems-<version>.jar` into the `mods` folder.

## Contents (v0.1.0)

### German H/V and Sh light signals

| Block | Aspects |
|---|---|
| H/V light main signal | Hp0 (stop), Hp1 (proceed), Hp2 (proceed at reduced speed) |
| H/V light distant signal | Vr0 (expect stop), Vr1 (expect proceed), Vr2 (expect reduced speed) |
| H/V main signal with distant signal on the mast | Hp + Vr, distant signal dark when Hp0 |
| Light shunting signal (dwarf signal) | Sh0 (stop, movement prohibited), Sh1 (movement permitted) |
| Signal mast | Standard mast: two plates with rungs in between |
| Light signal mast | Slim mast: a single pole |
| Lattice signal mast | Wide square lattice mast: corner posts with diagonal braces |

### German H/V semaphore signals

| Block | Aspects |
|---|---|
| Semaphore main signal (one arm) | Hp0 (arm horizontal), Hp1 (arm raised 45°) |
| Semaphore main signal (two arms) | Hp0 (upper arm horizontal, lower arm hanging along the mast), Hp1 and Hp2 (both arms raised 45°; told apart at night by the lantern) |

The arms swing smoothly into their new position and reach beyond the block; the hitbox stays within the signal's blocks. At night a lantern shows red (Hp0), green (Hp1) or green above yellow (Hp2). Semaphore signals are three blocks tall and have no supplementary signals yet.

### German Ks signals

| Block | Aspects |
|---|---|
| Ks main signal | Hp0 (stop), Ks1 (proceed), optionally with Zs3 |
| Ks distant signal | Ks1, flashing Ks1 with Zs3v, Ks2 (expect stop), with Ne 2 board |
| Ks multi-section signal | Hp0, Ks1, flashing Ks1 with Zs3v, Ks2, optionally with Zs3 |

### Supplementary signals on main signals

| Signal | Appearance | When it is lit |
|---|---|---|
| Zs1 substitute signal | H/V: three white lights in an A shape, Ks: flashing white light | Signal shows stop and receives redstone |
| Zs7 caution signal | three yellow lights in a V shape | Signal shows stop and receives redstone |
| Zs8 opposite track substitute signal | diagonal row of white lights, flashing (in the Zs6 housing) | Signal shows stop and receives redstone |
| Zs2 route indicator | white letter A–Z | Signal shows proceed and receives redstone |
| Zs2v route pre-indicator | yellow letter on the distant signal | Announced main signal shows its Zs2 |
| Zs6 opposite track indicator | diagonal row of white lights | Signal shows proceed |
| Identification light (Kennlicht) | white light in the top left of the signal head | Signal is switched off, all other lights are dark |

Zs1/Zs7/Zs8 are purely visual: Create trains still won't pass a signal showing stop. A signal can carry both a substitute signal and a Zs2: the same redstone signal lights the substitute signal at stop and the Zs2 at proceed.

Zs2v is available on the H/V distant signal, the H/V combined signal, the Ks distant signal and the Ks multi-section signal.

All blocks are in the **Create: Railway Systems** creative tab.

### Usage

1. Place a Create signal on the track as usual.
2. With the signal in hand, **right-click the Create signal**. The signal is now linked and the Create signal is highlighted.
3. Place the signal. It may be at most 32 blocks away from the Create signal (server config `signalLinkRange`).
4. **Sneak + right-click** clears a link before the signal is placed.

A signal can't be placed without a link.

### Placement

- **Free-standing:** The signal stands on a mast. Put signal mast blocks underneath to make it as tall as you like.
- **On a wall:** Placed against a solid block behind it (e.g. a tunnel or bridge wall), the signal head moves to the back edge of the block and sits flat on the wall, without a mast or brackets.
- **On a signal mast:** Placed against any of the three signal mast blocks, the head also sits flush at the edge of its own block, without a mast of its own, and two short holder bars reach to the mast surface. Two-block-tall signals need a mast block behind both halves. This way one mast column can carry signals on several sides and at any height. Masts rotate in 16 steps of 22.5°: when placed they face the player's view direction, and each wrench click turns them one step. A signal on a mast turns with it around the mast centre, towards the nearest side of the mast; signals elsewhere keep their usual orientation. Masts from worlds made before this version reset to their default orientation.
- **Side brackets:** Placed against a solid block to the left or right, the signal keeps its centred position and gets a horizontal bracket to that block.
- If the supporting block is removed, the signal stands on its own mast again; if a mast is replaced by a solid block, it sits flat on the wall.
- **Main signals are two blocks tall:** H/V main signal, H/V main signal with distant signal on the mast, Ks main signal and Ks multi-section signal. The block above the placement position must be free. Breaking one half removes the other as well. Linking, wrench and pick-block work on both halves. Semaphore main signals are three blocks tall and need two free blocks above. Distant signals and the shunting signal are one block tall.
- **Shunting signal:** On the ground or on a solid block it stands on a small base. With a signal mast below, it sits directly on the mast without a base.
- The signal head always faces the player when placed.

### Configuring equipment

H/V main signals, H/V combined signals, Ks main signals and Ks multi-section signals have four Create value boxes on the **sides of the lower half**, two on each side, so they stay reachable when the signal sits on a wall. Aim at a box with the **wrench**, hold right-click and choose:

| | right side | left side |
|---|---|---|
| **top** | Substitute signal: none, Zs1, Zs7 or Zs8 | Zs2 letter: none or A–Z |
| **bottom** | Opposite track indicator: none or Zs6 | Operation: in service or switched off (identification light) |

Left and right are as seen from the front. Right-clicking with the wrench outside the boxes toggles Hp2 (H/V) or sets Zs3 (Ks) as before. Redstone for Zs1/Zs7/Zs8 and Zs2 can be applied to the upper or lower half, e.g. a lever, redstone block or redstone dust right next to the signal.

**Built-in wireless link:** The two-block-tall signals have two frequency slots on the left side of the **upper half** (seen from the front), just like a Create Redstone Link. Right-click a slot with an item in hand to set the frequency. A Redstone Link transmitting on the same frequency acts like redstone applied to the signal. No extra Redstone Link block is needed at the signal.

### Signal logic

- **H/V main signal:** Create signal red → Hp0, green/yellow → Hp1. Create has no concept of speeds, so the **Create wrench** toggles between Hp1 and Hp2.
- **Semaphore main signal:** Same logic as the H/V main signal. Only the two-arm version can show Hp2 (preselected with the wrench); on the one-arm version the wrench rotates the signal.
- **H/V distant signal:** Linked to the Create signal of the **announced** main signal and placed at braking distance in front of it. If that main signal imposes a speed (Hp2 or Ks Zs3), it shows Vr2.
- **H/V main signal with distant signal on the mast:** The main signal part works like the H/V main signal. The distant signal part automatically announces the signals at the end of the block section. If switches lead to several following signals, it only expects proceed when all of them show proceed, otherwise Vr0. When its own main signal shows Hp0, the distant signal is dark.
- **Shunting signal:** Create signal red → Sh0, green/yellow → Sh1.
- **Ks main signal:** Create signal red → Hp0, green/yellow → Ks1. The **wrench** sets the speed indicator **Zs3** (off, 10–90 km/h). It is lit while the signal shows proceed.
- **Ks distant signal:** Linked like the H/V distant signal to the Create signal of the announced main signal. Main signal at stop → Ks2. Main signal imposes a speed (Zs3 or H/V Hp2) → Ks1 flashes and the **Zs3v** shows it. Otherwise Ks1.
- **Ks multi-section signal:** Own Create signal red → Hp0. Otherwise, like the H/V combined signal, it automatically announces the following signals: Ks2, flashing Ks1 with Zs3v, or Ks1. Its own Zs3 can be set as well.
- **Zs2v:** Distant signals announce the letter the next main signal's Zs2 is currently showing. If several following signals show different letters, the Zs2v stays dark.
- **Cross-system:** H/V and Ks signals announce each other. An H/V Hp2 counts as 40 km/h, and a Ks Zs3 makes H/V distant signals show Vr2. With several following signals, the lowest speed is announced.

### Traffic lights

| Block / item | Description |
|---|---|
| Traffic light mast with arm | Mast with a curved bend and an arm over the road, vehicle signal head (red, yellow, green) at the end of the arm and optionally on the pole. Height and reach are configurable |
| Crosswalk light mast | Same mast with a crosswalk light over the road (sign 350 on both sides, lamp underneath, as used in Hamburg) and a flat sign 350 on the pole. Height, reach and sign height are configurable; the light is switched by a built-in redstone link receiver |
| Pedestrian light | Red/green with pedestrian symbol. Put signal masts underneath to raise it |
| Traffic light controller | Cabinet that switches the linked traffic lights according to a signal timing plan |
| Traffic light programmer | Tool for linking traffic lights to a controller |

**Placing a traffic light mast**

1. Hold the traffic light mast and **click into the air**. In the menu, set the pole height (3–12), reach (2–16), the side of the arm and the signal head on the pole. A sketch shows the shape and the clearance.
2. Aim at a block: an outline shows pole, arm and signal heads (red if there is no room for the pole). **Right-click** places the mast with this shape. The signal heads face the player, the arm points to the player's left or right.
3. To change it later, use the **wrench** on the mast to open the same menu.

The vertical pole is made of blocks (collision, clickable). The bend, arm and signal heads above it are only rendered and have no collision. Breaking any part of the pole removes the whole mast.

**Crosswalk light mast:** Placed and adjusted like the traffic light mast; instead of the signal head on the pole, the menu sets the height of the sign on the pole. The foot of the mast holds a redstone link receiver with its two frequency slots on the back. While a redstone signal arrives on that frequency, the crosswalk light shines.

**Stopping trains:** Like the railway signals, a traffic light can be linked to a Create signal by right-clicking it before placing, e.g. on a phantom track used for road vehicles. While the light shows red, red-yellow or yellow it holds that Create signal at stop and trains wait in front of it. Without a link the light only displays its aspect.

**Linking traffic lights**

1. Right-click the controller with the traffic light programmer.
2. Right-click the traffic lights one after another (any part of a mast works). Each light goes into the first group of its kind (vehicle or pedestrian); if there is none yet, one is created. Clicking a linked light again unlinks it.
3. Click into the air to finish.

While a controller is selected, it and all linked lights are outlined, colored by signal group.

**Signal timing plan** (right-click the controller)

- **Timing:** cycle time, yellow time, red-yellow time and intergreen time. Each signal group has a name, a kind (vehicle/pedestrian) and a green window ("green … until", in seconds). A window may wrap around the end of the cycle. The controller adds yellow after green and red-yellow before it for vehicle groups; pedestrian lights go straight from green to red. The timeline shows the sequence: left-click sets the green start, right-click the green end. Shift-click on −/+ changes by 5.
- **Conflicts:** matrix of conflicting groups that must never be green at the same time. If their green windows overlap or are closer than the intergreen time, the menu shows a warning.
- **Lights:** all linked lights with their position. Click to change the group (shift-click goes backwards), "Unlink" removes the light.

Changes apply when you click **Save**. **Redstone** on the controller switches to night mode: vehicle lights flash yellow, pedestrian lights are off, linked Create signals stay open.

Example intersection, 60 s cycle: K1 (main road) green 0–25, K2 (side road) green 30–52, F1 (pedestrians parallel to K1) green 0–20. Mark K1/K2 and K2/F1 as conflicting in the matrix.

### Level crossings

| Block | Description |
|---|---|
| St. Andrew's cross | Sign 201 on a short post |
| Level crossing light (yellow/red) | German "Lichtzeichen": when switched on it shows yellow for 3 s, then steady red |
| Level crossing flashing light | Single red light that flashes while switched on |

The lights are switched on while a redstone signal reaches the block or while their built-in redstone link receiver (two frequency slots on the back of the post) receives a signal. The post runs through the whole block, so a St. Andrew's cross can sit on top and signal masts can go underneath.

**Train detection for now:** place Create track observers a few dozen blocks before the crossing (one per direction) and send their signal to the lights over a redstone link, e.g. through a latch that a second observer behind the crossing resets. Automatic train detection, barriers and a crossing controller are planned.

## Development

Requires JDK 21 and Python 3.10+ with Pillow (`pip install pillow`).

```sh
./gradlew build                                # jar to build/libs/ (Windows: .\gradlew.bat build)
./gradlew runClient                            # start test client
./gradlew runClient -PquickPlay=<world>        # start test client directly into a singleplayer world
./gradlew runServer                            # start test server
python tools/generate_assets.py                # regenerate models, blockstates, textures, loot tables
python tools/generate_assets.py --vorlagen     # export the built-in signals as Blockbench templates
```

Blockstates, models and placeholder textures are generated by `tools/generate_assets.py`. Geometry changes belong there (or in a Blockbench model, see below), not in the generated JSON files. The generator deletes models and blockstates it no longer produces.

### Custom models with Blockbench

A signal head can be designed in [Blockbench](https://www.blockbench.net/). The script generates all variants from it: aspects, mast or wall brackets, the lower/upper half for tall signals, and the overlay indicators at the marked positions.

1. Run `python tools/generate_assets.py --vorlagen`. The built-in signals are written to `tools/models/vorlagen/<block>.json` (not in Git, they contain local texture paths).
2. Open the template in the Blockbench desktop app (*File → Open Model*), project type **Java Block/Item**. On Windows the textures are loaded along with it.
3. Edit the model and save it with *File → Export → Export Block/Item Model* as `tools/models/<block>.json`, e.g. `tools/models/ks_hauptsignal.json`. The file name must be exactly the block name.
4. Run `python tools/generate_assets.py` and start the game.

Without a file in `tools/models/`, the built-in geometry is used.

The signal mast blocks can be replaced the same way with `tools/models/signal_mast.json`, `signal_mast_light.json` or `signal_mast_lattice.json` (no lamps or markers; every element is drawn). Signals without their own `mast` then use the standard mast too, repeated every 16 px and cut to height. Where a screen or sign intersects the mast, the mast is left out at that height. The Mastschild and the overlay boxes move in front of the mast. Wall brackets keep their built-in shape.

**The names of elements (or their group) control their function:**

| Name | Meaning |
|---|---|
| `lamp_<name>` | Lamp. Its **north face** shows the matching lamp texture, on or off, depending on the aspect. The script sets up the glow. |
| `mast` | Only visible when the signal is free-standing. Without `mast`, the built-in mast is used. |
| `arm` | Marker only: height (y from–to) of the holder bars to a signal mast and of the side brackets, not rendered itself. |
| `sockel` | Shunting signal only: base when it is not on a signal mast. On a mast it is replaced by a mast stub up to the top of the base. |
| `kennlicht`, `zs1`, `zs7` | Marker only: position of the lamps of these overlay indicators. For `zs1`/`zs7`, one element per lamp. |
| `zs2`, `zs2v`, `zs3`, `zs3v`, `zs6` | Marker only: box of the indicator. The script inserts the housing and symbol in front of it; the Zs8 uses the Zs6 box. |
| anything else | Solid signal body (screen, hoods, mast plate …) |

A trailing number is allowed (`arm2`, `lamp_red2`, `zs12`), but `mastschild` does not count as `mast`. If a marker is missing, the indicator sits where it is in the built-in model; the script will point this out. A signal's template shows which indicators it has.

**Lamp names per block:**

| Block | Lamps |
|---|---|
| `hv_hauptsignal` | `lamp_red`, `lamp_green`, `lamp_yellow` |
| `hv_vorsignal` | `lamp_y_bl`, `lamp_y_tr`, `lamp_g_bl`, `lamp_g_tr` (yellow/green, bottom left/top right) |
| `hv_kombisignal` | both sets above |
| `sh_sperrsignal` | `lamp_r_l`, `lamp_r_r`, `lamp_w_bl`, `lamp_w_tr` |
| `ks_hauptsignal` | `lamp_red`, `lamp_green` |
| `ks_vorsignal` | `lamp_green`, `lamp_yellow` |
| `ks_mehrabschnittssignal` | `lamp_red`, `lamp_green`, `lamp_yellow` |

**Rules:**

- The screen faces **north**. The blockstate rotates it for the other directions.
- The back of the signal head is at z = 6.5, as in the templates. On a wall or signal mast the whole head, overlays included, moves 9.5 px back to the block edge.
- Two-block-tall signals (H/V main, H/V combined, Ks main and Ks multi-section signal) are built at 0–32 px height, the others at 0–16 px. The script splits tall models at y = 16. Rotated elements are not cut but end up entirely in the half containing their center.
- All coordinates lie between −16 and 32. Rotation is only allowed around **one** axis in steps of 22.5° (−45 to 45). Otherwise the script aborts with a message.
- Textures go in `src/main/resources/assets/create_railway_systems/textures/block/` or in `tools/models/textures/`. The script copies them from the latter; this also replaces placeholders such as `lamp_red.png`. Other texture paths in the export are reduced to the file name.
- Texture variables with built-in names (`mast`, `screen`, `lamp_red` …) also apply to the generated parts. If you point `mast` to your own texture, the wall brackets get it too.
- Models from Blockbench are written with `render_type: cutout`, so transparent pixels work. Translucency does not.
- The `display` settings from Blockbench apply to the item.

### Structure

```
src/main/java/de/bausteinchen/railwaysystems/
  RailwaySystems.java, Config.java     Entry point and server config
  registry/                            Blocks, items, block entities, creative tab
  signal/                              Shared base: linking, block entity, mounting, mast
  signal/hv/                           H/V and Sh signals
  signal/ks/                           Ks signals
  traffic/                             Traffic lights: mast, pedestrian light, controller, signal timing plan
  crossing/                            Level crossings: St. Andrew's cross, crossing lights
  network/                             Menu packets (client -> server)
  client/                              Overlay renderer for indicators, traffic light renderer, menus, outlines
tools/generate_assets.py               Asset generator
tools/models/                          Custom Blockbench models and textures (optional)
```

## Planned

- Better textures
- Level crossings: barriers, controller with automatic train detection, signal interlocking, bell

## License

MIT
