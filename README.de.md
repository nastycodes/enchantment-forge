# Enchantment Forge (Verzauberungsschmiede)

[🇬🇧 English version](README.md)

Fabric-Mod für **Minecraft 1.21.11** (Fabric Loader 0.20.1+, Fabric API). Fügt die **Verzauberungsschmiede** hinzu:
einen Block, mit dem sich die Verzauberungen von Waffen, Rüstungen und Werkzeugen über das Vanilla-Maximum hinaus
verbessern lassen (Standard: bis Stufe **XX**). Jede Stufe kostet das Material des Gegenstands selbst und gelingt nur
mit einer Wahrscheinlichkeit, die mit jeder Stufe sinkt.

## Installation

`enchantforge-1.0.0.jar` zusammen mit der [Fabric API](https://modrinth.com/mod/fabric-api) in den `mods`-Ordner
legen – auf dem Server **und** bei jedem Client. [Mod Menu](https://modrinth.com/mod/modmenu) ist optional und bringt
den Einstellungsbildschirm im Spiel.

## Spielen

1. Schmiede craften:

   ```
   Diamant      Zaubertisch  Diamant
   Obsidian     Amboss       Obsidian
   Obsidian     Obsidian     Obsidian
   ```

   (Kreativ: Reiter „Funktionsblöcke“, direkt hinter dem Zaubertisch.)
2. Schmiede mit Rechtsklick öffnen, verzauberten Gegenstand oben links einlegen, Material darunter,
   optional **Glücksbringer** in den unteren (lila) Slot.
3. In der Liste die gewünschte Verzauberung anklicken. Der Tooltip zeigt Chance, Material-, XP-Kosten und
   Maximalstufe.

Material und XP werden auch bei Fehlschlag verbraucht.

### Glücksbringer

Erhöht die Erfolgschance: Jeder Glücksbringer im Charm-Slot bringt standardmäßig **+1 Prozentpunkt**, bis zu
64 Stück pro Versuch. Pro Versuch werden nur so viele verbraucht, wie nötig sind, um 100 % zu erreichen – der
Rest bleibt liegen. Verbraucht werden sie bei Erfolg und bei Fehlschlag. In der Liste markiert ein grünes ♣
Chancen, die durch Glücksbringer erhöht sind; der Tooltip zeigt Basis-Chance und Bonus.

Rezept (ergibt 2 Stück):

```
         Knochen
Knochen  Eisenbarren  Knochen
         Knochen
```

### Material

Welches Material gebraucht wird, hängt vom Gegenstand ab – es ist sein Vanilla-Reparaturmaterial:

| Gegenstand | Material |
| --- | --- |
| Holz-Werkzeuge, Schild | beliebige Bretter |
| Stein-Werkzeuge | Bruchstein / Schwarzstein / Bruchtiefenschiefer |
| Kupfer / Eisen / Gold / Diamant | Kupfer-, Eisen-, Goldbarren, Diamant |
| Netherit | Netheritbarren |
| Lederrüstung / Kettenrüstung | Leder / Eisenbarren |
| Elytren / Schildkrötenpanzer / Streitkolben | Phantomhaut / Schildkrötenschuppe / Breezerute |
| Bogen, Angel | Faden (Config) |
| Armbrust, Schere, Feuerzeug | Eisenbarren (Config) |
| Dreizack | Prismarinscherbe (Config) |
| alles andere ohne Reparaturmaterial | Diamant (`fallbackMaterial`) |

Im leeren Materialslot wird das benötigte Material blass angezeigt.

Nicht verbesserbar sind standardmäßig: Verzauberungen mit nur einer Stufe (Behutsamkeit, Reparatur, Unendlichkeit,
Mehrfachschuss, Entzündung, Kanalisierung, Wasseraffinität …) und Flüche. Köder ist auf V begrenzt, weil Fische
darüber in Vanilla gar nicht mehr anbeißen.

Mit den Standardwerten ergibt das z. B.:

| Schritt | Chance | Diamanten | XP-Level |
| --- | --- | --- | --- |
| V → VI | 49,6 % | 6 | 6 |
| IX → X | 25,9 % | 10 | 10 |
| XIV → XV | 11,5 % | 15 | 15 |
| XIX → XX | 5,1 % | 20 | 20 |

Mit 64 Glücksbringern steigt z. B. XIX → XX von 5,1 % auf 69,1 %.

## Einstellungen

Mit Mod Menu: **Mods → Verzauberungsschmiede → Einstellungs-Button**. Jede Option hat einen Tooltip und einen
„Standard“-Button, Eingaben werden vor dem Speichern geprüft; Chancen stehen in Prozent da, Listen und Maps öffnen
sich in einem Texteditor (ein Eintrag pro Zeile). Änderungen gelten sofort. Ohne Mod Menu
`config/enchantforge.json` bearbeiten (wird beim ersten Start angelegt) und `/enchantforge reload` ausführen
(OP-Level 2).

Es gelten die Einstellungen dessen, der die Welt betreibt: in Einzelspieler- und LAN-Welten deine eigenen, auf einem
Server die `config/enchantforge.json` des Servers.

| Schlüssel | Standard | Bedeutung |
| --- | --- | --- |
| `maxLevel` | `20` | Höchste erreichbare Stufe (1–255) |
| `maxLevelOverrides` | `{"minecraft:lure": 5}` | Eigene Grenze pro Verzauberung. Ein Eintrag macht auch Einzelstufen-Verzauberungen verbesserbar (z. B. `"minecraft:multishot": 5`) |
| `excludeSingleLevelEnchantments` | `true` | Verzauberungen mit Vanilla-Maximum I ausschließen |
| `excludeCurses` | `true` | Flüche ausschließen |
| `excludedEnchantments` | Behutsamkeit, Reparatur, Unendlichkeit | Immer ausgeschlossen |
| `excludedItems` | `[]` | Item-IDs, die die Schmiede ablehnt |
| `allowEnchantedBooks` | `false` | Verzauberte Bücher erlauben (Material: Lapislazuli) |
| `baseChance` | `0.95` | Chance für den Schritt I → II |
| `chanceMultiplierPerLevel` | `0.85` | Jede weitere Zielstufe multipliziert die Chance damit |
| `minChance` | `0.02` | Untergrenze der Chance |
| `chancePerTargetLevel` | `{}` | Feste Chancen pro Zielstufe, z. B. `{"20": 0.01}` |
| `failDowngradeChance` | `0.0` | Chance, bei Fehlschlag eine Stufe zu verlieren |
| `charmChanceBonus` | `0.01` | Zusätzliche Chance pro Glücksbringer (0.01 = +1 Prozentpunkt) |
| `charmMaxPerAttempt` | `64` | Höchstens so viele Glücksbringer pro Versuch (0 schaltet sie ab) |
| `charmMaxChance` | `1.0` | Glücksbringer heben die Chance nie über diesen Wert |
| `materialCostBase` / `materialCostPerLevel` | `1` / `1` | Materialkosten = ⌈(Basis + proStufe × aktuelleStufe) × Multiplikator⌉, max. ein Stack |
| `materialCostMultipliers` | `{"minecraft:netherite_ingot": 0.25}` | Kosten-Faktor pro Material |
| `xpCostBase` / `xpCostPerLevel` | `1` / `1` | XP-Level-Kosten = Basis + proStufe × aktuelleStufe |
| `creativeIgnoresCosts` | `true` | Kreativmodus zahlt nichts und verbraucht keine Glücksbringer (Chance gilt trotzdem) |
| `materialOverrides` | Bogen → Faden … | Material pro Item, auch Tags: `"#minecraft:planks"` |
| `fallbackMaterial` | `minecraft:diamond` | Material für Items ohne Reparaturmaterial und ohne Override |

Im Einstellungsbildschirm werden Chancen in Prozent eingegeben (`95` = `0.95`), auch in der Map der festen Chancen
(`20 = 1` = 1 % für Stufe XX). Ein Komma als Dezimaltrennzeichen geht auch.

## Hinweis

Der Vanilla-Amboss begrenzt beim Kombinieren zweier Gegenstände mit **derselben** Verzauberung auf das
Vanilla-Maximum – ein Schärfe-XX-Schwert, das im Amboss mit einem Schärfe-Buch kombiniert wird, fällt also auf V
zurück. Reparieren im Amboss mit Material oder Kombinieren ohne gemeinsame Verzauberung ist unproblematisch.

## Sprachen

Englisch und Deutsch. Es gilt die Spielsprache; auch der Einstellungsbildschirm ist übersetzt.

## Entwickeln

Benötigt ein JDK 21 oder neuer.

```
gradlew build              # baut build/libs/enchantforge-1.0.0.jar, inkl. Übersetzungsprüfung
gradlew runClient          # Testclient (mit Mod Menu)
gradlew runClientGameTest  # End-to-End-Test im echten Client, Screenshots in build/run/clientGameTest/screenshots
python tools/gen_textures.py .  # erzeugt die Texturen (Block, GUI, Glücksbringer) und das Icon neu
```

`checkTranslations` (Teil von `gradlew build`) bricht ab, wenn `en_us.json` und `de_de.json` nicht dieselben
Schlüssel haben. Der Client-Gametest läuft auf Englisch in 1920×1080 und prüft auch den Einstellungsbildschirm
(ungültige Eingaben, Prozentwerte, Map-Editor,
„Änderungen verwerfen?“).

## Lizenz

[MIT](LICENSE)
