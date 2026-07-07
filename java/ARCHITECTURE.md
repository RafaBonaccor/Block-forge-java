# Blockforge Java Architecture

## Package map

- `blockforge`: application entry point
- `blockforge.ui`: Swing view, input wiring, game loop and HUD
- `blockforge.game`: player state, targeting, mining and gameplay events
- `blockforge.world`: voxel storage, block definitions, terrain and fluid simulation
- `blockforge.render`: first-person/isometric rendering, projection and greedy meshing
- `blockforge.persistence`: save and load

## Main responsibilities

- `Main`: creates the application window
- `GamePanel`: coordinates systems and translates input into game actions
- `MiningController`: owns mining target, progress and block-by-block removal
- `World`: stores voxels and exposes world queries and mutations
- `WaterSimulator`: computes procedural water fall and horizontal flow
- `FirstPersonWorldRenderer`: first-person world, selection and raycast rendering
- `IsometricWorldRenderer`: superior/isometric world rendering
- `GreedyMesher`: builds cached exposed chunk faces
- `SaveGame`: serializes and restores game state

## Dependency direction

`ui` coordinates `game`, `world`, `render` and `persistence`. Renderers read game/world state but do not mutate gameplay. The world owns voxel data and delegates fluid behavior to `WaterSimulator`.

## Next extractions

1. move player collision and gravity from `GamePanel` into a physics controller
2. move HUD and pause-menu painting into dedicated UI components
3. move procedural terrain generation out of `World`
