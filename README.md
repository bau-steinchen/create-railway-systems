# Create: Railway Systems

Ein Addon für [Create](https://github.com/Creators-of-Create/Create), das moderne Bahnsysteme in Minecraft bringt. Den Anfang machen **deutsche Lichtsignale** (H/V, Ks und Sh), die den Zustand der Create-Zugsignale vorbildgerecht anzeigen.

## Voraussetzungen

| | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.219 oder neuer |
| Create | 6.0.10 oder neuer |

## Installation

1. NeoForge für Minecraft 1.21.1 installieren.
2. Create (NeoForge 1.21.1) in den `mods`-Ordner legen.
3. `create_railway_systems-<version>.jar` in den `mods`-Ordner legen.

## Inhalt (v0.1.0)

### Deutsche H/V-Lichtsignale

| Block | Signalbegriffe |
|---|---|
| H/V-Lichthauptsignal | Hp0 (Halt), Hp1 (Fahrt), Hp2 (Langsamfahrt) |
| H/V-Lichtvorsignal | Vr0 (Halt erwarten), Vr1 (Fahrt erwarten), Vr2 (Langsamfahrt erwarten) |
| H/V-Hauptsignal mit Vorsignal am Mast | Hp + Vr, Vorsignal dunkel bei Hp0 |
| Lichtsperrsignal (Zwergsignal) | Sh0 (Halt! Fahrverbot), Sh1 (Fahrverbot aufgehoben) |
| Signalmast | Mast zum Aufbauen beliebig hoher Signale |

### Deutsche Ks-Signale

| Block | Signalbegriffe |
|---|---|
| Ks-Hauptsignal | Hp0 (Halt), Ks1 (Fahrt), optional mit Zs3 |
| Ks-Vorsignal | Ks1, Ks1 blinkend mit Zs3v, Ks2 (Halt erwarten), mit Ne 2-Tafel |
| Ks-Mehrabschnittssignal | Hp0, Ks1, Ks1 blinkend mit Zs3v, Ks2, optional mit Zs3 |

Alle Blöcke stehen im Kreativ-Tab **Create: Railway Systems**.

### Benutzung

1. Ein Create-Signal wie gewohnt an die Strecke setzen.
2. Mit dem Signal in der Hand **Rechtsklick auf das Create-Signal**. Das Signal ist jetzt gebunden, das Create-Signal wird hervorgehoben.
3. Das Signal platzieren. Es darf höchstens 32 Blöcke vom Create-Signal entfernt stehen (Server-Config `signalLinkRange`).
4. **Schleichen + Rechtsklick** löscht eine Bindung, bevor das Signal platziert ist.

Ohne Bindung lässt sich ein Signal nicht platzieren.

### Aufstellung

- **Frei stehend:** Das Signal steht auf einem Mast. Mit Signalmast-Blöcken darunter wird es beliebig hoch.
- **Wandmontage:** Gegen die Seite eines festen Blocks gesetzt (z. B. Tunnel- oder Brückenwand), bekommt das Signal waagerechte Ausleger statt eines Masts. Die Ausleger zeigen nach hinten oder zur Seite, je nachdem wo der Block liegt. Wird der Block entfernt, steht das Signal wieder auf einem Mast.
- **Lichtsperrsignal:** Auf dem Boden oder einem festen Block steht es auf einem kleinen Fuß. Steht darunter ein Signalmast, sitzt es ohne Fuß direkt auf dem Mast.
- Der Schirm zeigt beim Platzieren immer zum Spieler.

### Signallogik

- **Hauptsignal:** Create-Signal rot → Hp0, grün/gelb → Hp1. Create kennt keine Geschwindigkeiten. Mit dem **Create-Schraubenschlüssel** schaltest du deshalb zwischen Hp1 und Hp2 um.
- **Vorsignal:** Wird an das Create-Signal des **angekündigten** Hauptsignals gebunden und steht im Bremswegabstand davor. Ist das H/V-Hauptsignal dort auf Hp2 eingestellt, zeigt es Vr2.
- **Lichtsperrsignal:** Create-Signal rot → Sh0, grün/gelb → Sh1.
- **Ks-Hauptsignal:** Create-Signal rot → Hp0, grün/gelb → Ks1. Mit dem **Schraubenschlüssel** wird der Geschwindigkeitsanzeiger **Zs3** eingestellt (aus, 10–90 km/h). Er leuchtet, solange das Signal Fahrt zeigt.
- **Ks-Vorsignal:** Wird wie das H/V-Vorsignal an das Create-Signal des angekündigten Hauptsignals gebunden. Hält das Hauptsignal → Ks2. Gibt es dort eine Geschwindigkeit vor (Zs3 oder H/V-Hp2) → Ks1 blinkt und der **Zs3v** zeigt sie an. Sonst Ks1.
- **Ks-Mehrabschnittssignal:** Eigenes Create-Signal rot → Hp0. Sonst kündigt es wie das Kombisignal automatisch die Folgesignale an: Ks2, Ks1 blinkend mit Zs3v oder Ks1. Zusätzlich lässt sich ein eigener Zs3 einstellen.
- **Systemübergreifend:** H/V- und Ks-Signale kündigen sich gegenseitig an. Ein H/V-Hp2 gilt als 40 km/h, ein Ks-Zs3 lässt H/V-Vorsignale Vr2 zeigen.
- **Hauptsignal mit Vorsignal am Mast:** Der Vorsignalteil kündigt automatisch die Signale am Ende des Blockabschnitts an. Gibt es durch Weichen mehrere Folgesignale, erwartet es nur Fahrt, wenn alle Fahrt zeigen, sonst Vr0. Zeigt das eigene Hauptsignal Hp0, ist das Vorsignal dunkel.

## Entwicklung

Benötigt JDK 21.

```sh
./gradlew build                    # Jar nach build/libs/
./gradlew runClient                # Testclient starten
./gradlew runServer                # Testserver starten
python3 tools/generate_assets.py   # Modelle, Blockstates, Texturen, Loot-Tables neu erzeugen (Pillow nötig)
```

Blockstates, Modelle und Platzhalter-Texturen werden von `tools/generate_assets.py` erzeugt. Änderungen an der Geometrie gehören dort hinein, nicht in die erzeugten JSON-Dateien.

### Struktur

```
src/main/java/de/bausteinchen/railwaysystems/
  RailwaySystems.java, Config.java     Einstieg und Server-Config
  registry/                            Blöcke, Items, Block-Entities, Kreativ-Tab
  signal/                              Gemeinsame Basis: Bindung, Block-Entity, Montage, Mast
  signal/hv/                           H/V- und Sh-Signale
  signal/ks/                           Ks-Signale
tools/generate_assets.py               Asset-Generator
```

## Geplant

- Zusatzanzeiger (Zs3, Zs3v, Zs1, Zs7 …)
- Bessere Texturen

## Lizenz

MIT
