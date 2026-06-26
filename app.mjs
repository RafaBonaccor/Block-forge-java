import * as THREE from "three";
import { PointerLockControls } from "three/addons/controls/PointerLockControls.js";

const WORLD_RADIUS = 16;
const MOVE_SPEED = 6;
const GRAVITY = 24;
const JUMP_FORCE = 9.5;
const PLAYER_HEIGHT = 1.72;
const PLAYER_RADIUS = 0.32;
const STEP_HEIGHT = 1.05;
const REACH_DISTANCE = 6;
const EPSILON = 1e-4;
const VOID_DEATH_Y = -12;
const RESPAWN_DELAY = 1.2;

const blockTypes = [
  { id: "grass", label: "Grass", color: 0x68b95b },
  { id: "dirt", label: "Dirt", color: 0x7a5134 },
  { id: "stone", label: "Stone", color: 0x8d95a0 },
  { id: "wood", label: "Wood", color: 0x9c6a36 },
  { id: "glow", label: "Glow", color: 0xf6d66e, emissive: 0x98731c },
];

const canvas = document.querySelector("#game");
const introPanel = document.querySelector("#intro-panel");
const playButton = document.querySelector("#play-button");
const startMessage = document.querySelector("#start-message");
const noticeBanner = document.querySelector("#notice-banner");
const deathCountLabel = document.querySelector("#death-count-label");
const selectedBlockLabel = document.querySelector("#selected-block-label");
const hotbarButtons = [...document.querySelectorAll(".slot")];

function setStartMessage(message) {
  if (startMessage) {
    startMessage.textContent = message;
  }
}

function setNotice(message) {
  if (noticeBanner) {
    noticeBanner.textContent = message;
  }
}

const renderer = new THREE.WebGLRenderer({
  canvas,
  antialias: true,
});
renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
renderer.setSize(window.innerWidth, window.innerHeight);
renderer.shadowMap.enabled = false;

const scene = new THREE.Scene();
scene.background = new THREE.Color(0xaad9ff);
scene.fog = new THREE.Fog(0xaad9ff, 16, 62);

const camera = new THREE.PerspectiveCamera(
  75,
  window.innerWidth / window.innerHeight,
  0.1,
  180
);

const controls = new PointerLockControls(camera, renderer.domElement);
const playerObject =
  typeof controls.getObject === "function" ? controls.getObject() : controls.object;
scene.add(playerObject);

const hemiLight = new THREE.HemisphereLight(0xf2f6ff, 0x567241, 1.4);
scene.add(hemiLight);

const sun = new THREE.DirectionalLight(0xfff1c7, 1.25);
sun.position.set(12, 24, 10);
scene.add(sun);

const blockGeometry = new THREE.BoxGeometry(1, 1, 1);
const selectionGeometry = new THREE.BoxGeometry(1.04, 1.04, 1.04);
const selectionMaterial = new THREE.MeshBasicMaterial({
  color: 0xffffff,
  wireframe: true,
  transparent: true,
  opacity: 0.9,
});
const selectionMesh = new THREE.Mesh(selectionGeometry, selectionMaterial);
selectionMesh.visible = false;
scene.add(selectionMesh);

const materials = new Map(
  blockTypes.map((type) => [
    type.id,
    new THREE.MeshStandardMaterial({
      color: type.color,
      emissive: type.emissive ?? 0x000000,
      roughness: 1,
      metalness: 0,
    }),
  ])
);

const world = new Map();
const pickableMeshes = [];
const raycaster = new THREE.Raycaster();
const velocity = new THREE.Vector3();
const direction = new THREE.Vector3();
const moveState = {
  forward: false,
  backward: false,
  left: false,
  right: false,
};

let onGround = false;
let activeSlot = 0;
let currentTarget = null;
let previousFrame = performance.now();
let deathCount = 0;
let respawnCountdown = 0;

function keyFor(x, y, z) {
  return `${x},${y},${z}`;
}

function getTerrainHeight(x, z) {
  const base =
    Math.sin(x * 0.32) * 1.9 +
    Math.cos(z * 0.27) * 1.7 +
    Math.sin((x + z) * 0.18) * 1.3;
  return Math.max(2, Math.floor(base + 5));
}

function addBlock(x, y, z, typeId) {
  const key = keyFor(x, y, z);
  if (world.has(key)) {
    return;
  }

  const material = materials.get(typeId);
  const mesh = new THREE.Mesh(blockGeometry, material);
  mesh.position.set(x + 0.5, y + 0.5, z + 0.5);
  mesh.userData.block = { x, y, z, typeId };
  scene.add(mesh);

  world.set(key, { typeId, mesh });
  pickableMeshes.push(mesh);
}

function removeBlock(x, y, z) {
  const key = keyFor(x, y, z);
  const block = world.get(key);
  if (!block) {
    return;
  }

  scene.remove(block.mesh);
  const meshIndex = pickableMeshes.indexOf(block.mesh);
  if (meshIndex >= 0) {
    pickableMeshes.splice(meshIndex, 1);
  }
  world.delete(key);
}

function hasBlock(x, y, z) {
  return world.has(keyFor(x, y, z));
}

function isSolidBlock(block) {
  return block !== undefined;
}

function generateWorld() {
  for (let x = -WORLD_RADIUS; x <= WORLD_RADIUS; x += 1) {
    for (let z = -WORLD_RADIUS; z <= WORLD_RADIUS; z += 1) {
      const top = getTerrainHeight(x, z);
      for (let y = 0; y <= top; y += 1) {
        let typeId = "stone";
        if (y === top) {
          typeId = top <= 3 ? "dirt" : "grass";
        } else if (y >= top - 2) {
          typeId = "dirt";
        }
        addBlock(x, y, z, typeId);
      }

      if ((x * x + z * z) % 31 === 0 && top >= 4) {
        addBlock(x, top + 1, z, "wood");
        if ((Math.abs(x) + Math.abs(z)) % 2 === 0) {
          addBlock(x, top + 2, z, "glow");
        }
      }
    }
  }
}

function addClouds() {
  const cloudMaterial = new THREE.MeshStandardMaterial({
    color: 0xfafcff,
    transparent: true,
    opacity: 0.92,
    roughness: 1,
  });
  const cloudGeometry = new THREE.BoxGeometry(2.8, 1.1, 1.8);
  const cloudAnchors = [
    [-10, 18, -8],
    [0, 21, 12],
    [11, 17, 2],
    [8, 23, -11],
  ];

  for (const [x, y, z] of cloudAnchors) {
    const core = new THREE.Mesh(cloudGeometry, cloudMaterial);
    core.position.set(x, y, z);
    scene.add(core);

    const puff = new THREE.Mesh(cloudGeometry, cloudMaterial);
    puff.position.set(x + 2.3, y + 0.2, z - 0.4);
    puff.scale.set(0.78, 0.78, 0.78);
    scene.add(puff);
  }
}

function spawnPlayer() {
  const startY = getTerrainHeight(0, 0) + PLAYER_HEIGHT + 2;
  playerObject.position.set(0.5, startY, 0.5);
  velocity.set(0, 0, 0);
  onGround = false;
  currentTarget = null;
  selectionMesh.visible = false;
}

function updateDeathLabel() {
  if (deathCountLabel) {
    deathCountLabel.textContent = `Deaths: ${deathCount}`;
  }
}

function killPlayer() {
  if (respawnCountdown > 0) {
    return;
  }

  deathCount += 1;
  updateDeathLabel();
  respawnCountdown = RESPAWN_DELAY;
  velocity.set(0, 0, 0);
  currentTarget = null;
  selectionMesh.visible = false;
  setNotice("Sei caduto nel vuoto. Respawn in corso...");
}

function updateRespawn(delta) {
  if (respawnCountdown <= 0) {
    return false;
  }

  respawnCountdown = Math.max(0, respawnCountdown - delta);
  if (respawnCountdown === 0) {
    spawnPlayer();
    setNotice("Respawn completato.");
  }
  return true;
}

function getPlayerAabb(position) {
  return {
    minX: position.x - PLAYER_RADIUS,
    maxX: position.x + PLAYER_RADIUS,
    minY: position.y - PLAYER_HEIGHT,
    maxY: position.y,
    minZ: position.z - PLAYER_RADIUS,
    maxZ: position.z + PLAYER_RADIUS,
  };
}

function getCollidingBlocks(position) {
  const aabb = getPlayerAabb(position);
  const collisions = [];
  const maxX = Math.floor(aabb.maxX - EPSILON);
  const maxY = Math.floor(aabb.maxY - EPSILON);
  const maxZ = Math.floor(aabb.maxZ - EPSILON);

  for (let x = Math.floor(aabb.minX); x <= maxX; x += 1) {
    for (let y = Math.floor(aabb.minY); y <= maxY; y += 1) {
      for (let z = Math.floor(aabb.minZ); z <= maxZ; z += 1) {
        const block = world.get(keyFor(x, y, z));
        if (!isSolidBlock(block)) {
          continue;
        }

        collisions.push({ x, y, z });
      }
    }
  }

  return collisions;
}

function collidesAt(position) {
  return getCollidingBlocks(position).length > 0;
}

function movePlayerHorizontally(delta) {
  direction.set(0, 0, 0);
  if (moveState.forward) {
    direction.z -= 1;
  }
  if (moveState.backward) {
    direction.z += 1;
  }
  if (moveState.left) {
    direction.x -= 1;
  }
  if (moveState.right) {
    direction.x += 1;
  }

  if (direction.lengthSq() === 0) {
    return;
  }

  direction.normalize();

  const right = new THREE.Vector3().setFromMatrixColumn(camera.matrix, 0).normalize();
  const forward = new THREE.Vector3().crossVectors(camera.up, right).normalize();
  const move = new THREE.Vector3();
  move.addScaledVector(forward, -direction.z * MOVE_SPEED * delta);
  move.addScaledVector(right, direction.x * MOVE_SPEED * delta);

  const player = playerObject.position;
  const allowStep = onGround || velocity.y <= 0;

  if (move.x !== 0) {
    const candidate = player.clone();
    candidate.x += move.x;
    if (!collidesAt(candidate)) {
      player.x = candidate.x;
    } else if (allowStep) {
      const stepped = candidate.clone();
      stepped.y += STEP_HEIGHT;
      if (!collidesAt(stepped)) {
        player.copy(stepped);
      }
    }
  }

  if (move.z !== 0) {
    const candidate = player.clone();
    candidate.z += move.z;
    if (!collidesAt(candidate)) {
      player.z = candidate.z;
    } else if (allowStep) {
      const stepped = candidate.clone();
      stepped.y += STEP_HEIGHT;
      if (!collidesAt(stepped)) {
        player.copy(stepped);
      }
    }
  }
}

function applyGravity(delta) {
  velocity.y -= GRAVITY * delta;
  const player = playerObject.position;
  const candidate = player.clone();
  candidate.y += velocity.y * delta;

  const collisions = getCollidingBlocks(candidate);
  if (collisions.length === 0) {
    player.y = candidate.y;
    onGround = false;
    return;
  }

  if (velocity.y <= 0) {
    let highestSurface = -Infinity;
    for (const block of collisions) {
      highestSurface = Math.max(highestSurface, block.y + 1);
    }
    player.y = highestSurface + PLAYER_HEIGHT;
    velocity.y = 0;
    onGround = true;
    return;
  }

  let lowestCeiling = Infinity;
  for (const block of collisions) {
    lowestCeiling = Math.min(lowestCeiling, block.y);
  }
  player.y = lowestCeiling;
  velocity.y = 0;
}

function updateSelection() {
  const origin = camera.getWorldPosition(new THREE.Vector3());
  const directionVector = camera.getWorldDirection(new THREE.Vector3());
  raycaster.set(origin, directionVector);
  raycaster.near = 0;
  raycaster.far = REACH_DISTANCE;
  const intersections = raycaster.intersectObjects(pickableMeshes, false);

  if (intersections.length === 0) {
    selectionMesh.visible = false;
    currentTarget = null;
    return;
  }

  const target = intersections[0];
  currentTarget = target;
  selectionMesh.visible = true;
  selectionMesh.position.copy(target.object.position);
}

function updateHotbar() {
  hotbarButtons.forEach((button, index) => {
    button.classList.toggle("slot--active", index === activeSlot);
  });
  selectedBlockLabel.textContent = blockTypes[activeSlot].label;
}

function selectSlot(index) {
  activeSlot = (index + blockTypes.length) % blockTypes.length;
  updateHotbar();
}

function canPlaceBlockAt(x, y, z) {
  if (hasBlock(x, y, z)) {
    return false;
  }

  const playerAabb = getPlayerAabb(playerObject.position);
  const overlapsPlayer =
    playerAabb.minX < x + 1 &&
    playerAabb.maxX > x &&
    playerAabb.minY < y + 1 &&
    playerAabb.maxY > y &&
    playerAabb.minZ < z + 1 &&
    playerAabb.maxZ > z;

  return !overlapsPlayer;
}

function breakTargetedBlock() {
  if (!currentTarget) {
    return;
  }

  const { x, y, z } = currentTarget.object.userData.block;
  if (y === 0) {
    return;
  }
  removeBlock(x, y, z);
  selectionMesh.visible = false;
  currentTarget = null;
}

function placeBlockNextToTarget() {
  if (!currentTarget) {
    return;
  }

  const targetBlock = currentTarget.object.userData.block;
  const normal = currentTarget.face.normal
    .clone()
    .transformDirection(currentTarget.object.matrixWorld)
    .round();
  const x = targetBlock.x + normal.x;
  const y = targetBlock.y + normal.y;
  const z = targetBlock.z + normal.z;

  if (!canPlaceBlockAt(x, y, z)) {
    return;
  }

  addBlock(x, y, z, blockTypes[activeSlot].id);
}

function onMouseDown(event) {
  if (!controls.isLocked) {
    return;
  }

  if (event.button === 0) {
    breakTargetedBlock();
  } else if (event.button === 2) {
    placeBlockNextToTarget();
  }
}

function onKeyDown(event) {
  switch (event.code) {
    case "KeyW":
      moveState.forward = true;
      break;
    case "KeyS":
      moveState.backward = true;
      break;
    case "KeyA":
      moveState.left = true;
      break;
    case "KeyD":
      moveState.right = true;
      break;
    case "Space":
      if (onGround) {
        velocity.y = JUMP_FORCE;
        onGround = false;
      }
      break;
    case "Digit1":
    case "Digit2":
    case "Digit3":
    case "Digit4":
    case "Digit5":
      selectSlot(Number(event.code.replace("Digit", "")) - 1);
      break;
    default:
      break;
  }
}

function onKeyUp(event) {
  switch (event.code) {
    case "KeyW":
      moveState.forward = false;
      break;
    case "KeyS":
      moveState.backward = false;
      break;
    case "KeyA":
      moveState.left = false;
      break;
    case "KeyD":
      moveState.right = false;
      break;
    default:
      break;
  }
}

function onResize() {
  camera.aspect = window.innerWidth / window.innerHeight;
  camera.updateProjectionMatrix();
  renderer.setSize(window.innerWidth, window.innerHeight);
}

function animate(now) {
  const delta = Math.min((now - previousFrame) / 1000, 0.05);
  previousFrame = now;

  if (controls.isLocked) {
    if (!updateRespawn(delta)) {
      movePlayerHorizontally(delta);
      applyGravity(delta);
      if (playerObject.position.y < VOID_DEATH_Y) {
        killPlayer();
      } else {
        updateSelection();
      }
    }
  }

  renderer.render(scene, camera);
  requestAnimationFrame(animate);
}

generateWorld();
addClouds();
spawnPlayer();
updateHotbar();
updateDeathLabel();
window.__blockforgeBooted = true;
setStartMessage("Click Enter World, then allow mouse capture if your browser asks.");
setNotice("Raggiungi il bordo o scava troppo in basso: se cadi nel vuoto, muori.");

document.addEventListener("contextmenu", (event) => event.preventDefault());
document.addEventListener("mousedown", onMouseDown);
document.addEventListener("pointerlockerror", () => {
  setStartMessage(
    "Pointer lock was blocked. Click Enter World again, then click inside the game area."
  );
});
window.addEventListener("keydown", onKeyDown);
window.addEventListener("keyup", onKeyUp);
window.addEventListener("resize", onResize);

hotbarButtons.forEach((button) => {
  button.addEventListener("click", () => {
    selectSlot(Number(button.dataset.slot));
  });
});

playButton.addEventListener("click", () => {
  setStartMessage("Starting world...");
  controls.lock();
});

controls.addEventListener("lock", () => {
  introPanel.classList.add("panel--hidden");
  setStartMessage("");
});

controls.addEventListener("unlock", () => {
  introPanel.classList.remove("panel--hidden");
  setStartMessage("Game paused. Click Enter World to resume.");
});

requestAnimationFrame(animate);
