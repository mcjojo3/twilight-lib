# ✨ Twilight Lib

[![CurseForge](https://img.shields.io/badge/CurseForge-Twilight_Lib-orange?style=for-the-badge&logo=curseforge)](https://legacy.curseforge.com/minecraft/mc-mods/twilight-lib)
[![Modrinth](https://img.shields.io/badge/Modrinth-Twilight_Lib-green?style=for-the-badge&logo=modrinth)](https://modrinth.com/mod/twilight-lib)

**Twilight Lib** is a high-performance, cross-platform library mod designed for mc_jojo3's projects. It provides a robust framework for player cosmetics, data synchronization, and multi-loader compatibility.

---

## 🚀 Key Features

### 🎭 Advanced Morph System
Transform into any supported Minecraft entity. The system automatically handles:
- **Visual Transforms**: Accurate entity rendering.
- **Hitbox Synchronization**: Server-side hitbox updates to match the morphed entity.
- **Perspective Adjustment**: Eye height shifts based on entity size.

### 👒 Cosmetic Addon System
Attach custom 3D models directly to the player rig:
- **Modular Addons**: Equipment like tails, ears, wings, and more.
- **Tinting System**: Support for per-layer color overrides.
- **Low Impact**: Optimized rendering to ensure zero FPS drops.

### ✨ Ambient Cosmetics
- **Particle Trails**: Custom particle effects that follow movement.
- **Visual Effects**: Special effects for spawning, death, and persistent idle states.

### 🛡️ Supporter Integration
- **Cross-Platform Sync**: Automatic unlocking of cosmetics based on supporter tiers.
- **Persistent Storage**: All selections are saved to player NBT and synchronized across servers.

---

## 📁 Repository Structure

This repository follows a **multi-version ARCHITECTURY** pattern, allowing seamless development across multiple Minecraft versions and loaders:

- **[`1.20.1/`](file:///H:/Github/twilight-lib/1.20.1)**: Supports Forge & Fabric.
- **[`1.21.1/`](file:///H:/Github/twilight-lib/1.21.1)**: Supports NeoForge & Fabric.

Each version is further divided into:
- **`common`**: Shared core logic, registry, and assets.
- **`fabric` / `forge` / `neoforge`**: Platform-specific hooks and implementation.

---

## 🛠️ Build Information

The project uses a centralized Gradle setup. You can build all platforms simultaneously from the root directory.

### **Building**
```bash
./gradlew build
```

### **Running (Dev Environment)**
```bash
# 1.20.1 Fabric
./gradlew :1.20.1:fabric:runClient

# 1.21.1 NeoForge
./gradlew :1.21.1:neoforge:runClient
```

---

## 🤝 Used By
- [CreRaces](https://modrinth.com/mod/creraces-experimental)
- [CreRaces Classic](https://modrinth.com/mod/creraces)

---

## 📜 Credits
- **Primary Developer**: [mc_jojo3](https://www.legacy.curseforge.com/members/mc_jojo3/)
- **Icon Art**: Traced from original art by [Kezi](https://www.artstation.com/kezi)
- **Special Thanks**: To all supporters who make development possible!

---
<p align="center">Made by mc_jojo3</p>
