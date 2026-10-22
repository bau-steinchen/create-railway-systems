# create-railway-systems

Ein Addon für [Create](https://github.com/Creators-of-Create/Create) (Minecraft 1.21.1, NeoForge), das moderne Bahnsysteme hinzufügt.

## Enthalten (v0.1)

### Deutsche H/V-Lichtsignale

| Block | Begriffe |
|---|---|
| H/V-Lichthauptsignal | Hp0 (Halt), Hp1 (Fahrt), Hp2 (Langsamfahrt) |
| H/V-Lichtvorsignal | Vr0 (Halt erwarten), Vr1 (Fahrt erwarten), Vr2 (Langsamfahrt erwarten) |
| H/V-Hauptsignal mit Vorsignal am Mast | Hp + Vr, Vorsignal dunkel bei Hp0 |
| Signalmast | Mast zum Aufbauen |

**Benutzung**

1. Create-Signal ganz normal an die Strecke setzen.
2. Mit dem H/V-Signal in der Hand **Rechtsklick auf das Create-Signal**, um es zu binden. Das gebundene Signal wird hervorgehoben.
3. H/V-Signal platzieren (max. 32 Blöcke entfernt, einstellbar über `signalLinkRange` in der Server-Config).
4. Schleichen + Rechtsklick löscht eine Bindung, bevor das Signal platziert ist.

**Logik**

- Hauptsignal: Create rot → Hp0, Create grün/gelb → Hp1. Mit dem Create-Schraubenschlüssel lässt sich Hp2 (Langsamfahrt) vorwählen.
- Vorsignal: an das Create-Signal des **angekündigten** Hauptsignals binden. Zeigt Vr2, wenn das dortige H/V-Hauptsignal auf Hp2 eingestellt ist.
- Kombisignal: der Vorsignalteil kündigt automatisch die Signale am Ende des Blockabschnitts an. Bei mehreren erreichbaren Folgesignalen (Weichen) zeigt es nur dann Vr1/Vr2, wenn alle Fahrt zeigen, sonst Vr0.

## Entwicklung

```sh
./gradlew build        # Jar nach build/libs
./gradlew runClient    # Testclient
python3 tools/generate_assets.py   # Modelle, Blockstates, Texturen neu erzeugen (Pillow nötig)
```

Benötigt JDK 21.
