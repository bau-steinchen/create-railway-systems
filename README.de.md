# Create: Railway Systems

[English](README.md) | **Deutsch**

Ein Addon für [Create](https://github.com/Creators-of-Create/Create), das moderne Bahnsysteme in Minecraft bringt. Den Anfang machen **deutsche Lichtsignale** (H/V, Ks und Sh) mit Zusatzsignalen (Zs1, Zs2, Zs2v, Zs3, Zs3v, Zs6, Zs7, Zs8), die den Zustand der Create-Zugsignale vorbildgerecht anzeigen.

## Voraussetzungen

| | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.215 oder neuer |
| Create | 6.0.9 oder neuer |

## Installation

1. NeoForge für Minecraft 1.21.1 installieren.
2. Create (NeoForge 1.21.1) in den `mods`-Ordner legen.
3. `create_railway_systems-<version>.jar` in den `mods`-Ordner legen.

## Inhalt (v0.1.0)

### Deutsche H/V- und Sh-Lichtsignale

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

### Zusatzsignale an Hauptsignalen

| Zusatzsignal | Aussehen | Wann es leuchtet |
|---|---|---|
| Zs1 Ersatzsignal | H/V: drei weiße Lichter in A-Form, Ks: weißes Blinklicht | Signal zeigt Halt und bekommt Redstone |
| Zs7 Vorsichtsignal | drei gelbe Lichter in V-Form | Signal zeigt Halt und bekommt Redstone |
| Zs8 Gegengleis-Ersatzsignal | weiße schräge Lichtpunktreihe, blinkend (im Kasten des Zs6) | Signal zeigt Halt und bekommt Redstone |
| Zs2 Richtungsanzeiger | weißer Buchstabe A–Z | Signal zeigt Fahrt und bekommt Redstone |
| Zs2v Richtungsvoranzeiger | gelber Buchstabe am Vorsignal | angekündigtes Hauptsignal zeigt seinen Zs2 |
| Zs6 Gegengleisanzeiger | weiße schräge Lichtpunktreihe | Signal zeigt Fahrt |
| Kennlicht | weißes Licht oben links im Schirm | Signal ist abgeschaltet, alle anderen Lichter sind dunkel |

Zs1/Zs7/Zs8 sind rein optisch: Create-Züge fahren weiterhin nicht an einem Halt zeigenden Signal vorbei. Ein Signal kann sowohl ein Ersatzsignal als auch einen Zs2 tragen: Dasselbe Redstone-Signal lässt bei Halt das Ersatzsignal, bei Fahrt den Zs2 leuchten.

Zs2v gibt es am H/V-Vorsignal, am H/V-Kombisignal, am Ks-Vorsignal und am Ks-Mehrabschnittssignal.

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
- **Hauptsignale sind zwei Blöcke hoch:** H/V-Hauptsignal, H/V-Hauptsignal mit Vorsignal am Mast, Ks-Hauptsignal und Ks-Mehrabschnittssignal. Über dem Platzierungsort muss ein Block frei sein. Wird eine Hälfte abgebaut, verschwindet auch die andere. Bindung, Schraubenschlüssel und Aufnehmen funktionieren an beiden Hälften. Vorsignale und das Lichtsperrsignal sind einen Block hoch.
- **Lichtsperrsignal:** Auf dem Boden oder einem festen Block steht es auf einem kleinen Fuß. Steht darunter ein Signalmast, sitzt es ohne Fuß direkt auf dem Mast.
- Der Schirm zeigt beim Platzieren immer zum Spieler.

### Ausstattung einstellen

H/V-Hauptsignale, H/V-Kombisignale, Ks-Hauptsignale und Ks-Mehrabschnittssignale haben auf der **Rückseite der unteren Hälfte** vier Create-Wertefelder im Quadrat. Mit dem **Schraubenschlüssel** auf ein Feld zielen, Rechtsklick gedrückt halten und auswählen:

| | links | rechts |
|---|---|---|
| **oben** | Ersatzsignal: keins, Zs1, Zs7 oder Zs8 | Zs2-Buchstabe: keiner oder A–Z |
| **unten** | Gegengleisanzeiger: keiner oder Zs6 | Betrieb: in Betrieb oder abgeschaltet (Kennlicht) |

Links und rechts gelten von hinten gesehen. Ein Rechtsklick mit dem Schraubenschlüssel außerhalb der Felder stellt wie bisher Hp2 (H/V) bzw. Zs3 (Ks) ein. Redstone für Zs1/Zs7/Zs8 und Zs2 kann an der oberen oder unteren Hälfte anliegen, z. B. als Hebel, Redstone-Block oder Redstone-Staub direkt neben dem Signal.

**Eingebaute Funkverbindung:** Die zwei Blöcke hohen Signale haben auf der Rückseite der **oberen Hälfte** zwei Frequenzfelder wie ein Create-Redstone-Link. Mit einem Gegenstand in der Hand auf ein Feld rechtsklicken setzt die Frequenz. Sendet ein Redstone-Link auf derselben Frequenz, wirkt das wie Redstone am Signal. Ein zusätzlicher Redstone-Link-Block am Signal ist nicht nötig.

### Signallogik

- **H/V-Hauptsignal:** Create-Signal rot → Hp0, grün/gelb → Hp1. Create kennt keine Geschwindigkeiten. Mit dem **Create-Schraubenschlüssel** schaltest du deshalb zwischen Hp1 und Hp2 um.
- **H/V-Vorsignal:** Wird an das Create-Signal des **angekündigten** Hauptsignals gebunden und steht im Bremswegabstand davor. Gibt das Hauptsignal dort eine Geschwindigkeit vor (Hp2 oder Ks-Zs3), zeigt es Vr2.
- **H/V-Hauptsignal mit Vorsignal am Mast:** Der Hauptsignalteil arbeitet wie das H/V-Hauptsignal. Der Vorsignalteil kündigt automatisch die Signale am Ende des Blockabschnitts an. Gibt es durch Weichen mehrere Folgesignale, erwartet es nur Fahrt, wenn alle Fahrt zeigen, sonst Vr0. Zeigt das eigene Hauptsignal Hp0, ist das Vorsignal dunkel.
- **Lichtsperrsignal:** Create-Signal rot → Sh0, grün/gelb → Sh1.
- **Ks-Hauptsignal:** Create-Signal rot → Hp0, grün/gelb → Ks1. Mit dem **Schraubenschlüssel** wird der Geschwindigkeitsanzeiger **Zs3** eingestellt (aus, 10–90 km/h). Er leuchtet, solange das Signal Fahrt zeigt.
- **Ks-Vorsignal:** Wird wie das H/V-Vorsignal an das Create-Signal des angekündigten Hauptsignals gebunden. Hält das Hauptsignal → Ks2. Gibt es dort eine Geschwindigkeit vor (Zs3 oder H/V-Hp2) → Ks1 blinkt und der **Zs3v** zeigt sie an. Sonst Ks1.
- **Ks-Mehrabschnittssignal:** Eigenes Create-Signal rot → Hp0. Sonst kündigt es wie das H/V-Kombisignal automatisch die Folgesignale an: Ks2, Ks1 blinkend mit Zs3v oder Ks1. Zusätzlich lässt sich ein eigener Zs3 einstellen.
- **Zs2v:** Vorsignale kündigen den Buchstaben an, den der Zs2 des nächsten Hauptsignals gerade zeigt. Führen mehrere Folgesignale unterschiedliche Buchstaben, bleibt der Zs2v dunkel.
- **Systemübergreifend:** H/V- und Ks-Signale kündigen sich gegenseitig an. Ein H/V-Hp2 gilt als 40 km/h, ein Ks-Zs3 lässt H/V-Vorsignale Vr2 zeigen. Bei mehreren Folgesignalen wird die niedrigste Geschwindigkeit angekündigt.

## Entwicklung

Benötigt JDK 21 und Python 3.10+ mit Pillow (`pip install pillow`).

```sh
./gradlew build                                # Jar nach build/libs/ (Windows: .\gradlew.bat build)
./gradlew runClient                            # Testclient starten
./gradlew runClient -PquickPlay=<Welt>         # Testclient direkt in eine Einzelspielerwelt starten
./gradlew runServer                            # Testserver starten
python tools/generate_assets.py                # Modelle, Blockstates, Texturen, Loot-Tables neu erzeugen
python tools/generate_assets.py --vorlagen     # eingebaute Signale als Blockbench-Vorlagen ausgeben
```

Blockstates, Modelle und Platzhalter-Texturen werden von `tools/generate_assets.py` erzeugt. Änderungen an der Geometrie gehören dort hinein (oder in ein Blockbench-Modell, siehe unten), nicht in die erzeugten JSON-Dateien. Der Generator löscht Modelle und Blockstates, die er nicht mehr erzeugt.

### Eigene Modelle mit Blockbench

Ein Signalkopf lässt sich in [Blockbench](https://www.blockbench.net/) gestalten. Das Skript erzeugt daraus alle Varianten: Signalbegriffe, Mast oder Wandausleger, bei hohen Signalen die untere/obere Hälfte und die Overlay-Anzeigen an den markierten Stellen.

1. `python tools/generate_assets.py --vorlagen` ausführen. Die eingebauten Signale landen als `tools/models/vorlagen/<block>.json` (nicht im Git, enthalten lokale Texturpfade).
2. Vorlage in der Blockbench-Desktop-App öffnen (*File → Open Model*), Projekttyp **Java Block/Item**. Unter Windows werden die Texturen direkt mitgeladen.
3. Modell bearbeiten und mit *File → Export → Export Block/Item Model* als `tools/models/<block>.json` speichern, z. B. `tools/models/ks_hauptsignal.json`. Der Dateiname muss genau der Blockname sein.
4. `python tools/generate_assets.py` ausführen und das Spiel starten.

Ohne Datei in `tools/models/` gilt die eingebaute Geometrie.

Den Signalmast-Block ersetzt `tools/models/signal_mast.json` genauso (ohne Lampen und Marker, alle Elemente werden gezeichnet). Signale ohne eigenen `mast` nutzen dann ebenfalls diesen Mast, alle 16 px wiederholt und auf ihre Höhe gekürzt. Wo ein Schirm oder Schild den Mast schneidet, fällt der Mast in dieser Höhe weg. Mastschild und Overlay-Kästen rücken vor den Mast. Die Wandausleger behalten ihre eingebaute Form.

**Namen der Elemente (oder ihrer Gruppe) steuern die Funktion:**

| Name | Bedeutung |
|---|---|
| `lamp_<name>` | Lampe. Ihre **Nordfläche** zeigt je nach Begriff die passende Lampentextur, an oder aus. Leuchten stellt das Skript ein. |
| `mast` | Nur sichtbar, wenn das Signal frei steht. Ohne `mast` kommt der eingebaute Mast. |
| `arm` | Nur Markierung: Höhe (y von–bis) der Wandausleger, wird selbst nicht gezeichnet. |
| `sockel` | Nur Lichtsperrsignal: Fuß, wenn es nicht auf einem Signalmast steht. Auf dem Mast ersetzt ihn ein Maststummel bis zur Sockeloberkante. |
| `kennlicht`, `zs1`, `zs7` | Nur Markierung: Lage der Lampen dieser Overlay-Anzeigen. Bei `zs1`/`zs7` ein Element je Lampe. |
| `zs2`, `zs2v`, `zs3`, `zs3v`, `zs6` | Nur Markierung: Kasten der Anzeige. Das Skript setzt Gehäuse und Zeichen davor ein; der Zs8 nutzt den Kasten des Zs6. |
| alles andere | Fester Signalkörper (Schirm, Schuten, Mastschild …) |

Eine angehängte Zahl ist erlaubt (`arm2`, `lamp_red2`, `zs12`), `mastschild` zählt dagegen nicht als `mast`. Fehlt eine Markierung, sitzt die Anzeige an der Stelle des eingebauten Modells; das Skript weist darauf hin. Welche Anzeigen ein Signal hat, zeigt seine Vorlage.

**Lampennamen je Block:**

| Block | Lampen |
|---|---|
| `hv_hauptsignal` | `lamp_red`, `lamp_green`, `lamp_yellow` |
| `hv_vorsignal` | `lamp_y_bl`, `lamp_y_tr`, `lamp_g_bl`, `lamp_g_tr` (gelb/grün, unten links/oben rechts) |
| `hv_kombisignal` | beide Sätze oben |
| `sh_sperrsignal` | `lamp_r_l`, `lamp_r_r`, `lamp_w_bl`, `lamp_w_tr` |
| `ks_hauptsignal` | `lamp_red`, `lamp_green` |
| `ks_vorsignal` | `lamp_green`, `lamp_yellow` |
| `ks_mehrabschnittssignal` | `lamp_red`, `lamp_green`, `lamp_yellow` |

**Regeln:**

- Der Schirm zeigt nach **Norden**. Die übrigen Richtungen dreht der Blockstate.
- Zwei Blöcke hohe Signale (H/V-Haupt-, H/V-Kombi-, Ks-Haupt- und Ks-Mehrabschnittssignal) werden in 0–32 px Höhe gebaut, die anderen in 0–16 px. Das Skript teilt hohe Modelle an y = 16. Gedrehte Elemente werden nicht geschnitten, sondern landen ganz in der Hälfte ihrer Mitte.
- Alle Koordinaten liegen zwischen −16 und 32. Gedreht wird nur um **eine** Achse in Schritten von 22,5° (−45 bis 45). Sonst bricht das Skript mit einer Meldung ab.
- Texturen liegen in `src/main/resources/assets/create_railway_systems/textures/block/` oder in `tools/models/textures/`. Von dort kopiert das Skript sie mit; das ersetzt auch Platzhalter wie `lamp_red.png`. Andere Texturpfade im Export werden auf den Dateinamen zurückgeführt.
- Texturvariablen mit eingebauten Namen (`mast`, `screen`, `lamp_red` …) gelten auch für die erzeugten Teile. Wer `mast` auf eine eigene Textur zeigen lässt, bekommt sie auch an den Wandauslegern.
- Modelle aus Blockbench werden mit `render_type: cutout` geschrieben, durchsichtige Pixel funktionieren also. Halbtransparenz geht nicht.
- Die `display`-Einstellungen aus Blockbench gelten für das Item.

### Struktur

```
src/main/java/de/bausteinchen/railwaysystems/
  RailwaySystems.java, Config.java     Einstieg und Server-Config
  registry/                            Blöcke, Items, Block-Entities, Kreativ-Tab
  signal/                              Gemeinsame Basis: Bindung, Block-Entity, Montage, Mast
  signal/hv/                           H/V- und Sh-Signale
  signal/ks/                           Ks-Signale
  client/                              Overlay-Renderer für Zusatzanzeigen (Zs2, Zs2v, Zs3, Zs3v, Zs1/Zs7/Zs8, Zs6, Kennlicht)
tools/generate_assets.py               Asset-Generator
tools/models/                          Eigene Blockbench-Modelle und Texturen (optional)
```

## Geplant

- Bessere Texturen

## Lizenz

MIT
