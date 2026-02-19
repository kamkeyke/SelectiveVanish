# 🕵️ SVanish (Selective Vanish)

SVanish is a simples add-on for VanishMod. It introduces selective visibility control, allowing you to choose exactly who can see players while they are vanished.

### 🚀 Why use SVanish?
While the original VanishMod has a similar feature, it relies on Minecraft's /team system and is disabled by default. SVanish eliminates this necessity by using its own SavedData system on the server. This means you can manage visibility permissions independently without messing with your server's teams or scoreboard setup.

### ✨ Features
- **Independence:** No /team configuration required.
- **Persistence:** All data is saved directly in the server's world data.
- **Bulk Management:** Use the "group" syntax to manage multiple players at once easily.
- **Lore friendly:** Perfect for roleplaying with your friends or using on an SMP server.
---

### 🔗 Compatibility
- Requires [Vanishmod](https://www.curseforge.com/minecraft/mc-mods/vanishmod)
- Designed for Forge
- Safe to use in SMPs and roleplay-focused servers
- Should work fine with any other mods

---

### 💻 Commands & Syntax
| Command                                      | Description                                                         |
|:---------------------------------------------|:--------------------------------------------------------------------|
| /sv allow \<player> see \<target>            | Allows a specific player to see the target while vanished.          |
| /sv allow group \[<p1, p2...>] see \<target> | Allows a group of players to see the target.                        |
| /sv deny \<player> see \<target>             | Revokes a player's permission to see the target.                    |
| /sv deny group \[<p1, p2...>] see \<target>  | Revokes a group of player's permission to see the target.           |
| /sv clear \<player>                          | No one will be able to see this player in vanish anymore.           |
| /sv clear group \[<p1, p2...>]               | No one will be able to see this group of players in vanish anymore. |
| /sv get \<player>                            | Prints a list of all players who can currently see the target.      |

### 💡 Examples
- **/sv allow _KamKeyke_ see _Alio_** - Allows KamKeyke to see Alio when vanished.
- **/sv allow group _{KamKeyke, Koretzy}_ see _Alio_** - Allows KamKeyke and Koretzy to see Alio when vanished.
- **/sv deny _KamKeyke_ see _Alio_** - Revokes Kamkeyke's permission to see Alio in vanish; now only Koretzy can.
- **/sv clear _Alio_** - Now no one can see Alio in vanish anymore.