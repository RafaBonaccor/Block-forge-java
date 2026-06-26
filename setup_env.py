from __future__ import annotations

import os
import shutil
import subprocess
import venv
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parent
VENV_DIR = PROJECT_ROOT / ".venv"
REQUIREMENTS_FILE = PROJECT_ROOT / "requirements.txt"


def run(command: list[str]) -> None:
    print(f"$ {' '.join(command)}")
    subprocess.check_call(command, cwd=PROJECT_ROOT)


def venv_python() -> Path:
    if os.name == "nt":
        return VENV_DIR / "Scripts" / "python.exe"
    return VENV_DIR / "bin" / "python"


def create_venv() -> None:
    if VENV_DIR.exists():
        print(f"Using existing virtual environment: {VENV_DIR}")
        return

    print(f"Creating virtual environment: {VENV_DIR}")
    venv.EnvBuilder(with_pip=True).create(VENV_DIR)


def install_requirements() -> None:
    python = venv_python()
    if not REQUIREMENTS_FILE.exists():
        raise FileNotFoundError(f"Missing {REQUIREMENTS_FILE}")

    run([str(python), "-m", "pip", "install", "-r", str(REQUIREMENTS_FILE)])


def check_java() -> None:
    java = shutil.which("java")
    javac = shutil.which("javac")

    if not java or not javac:
        print()
        print("Java/Javac was not found on PATH.")
        print("Install JDK 21 or newer before running the Java version.")
        return

    print()
    run([javac, "-version"])
    run([java, "-version"])


def print_next_steps() -> None:
    print()
    print("Setup complete.")
    print()
    if os.name == "nt":
        print("Activate the environment:")
        print(r"  .\.venv\Scripts\Activate.ps1")
        print()
        print("Run the Java game:")
        print(r"  .\run-java.ps1")
    else:
        print("Activate the environment:")
        print("  source .venv/bin/activate")
        print()
        print("Run the Java game:")
        print("  ./run-java.sh")


def main() -> int:
    create_venv()
    install_requirements()
    check_java()
    print_next_steps()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
