# Twilight Lib

Twilight Lib is a library mod used by mc_jojo3's mods. It is designed to more easily allow synchronization and compatibility between my different mods and is in no way meant to be used in other ways. If anyone (which I doubt) finds this mod useful for other purposes, feel free to use it.

- [CurseForge](https://legacy.curseforge.com/minecraft/mc-mods/twilight-lib)
- [Modrinth](https://modrinth.com/mod/twilight-lib)

## 📁 Repository Structure

This repository uses a **mono-repo structure** with independent mod projects:

- **`forge-1.20.1/`** - Forge 1.20.1 mod (standalone Gradle project)
- **`neoforge-1.21.1/`** - NeoForge 1.21.1 mod (standalone Gradle project)

Each module is a complete, independent Gradle project with its own isolated dependency cache. This prevents classpath conflicts and allows both modules to be developed simultaneously.

### Quick Start

**Building:**
```bash
# Forge 1.20.1
cd forge-1.20.1
./gradlew build

# NeoForge 1.21.1
cd neoforge-1.21.1
./gradlew build
```

**Running:**
```bash
# Forge 1.20.1
cd forge-1.20.1
./gradlew runClient --offline

# NeoForge 1.21.1
cd neoforge-1.21.1
./gradlew runClient --offline
```

> **Note**: Both modules work fully in offline mode after initial dependency download. The isolated Gradle caches (`.gradle-forge/` and `.gradle-neoforge/`) ensure no conflicts between versions.

<details>
<summary><strong><u>Functionalities</u></strong></summary>

- Synchronize data between mods, like tags, variables etc
- Custom renderer, for mods like [CreRaces Classic](https://modrinth.com/mod/creraces) etc
- Possibly some shared items / blocks and more...

</details>

<details>
<summary><strong><u>Mods utilizing the Library</u></strong></summary>

- [CreRaces](https://modrinth.com/mod/creraces-experimental)
- [CreRaces Classic](https://modrinth.com/mod/creraces)

</details>

<details>
<summary><strong><u>Supported Versions</u></strong></summary>

- **1.7.10:** N/A: None
- **1.12.2:** N/A: None
- **1.14.4:** N/A: None
- **1.15.2:** N/A: None
- **1.16.5:** N/A: None
- **1.17.1:** N/A: None
- **1.18.2:** N/A: None
- **1.19.2:** N/A: None
- **1.19.4:** N/A: None
- **1.20.1:** ADS: "Active Development Support" (Forge)
- **1.20.4:** N/A: None
- **1.20.6:** N/A: None
- **1.21.1:** ADS: "Active Development Support" (NeoForge)

*Do note that this is the plan, but things may change over time!*

</details>

<details>
<summary><strong><u>Credits</u></strong></summary>

- **Developers:**
  - [mc_jojo3](https://www.legacy.curseforge.com/members/mc_jojo3/)

- **Textures:**
  - Icon traced from art by [Kezi](https://www.artstation.com/kezi)

</details>

Made by: [mc_jojo3](https://modrinth.com/user/mcjojo3)
