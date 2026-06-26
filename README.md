# Blockforge Prototype

This is a small browser-based Minecraft-like prototype. It gives you a real starting point:

- first-person camera and keyboard movement
- jumping and gravity
- procedurally generated voxel terrain
- break and place blocks
- death and respawn when you fall into the void
- block hotbar with five block types

## Run it

Use a simple local server from this folder.

Do not open `index.html` by double-clicking it with a `file:///...` URL. In that case the browser may show the menu, but the game script will not start correctly.

## Clean setup

After cloning the repository, run:

```bash
python setup_env.py
```

On Windows, if `python` is not available, use:

```powershell
py -3 setup_env.py
```

This creates a local `.venv`, installs `requirements.txt`, and checks whether Java/Javac are available. The current project has no third-party Python dependencies, so `requirements.txt` is intentionally minimal.

### Python

```bash
python -m http.server 8000
```

Then open `http://localhost:8000`.

The browser also needs internet access the first time so it can load the Three.js modules from the CDN.

## Java Port

A first Java port now lives in `java/`.

- source: `java/src/blockforge`
- build and run steps: `java/README.md`

## Controls

- `W A S D` to move
- `Space` to jump
- `Left click` to break a block
- `Right click` to place the selected block
- `1-5` to switch block types
- `Esc` to release the mouse

## Good next steps

- add real block textures instead of flat colors
- save and load the world state
- add trees, water, caves, and mobs
- move from a prototype to a chunk system for larger worlds
