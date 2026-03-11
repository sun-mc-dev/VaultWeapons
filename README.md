# VaultWeapons

**A Paper 1.21+ plugin that adds powerful, kill-powered custom weapons to your Minecraft server.**

---

## Overview

VaultWeapons introduces custom craftable weapons that **grow stronger as you kill enemies**. Each weapon has a unique
passive ability that evolves with every kill, tracked invisibly inside the item itself using Persistent Data
Containers (PDC). Everything — weapons, recipes, enchantments, abilities — is fully configurable from a
single `config.yml` file with no coding required.

---

## Weapons

### ⚔ Vault Sword

A diamond sword that starts at **Sharpness I** and becomes deadlier with every kill.

- **Passive Ability:** Every kill permanently increases the sword's Sharpness level by 1.
- **Cap:** Configurable max Sharpness (default: 10).
- **Feedback:** An action bar message shows the new Sharpness level and total kill count after every kill.
- **Crafting:** Shaped recipe using Diamonds and a Blaze Rod.

---

### 🏹 Vault Crossbow

A crossbow enchanted with **Unbreaking III, Multishot, and Infinity** that auto-reloads faster the more you kill.

- **Passive Ability:** Every kill reduces the auto-reload delay by a configurable amount of ticks.
- **Auto-Reload:** After each kill, the crossbow automatically reloads itself after the (reduced) delay — no manual
  reloading needed.
- **Cap:** Configurable minimum reload time so it never becomes instant (unless you want it to).
- **Feedback:** An action bar message shows the current reload delay and total kill count after every kill.
- **Crafting:** Shaped recipe using Gold Ingots, String, and a vanilla Crossbow.

---

## Features

### 🎨 Custom Texture Support

Every weapon supports a `custom_model_data` value in config, which maps directly to a resource pack model predicate.
This lets you give each weapon a completely unique 3D model and texture.

### 🔧 Fully Config-Driven

Weapons are defined entirely in `config.yml`. You can:

- Add unlimited new weapons without touching any code.
- Change weapon names, lore, materials, enchantments, and ability values freely.
- Modify crafting recipes per weapon.

### 📖 Recipe Viewer GUI

Players can run `/vw recipe <weapon_id>` to open a **9×3 inventory GUI** that visually displays the crafting grid,
ingredients, and the resulting weapon — exactly like a recipe book. All interactions with the GUI are locked (no item
stealing).

### 🎆 Craft Celebration

When a player successfully crafts a Vault Weapon they are greeted with:

- A **full-screen title** displaying "✦ VAULT WEAPON CRAFTED ✦" and the weapon name.
- A **challenge-complete sound** effect.
- **Two fireworks** that explode instantly at the player's location with no entity damage.
- A **chat message** confirming the forge.

### ⚡ Async & Multi-Threaded

Config loading and hot-reloads are performed asynchronously on worker threads. All Bukkit API calls are safely synced
back to the main thread, keeping the server performant even on large configs.

### 🔄 Hot Reload

Admins can run `/vw reload` to reload `config.yml` and re-register all weapon recipes at runtime — no server restart
needed.

---

## Commands

| Command                  | Permission           | Description                                |
|--------------------------|----------------------|--------------------------------------------|
| `/vw list`               | `vaultweapons.use`   | Lists all loaded weapons and their IDs     |
| `/vw recipe <id>`        | `vaultweapons.use`   | Opens the recipe viewer GUI for a weapon   |
| `/vw give <player> <id>` | `vaultweapons.admin` | Gives a weapon directly to a player        |
| `/vw reload`             | `vaultweapons.admin` | Reloads config and recipes without restart |

---

## Adding a New Weapon

Simply add a new block under `weapons:` in `config.yml` and run `/vw reload`:

```yaml
weapons:
  my_axe:
    type: NETHERITE_AXE
    name: "&4Void Axe"
    lore:
      - "&7Cleaves through reality."
    custom_model_data: 1003
    enchantments:
      sharpness: 3
      unbreaking: 2
    ability:
      type: NONE
    recipe:
      type: SHAPED
      shape:
        - "DD "
        - "DS "
        - " S "
      ingredients:
        D: NETHERITE_INGOT
        S: BLAZE_ROD
```

No code changes. No restarts. Just config.

---

## Technical Highlights

- **No deprecated Paper/Bukkit API used** — fully forward-compatible with Paper 1.21+.
- **Adventure API** for all text, titles, and action bars.
- **Registry-based enchantment resolution** — no static `Enchantment.*` fields.
- **PDC (Persistent Data Container)** for kill counts and ability state — data survives server restarts and item
  transfers.
- **`Firework#detonate()`** for instant fireworks with zero entity damage.