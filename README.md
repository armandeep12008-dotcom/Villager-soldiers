# Villager Soldiers (Fabric, Minecraft 26.2, Java 25)

## Build
1. Install JDK 25 and Gradle 9.x, then in this folder run `gradle wrapper`.
2. `./gradlew runClient` to test, `./gradlew build` for the jar (build/libs).

## Use
`/soldier spawn <type>` (op). Types: infantry_sword, infantry_spear, infantry_archer,
horse_sword, horse_spear, camel_guards, ghast_squad_bow, ghast_squad_crossbow, artillery.
Right-click any soldier -> 27-slot inventory. Slots 0-5 = main hand, off hand, head, chest, legs, feet.
Slots 6+ = ammo/spare. Change slots 0-5 and the soldier re-equips instantly.

## Status: UNTESTED scaffold
Written without being able to compile against 26.2. Expect some import/signature fixes
(package names, startRiding overload, saddle/harness slots, LargeFireball ctor). Fix in IDE.
Not done yet: camel/happy ghast flight AI, real cannon block/entity model, commander banner,
unit-follow/hold orders, spawn eggs, crafting recipes.
