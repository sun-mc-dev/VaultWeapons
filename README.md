# VaultWeapons

**A Paper 1.21+ plugin that adds powerful, kill-powered custom weapons to your Minecraft server.**

---

## Overview

VaultWeapons introduces custom craftable weapons that **grow stronger as you fight**. Each weapon carries a unique
passive ability that activates on kills or hits, tracked invisibly inside the item itself using Persistent Data
Containers (PDC). Everything — weapons, recipes, enchantments, abilities — is fully configurable from a single
`config.yml` file with no coding required.

---

## Weapons

### ⚔ Vault Sword

A diamond sword that starts at **Sharpness I** and becomes deadlier with every kill.

- **Ability:** `SWORD_SHARPNESS` — each kill increases Sharpness by 1 (cap configurable, default 10).
- **Crafting:** Diamonds + Blaze Rod.

### 🏹 Vault Crossbow

A crossbow loaded with **Unbreaking III, Multishot, and Infinity** that auto-reloads faster the more you kill.

- **Ability:** `CROSSBOW_RELOAD` — each kill reduces the auto-reload delay. After the delay, the crossbow reloads itself
  automatically.
- **Crafting:** Gold Ingots + String + vanilla Crossbow.

### 🗡 Soul Blade

A golden sword that feeds on fallen enemies.

- **Ability:** `LIFESTEAL` — each kill heals the wielder by a configurable amount of hearts.
- **Crafting:** Gold Ingots + Nether Star.

### 👻 Phantom Blade

A netherite sword that turns its wielder into a blur.

- **Ability:** `SPEED_BOOST` — each kill grants a configurable Speed effect for a configurable duration.
- **Crafting:** Feathers + Netherite Ingot + Stick.

### ⚡ Storm Sword

An iron sword that calls the sky to bear witness.

- **Ability:** `LIGHTNING_STRIKE` — each kill summons a visual lightning bolt at the victim's location. Purely
  cosmetic — no fire, no extra damage.
- **Crafting:** Lightning Rod + Iron Sword + Stick.

### ☠ Venom Dagger

A stone blade coated in slow-acting venom.

- **Ability:** `POISON_BLADE` — every hit applies a configurable Poison effect to the target.
- **Crafting:** Spider Eye + Poisonous Potato + Stick.

---

## Abilities Reference

| Ability            | Trigger    | Config Keys                                                   |
|--------------------|------------|---------------------------------------------------------------|
| `CROSSBOW_RELOAD`  | Kill (PvP) | `base_reload_ticks`, `reduction_per_kill`, `min_reload_ticks` |
| `SWORD_SHARPNESS`  | Kill (PvP) | `max_sharpness`                                               |
| `LIFESTEAL`        | Kill (PvP) | `heal_amount` (half-hearts, e.g. `4.0` = 2 hearts)            |
| `SPEED_BOOST`      | Kill (PvP) | `duration_ticks`, `amplifier` (1 = Speed I, 2 = Speed II)     |
| `LIGHTNING_STRIKE` | Kill (PvP) | *(no extra config needed)*                                    |
| `POISON_BLADE`     | Hit (PvP)  | `duration_ticks`, `amplifier` (0 = Poison I, 1 = Poison II)   |
| `NONE`             | —          | *(no ability)*                                                |

> All abilities are **PvP-only** — they only trigger when the victim is a player.

---

## Features

### 🎨 Custom Texture Support

Every weapon supports a `custom_model_data` value in config, which maps directly to a resource pack model predicate,
giving each weapon a completely unique 3D model and texture.

### 🔧 Fully Config-Driven

Weapons are defined entirely in `config.yml`. You can:

- Add unlimited new weapons without touching any code.
- Change names, lore, materials, enchantments, and ability values freely.
- Modify crafting recipes per weapon.
- Hot-reload everything with `/vw reload`.

### 📖 Recipe Viewer GUI

Players run `/vw recipe <weapon_id>` to open a **9×3 inventory GUI** showing the crafting grid, ingredients,
and the resulting weapon. All interactions are locked — no item stealing.

### 🎆 Craft Celebration

On crafting any Vault Weapon the player receives:

- A **full-screen title** — "✦ VAULT WEAPON CRAFTED ✦" + weapon name.
- A **challenge-complete sound** effect.
- **Two instant fireworks** at the player's feet — zero entity damage.
- A **chat confirmation** message.

### 🚫 Ingredient Protection

Vault Weapons cannot be placed into any crafting grid as an ingredient. The result slot is wiped
immediately via `PrepareItemCraftEvent` and the player sees an action bar warning.

### ⚡ Async & Multi-Threaded

Config loading and hot-reloads run on worker threads. All Bukkit API calls are synced back to the
main thread, keeping the server tick unaffected even with large weapon configs.

### 🔄 Hot Reload

`/vw reload` reloads `config.yml` and re-registers all recipes at runtime — no restart needed.

---

## Commands

| Command                  | Permission           | Description                                |
|--------------------------|----------------------|--------------------------------------------|
| `/vw list`               | `vaultweapons.use`   | Lists all loaded weapons and their IDs     |
| `/vw recipe <id>`        | `vaultweapons.use`   | Opens the 9×3 recipe viewer GUI            |
| `/vw give <player> <id>` | `vaultweapons.admin` | Gives a weapon directly to a player        |
| `/vw reload`             | `vaultweapons.admin` | Reloads config and recipes without restart |

---

## Adding a New Weapon

Add a block under `weapons:` in `config.yml` and run `/vw reload`:

```yaml
weapons:
  my_weapon:
    type: NETHERITE_AXE
    name: "&4Void Axe"
    lore:
      - "&7Cleaves through reality."
      - " "
      - "&ePassive: &fKills restore health."
    custom_model_data: 2001
    enchantments:
      sharpness: 3
      unbreaking: 2
    ability:
      type: LIFESTEAL
      heal_amount: 6.0      # 3 hearts per kill
    recipe:
      type: SHAPED
      shape:
        - "NN "
        - "NB "
        - " S "
      ingredients:
        N: NETHERITE_INGOT
        B: BLAZE_ROD
        S: STICK
```

No code changes. No restarts. Just config.

---

## Technical Highlights

- **No deprecated Paper/Bukkit API** — fully forward-compatible with Paper 1.21+.
- **Adventure API** for all text, titles, and action bars.
- **Registry-based enchantment resolution** — no static `Enchantment.*` fields.
- **PDC (Persistent Data Container)** — kill counts and ability state survive server restarts and item transfers.
- **`Firework#detonate()`** — instant fireworks with zero entity damage, backed by a UUID deny-list to cancel the damage
  event.
- **`PrepareItemCraftEvent`** — vault weapons are blocked as ingredients before the result slot is ever shown.
- **`HitListener`** — separate listener for hit-based abilities (`POISON_BLADE`) keeps kill and hit logic cleanly
  separated.