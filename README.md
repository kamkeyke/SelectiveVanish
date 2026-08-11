# 🕵️ SVanish (Selective Vanish)

SVanish is a simple add-on for VanishMod. It introduces selective visibility control, allowing you to choose exactly who can see players while they are vanished.
> ‼️ **Server-side mod:**
SVanish only needs to be installed on the server to function. Clients are not required to install it.

### 🚀 Why use SVanish?
While the original VanishMod has a similar feature, it relies on Minecraft's `/team` system and is disabled by default. SVanish eliminates this necessity by using its own SavedData system on the server. This means you can manage visibility permissions independently without messing with your server's teams or scoreboard setup.

### ✨ Features
- **Independence:** No `/team` configuration required.
- **Persistence:** All data is saved directly in the server's world data.
- **Bulk Management:** Manage multiple players at once using player lists.
- **Player Cache:** Players who have previously joined the server can be referenced even when they are offline.
- **Lore friendly:** Perfect for roleplaying with your friends or using on an SMP server.
---

### 🔗 Compatibility
- Requires [Vanishmod](https://www.curseforge.com/minecraft/mc-mods/vanishmod)
- Requires [RaccoonCore](https://github.com/kamkeyke/RaccoonCore) *(must be installed on both server and client)*
- Designed for Forge
- Safe to use in SMPs and roleplay-focused servers
- Should work fine with any other mods

---

### 💻 Commands & Syntax
SVanish commands can be used through either `/sv` or `/svanish`.
Player arguments support individual player names as well as comma-separated player lists using the `{player1,player2,...}` syntax. Players can also be selected from the server's known-player cache, allowing offline players to be referenced.

| Command                               | Description                                                                             |
|:--------------------------------------|:----------------------------------------------------------------------------------------|
| `/sv allow <viewers> see <targets>`   | Allows the specified viewers to see the specified players while vanished.               |
| `/sv deny <viewers> see <targets>`    | Revokes the specified viewers' permission to see the specified players while vanished.  |
| `/sv clear <targets>`                 | Removes all selective visibility permissions for the specified players.                 |
| `/sv get <targets>`                   | Prints a list of all players who can currently see the specified targets.               |
| `/sv cached knownPlayers`             | Displays all players currently stored in the known-player cache.                        |

The same commands can be used with `/svanish` instead of `/sv`.

### 💡 Examples

* **`/sv allow KamKeyke see Alio`**
  Allows KamKeyke to see Alio while Alio is vanished.
* **`/sv allow {KamKeyke,Koretzy} see Alio`**
  Allows both KamKeyke and Koretzy to see Alio while Alio is vanished.
* **`/sv allow KamKeyke see {Alio,Koretzy}`**
  Allows KamKeyke to see both Alio and Koretzy while they are vanished.
* **`/sv allow {KamKeyke,Koretzy} see {Alio,Hiaku}`**
  Allows both KamKeyke and Koretzy to see both Alio and Hiaku while they are vanished.
* **`/sv deny KamKeyke see Alio`**
  Revokes KamKeyke's permission to see Alio while vanished.
* **`/sv clear Bacalhau`**
  Removes all selective visibility permissions for Bacalhau. Now no one can see them while they are vanished.
* **`/sv get Bacalhau`**
  Displays all players who currently have permission to see Alio while vanished.
* **`/sv cached knownPlayers`**
  Displays all players currently stored in the server's known-player cache. Primarily for development purposes.