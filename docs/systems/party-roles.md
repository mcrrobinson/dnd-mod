# Party roles
Every class has a job in a party: **Tank**, **Healer**, **Damage**, **Support** or **Utility**, plus a secondary role. Roles show in the class picker, the Class Guidebook, the party HUD and the README class table.

![The class picker with role icons and the Barbarian tooltip](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/party-roles/party-roles-picker.png)

## How it works
### The roles
| Role | Colour | Icon | Job |
|-|-|-|-|
| Tank | blue `#5B8DEF` | shield | Stand in front, take the hits and keep enemies off the party |
| Healer | green `#55D17A` | cross | Keep the party on its feet |
| Damage | red `#E85D4A` | sword | Bring enemies down fast |
| Support | yellow `#E8C14A` | star | Make everyone nearby better with buffs, songs and debuffs |
| Utility | purple `#B07CE8` | gem | Gear, scouting, obstacles and odd tricks |

| Class | Primary | Secondary |
|-|-|-|
| Barbarian | Tank | Damage |
| Fighter | Tank | Damage |
| Paladin | Tank | Healer |
| Cleric | Healer | Support |
| Druid | Healer | Utility |
| Bard | Support | Healer |
| Alchemist | Support | Healer |
| Artificer | Utility | Tank |
| Necromancer | Utility | Damage |
| Rogue | Damage | Utility |
| Ranger | Damage | Utility |
| Wizard | Damage | Utility |
| Warlock | Damage | Support |
| Monk | Damage | Utility |
| Blood Hunter | Damage | Utility |

### Where roles show
- **Class picker**: each button has its primary role icon at the right edge. Hover a button for a tooltip with both roles, the class's first pro, its special and the obstacles only it can handle ("Only you: Heavy Boulders").
- **Class Guidebook**: two pages after Special. "Your role" shows the primary and secondary role, a line about what the class does in a party and what the role is for. "Obstacles you handle" lists the class's obstacles; bold ones only your class can get past.
- **Party HUD**: a 7x7 role icon before each member's name. Offline members show no icon. The rows widen so long names don't run into the health text.
- **README**: the Player Classes table has a Role column (primary in bold, secondary below).

Roles have no gameplay effect yet. Role synergies and the obstacles themselves come with later Party Roles tickets.

## Where to find it
Open the class picker (first join, or after `/dndclass set <player> none`), read your Class Guidebook, or join a `/party`.

## Commands
None.

## Configuration
None. Roles live in `class_info.json` (see below).

## Known limitations
- The obstacles listed in the guidebook and the picker tooltip don't exist in the world yet. They arrive with the obstacle tickets, which may adjust the wording.
- The picker shows only the primary role icon on the button; the secondary role is in the tooltip.

## For developers
- Data: each class in `src/main/resources/data/dndclasses/class_info.json` has `role` and `secondaryRole` (`PartyRole` names), `roleBlurb` (one phrase, no full stop) and `obstacles` (`**bold**` marks the ones only that class can handle; the picker's "Only you" line lists the bold names).
- `PartyRole.java`: the enum with `color()`, `translationKey()` (`role.dndclasses.<role>`, plus `.description`), `text()` and `iconU()`.
- `ClassInfo` record: `role()`, `secondaryRole()`, `roleBlurb()`, `obstacles()`.
- Icons: `assets/dndclasses/textures/gui/roles.png`, 64x8, one 7x7 icon per role every 8 pixels in enum order. Drawn with `Client/Hud/RoleIcon.draw`.
- Picker: `Client/Hud/RoleButton.java` (icon and tooltip), used by `ClassSelectionHud`.
- Guidebook: `ClassGuidebookScreen.rolePages`.
- Party HUD: `PartyEvents.syncHud` appends each member's class id (`DndCharacter` value as a VarInt, 0 offline) to the `dndclasses:party_hud` packet; `PartyHud` maps it to the role.
- README: `generateClassReadme` / `checkClassReadme` in `build.gradle` add the Role column.
- Devscripts: `devscripts/party-roles.txt` (picker tooltip and guidebook pages, using the `hover` step) and `party-roles-host.txt` / `party-roles-guest.txt` (LAN port 25631; start the guest once the host logs "Started serving on").
