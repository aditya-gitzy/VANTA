<div align="center">

# 🌌 VANTA
### Modular Desktop Operations Console for Linux & Windows

[![Java 21 LTS](https://img.shields.io/badge/Java-21%20LTS-007396?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![JavaFX 21](https://img.shields.io/badge/JavaFX-21.0.2-FF6600?logo=java&logoColor=white)](https://openjfx.io/)
[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen)](https://github.com/aditya-gitzy/VANTA)
[![Platform Linux](https://img.shields.io/badge/Platform-Linux%20%7C%20Ubuntu%20%7C%20Fedora%20%7C%20Arch%20%7C%20RPI-E95420?logo=linux&logoColor=white)](https://ubuntu.com/)
[![Platform Windows](https://img.shields.io/badge/Platform-Windows%2011%20%7C%2010-0078D6?logo=windows&logoColor=white)](#-windows-10--11-installation--launch)
[![Security](https://img.shields.io/badge/Security-Sandboxed%20ProcessBuilder-blueviolet)](#-security--isolation-model)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

<br/>

**VANTA** is a focused, high-performance desktop cockpit running on Linux and Windows.  
It provides a unified, security-bounded control interface for **browsing files**, **running approved diagnostic commands**, and **monitoring live hardware telemetry** — with zero desktop bloat, zero open web ports, and no custom kernel required.

[Features](#-key-features) • [Why VANTA](#-why-vanta) • [Windows Setup](#-windows-10--11-installation--launch) • [Linux Setup](#-linux-installation--launch) • [Command Reference](#-approved-command-reference) • [Documentation](IMPLEMENTATION_LOG.md)

</div>

---

## ⚡ Why VANTA?

When administering or interacting with a dedicated Linux workstation, field laptop, lab equipment, or edge device (such as a Raspberry Pi), operators usually face two extremes:

| Approach | Limitations |
| :--- | :--- |
| **Heavy Desktop Environments** *(GNOME, KDE)* | **Excessive Bloat & RAM Usage**: Consumes 1.5GB+ of memory, runs dozens of background indexing daemons, and adds unnecessary visual clutter when you only need operational tooling. |
| **Raw Terminal / Bash** *(SSH, Bash)* | **Fragmented & Catastrophic Risk**: A single accidental typo (like `rm -rf` or misdirected `dd`) can brick a system. Users must constantly juggle 5 separate CLI tools (`htop`, `df`, `free`, `ps`, `ls`). |
| **Web Dashboards** *(Cockpit, Webmin)* | **Heavy Memory & Network Attack Vectors**: Requires running web server daemons, listening on open HTTP ports, and consuming hundreds of megabytes just to render in Chromium. |

### The VANTA Solution
VANTA provides a **lightweight, native, security-bounded "Mission Control" desktop**:
- **Lightweight & Fast**: Boots in seconds on modern Java 21 LTS with hardware-accelerated JavaFX graphics.
- **Safety by Construction**: Prohibits arbitrary shell strings (`sh -c` / `bash -c`). Arguments are tokenized and executed strictly via Java's `ProcessBuilder`.
- **Zero Web Ports**: 100% native desktop application with no HTTP listener or web attack surface.
- **Direct Kernel Telemetry**: High-frequency metric collection reading directly from `/proc` and `/sys` virtual filesystems (with universal JVM fallback on Windows).

---

## 🚀 Key Features

```
┌────────────────────────────────────────────────────────────────────────┐
│                               VANTA SHELL                              │
├─────────────┬──────────────────────────────────────────────────────────┤
│  NAV DOCK   │  MAIN WORKSPACE                                          │
│             │                                                          │
│  [⬚ Overview]│  📁 File Browser       • Safe NIO operations & modals   │
│  [📁 Files]  │  💻 Command Console    • Sandboxed whitelist & history   │
│  [💻 Console]│  📊 System Monitor     • Direct /proc kernel telemetry  │
│  [📊 Monitor]│                                                          │
└─────────────┴──────────────────────────────────────────────────────────┘
```

### 1. 📁 Guarded File Browser
- **Visual Directory Navigation**: Instant tree traversal with editable address bar, Up, Home, and Refresh controls.
- **Asynchronous NIO Engine**: Directory indexing runs in background workers (`javafx.concurrent.Task`), keeping the interface smooth and responsive even across large filesystems.
- **Full File Lifecycle**: Create folders, create files, rename items, and perform safe deletions.
- **Human-in-the-Loop Safety**: Irreversible delete operations require explicit interactive modal confirmation.
- **Color-Coded Metadata**: Highlights directories in bold emerald green (`<DIR>`), formatted sizes (B, KB, MB, GB), timestamps, and POSIX permissions (`rwxr-xr-x`).

### 2. 💻 Sandboxed Command Console
- **No Shell Execution**: Commands bypass `/bin/sh` or `/bin/bash` completely; tokens are passed directly to `ProcessBuilder` to eliminate command injection.
- **Whitelist Enforcement**: Validates commands against an approved diagnostic catalog.
- **Interactive Terminal Layout**: Top command bar with live prompt, `▶ Run` and `■ Cancel` controls, and quick-action chips.
- **Multicolor Terminal Streaming**: Live color-coded output with green usernames, cyan path indicators, white command text, and coral red error alerts.
- **Built-in Virtual Directives**: Instant built-ins including `help`, `cd`, `pwd`, `history`, `clear`, and `touch`.
- **Interactive Cancellation**: Forcibly terminate running processes with one click (`Process.destroyForcibly()`).

### 3. 📊 Direct Kernel System Monitor
- **CPU Telemetry**: Real-time utilization gauge, core count, architecture, and processor model (`/proc/stat`, `/proc/cpuinfo`).
- **Memory (RAM) Telemetry**: Live memory allocation progress bar showing exact used, free, and available RAM (`/proc/meminfo`).
- **Storage Partitions**: Inspect mounted partitions, filesystem types, total capacities, and free disk space.
- **Process Supervision**: Displays live snapshots of top running processes with PID, user, and command paths.
- **Telemetry Controls**: Real-time 2-second background polling with an interactive **Pause/Resume Telemetry** toggle.

### 4. 🧭 Persistent Shell & Module Framework
- **Decoupled Architecture**: View presentation is completely separated from platform services.
- **Session Memory**: Automatically saves and restores your last open module across sessions (`~/.vanta/preferences.properties`).
- **Dark Operations Palette**: Optimized for low eye strain (`#0f1117` base, `#161922` surface panels, `#38bdf8` cyan accents).

---

## 💻 Operating System Installation Guides

Choose your operating system below for complete, copy-pasteable setup and launch instructions:

---

### 🪟 Windows (10 / 11) Installation & Launch

#### Step 1: Install Java 21 & Git
Open **PowerShell** as Administrator or standard user and run:

```powershell
# Option A: Install via Windows Package Manager (winget) - Recommended
winget install EclipseAdoptium.Temurin.21.JDK
winget install Git.Git

# (Optional) If you don't want to use the included Maven Wrapper, install Maven:
winget install Apache.Maven
```

> **Manual Alternative:** Download and run the official installer from [Eclipse Temurin JDK 21](https://adoptium.net/temurin/releases/?version=21) or [Oracle JDK 21](https://www.oracle.com/java/technologies/downloads/#java21).

Verify that Java 21 is active:
```powershell
java -version
```
*(Ensure it reports `openjdk version "21..."` or `java version "21..."`).*

#### Step 2: Clone the Repository
```powershell
git clone https://github.com/aditya-gitzy/VANTA.git
cd VANTA
```

#### Step 3: Build & Package
You can build using the **included Maven Wrapper** (no separate Maven install required) or global Maven:

```powershell
# Using the included Maven Wrapper (PowerShell)
.\mvnw.cmd clean package

# Or if you have Apache Maven installed globally:
mvn clean package
```

The standalone executable fat JAR will be generated at:
```
target\vanta-1.0.0-SNAPSHOT.jar
```

#### Step 4: Launch VANTA on Windows

```powershell
# Launch the packaged standalone application (Instant GUI)
java -jar target\vanta-1.0.0-SNAPSHOT.jar

# Or run directly in development mode:
.\mvnw.cmd javafx:run
```

---

### 🐧 Linux Installation & Launch

VANTA supports all major Linux distributions and architectures (`x86_64`, `aarch64` / ARM64).

#### Step 1: Install Prerequisites

Select your distribution family:

##### 🔹 Ubuntu / Debian / Linux Mint / Pop!_OS
```bash
sudo apt update
sudo apt install openjdk-21-jdk libgtk-3-0 libgl1-mesa-glx git maven
```

##### 🔹 Raspberry Pi OS (64-bit)
```bash
sudo apt update
sudo apt install openjdk-21-jdk libgtk-3-0 libgl1-mesa-glx git maven
```

##### 🔹 Fedora / RHEL / CentOS Stream / AlmaLinux
```bash
sudo dnf install java-21-openjdk java-21-openjdk-devel gtk3 mesa-libGL git maven
```

##### 🔹 Arch Linux / Manjaro / EndeavourOS
```bash
sudo pacman -S jdk21-openjdk gtk3 mesa git maven
```

##### 🔹 openSUSE (Tumbleweed / Leap)
```bash
sudo zypper install java-21-openjdk java-21-openjdk-devel gtk3 git maven
```

Verify your Java 21 environment:
```bash
java -version
```

#### Step 2: Clone the Repository
```bash
git clone https://github.com/aditya-gitzy/VANTA.git
cd VANTA
```

#### Step 3: Build & Package
```bash
# Make the wrapper executable (if using wrapper)
chmod +x mvnw

# Run automated unit tests and generate the standalone Fat JAR
./mvnw clean package

# (Or run with system maven)
mvn clean package
```

The resulting standalone executable JAR is located at:
```
target/vanta-1.0.0-SNAPSHOT.jar
```

#### Step 4: Launch VANTA on Linux

```bash
# Option A: Run the standalone executable JAR directly
java -jar target/vanta-1.0.0-SNAPSHOT.jar

# Option B: Run with Maven in development mode
./mvnw javafx:run
```

---

### 🍎 macOS (Apple Silicon M1/M2/M3 & Intel)

```bash
# 1. Install prerequisites via Homebrew
brew install openjdk@21 maven git

# 2. Clone and navigate
git clone https://github.com/aditya-gitzy/VANTA.git
cd VANTA

# 3. Build & Package
mvn clean package

# 4. Launch
java -jar target/vanta-1.0.0-SNAPSHOT.jar
```

---

## 🔒 Security & Isolation Model

VANTA is built with a defense-in-depth posture:

1. **Strict Metacharacter Sanitization**:
   Any attempt to inject shell operators (`;`, `&`, `|`, `>`, `<`, `` ` ``, `$()`, `${}`) is intercepted before process invocation:
   ```
   vanta:[Downloads] $ whoami ; id
   [Error] Command rejected: Shell metacharacters are strictly prohibited.
   ```
2. **Privilege Containment**:
   All processes and filesystem actions execute under the standard logged-in user session. The application explicitly removes `SUDO_USER` environment variables and prohibits automatic root privilege elevation.
3. **No Dynamic Third-Party Plugins**:
   All modules are strictly compiled into the verified core application binary, eliminating risks of untrusted remote code execution.

---

## 📋 Approved Command Reference

VANTA includes an authorized catalog of system inspection tools:

| Command | Category | Purpose | Example |
| :--- | :--- | :--- | :--- |
| `help` | Built-in | Display complete command directory and syntax | `help` |
| `cd` | Built-in | Change working directory (`..`, `~`, or relative path) | `cd ..` |
| `pwd` | Built-in | Print current working directory | `pwd` |
| `clear` / `cls`| Built-in | Clear terminal display buffer | `clear` |
| `history` | Built-in | Display numbered history of executed commands | `history` |
| `touch` | Files | Create new file or update timestamp | `touch note.txt` |
| `dir` / `ls` | Files | List directory contents with details | `ls -la` / `dir` |
| `cat` / `type`| Files | Output file contents to console | `cat /etc/os-release` |
| `head` / `tail`| Files | Inspect top or bottom of log files | `head -n 20 file.txt` |
| `mkdir` | Files | Create new directory | `mkdir my_folder` |
| `grep` / `findstr`| Utilities | Search for patterns in files | `grep error app.log` |
| `wc` | Utilities | Word, line, and byte counter | `wc -l file.txt` |
| `uname` | System | Print OS name and kernel architecture | `uname -a` |
| `whoami` | Identity | Print current logged-in username | `whoami` |
| `id` | Identity | Print user and group identifiers | `id` |
| `uptime` | System | Show how long the system has been running | `uptime` |
| `date` | System | Display or format current date and time | `date` |
| `df` | Storage | Report filesystem disk space usage | `df -h` |
| `free` | Memory | Display amount of free and used memory | `free -m` |
| `ps` | Process | Report snapshot of current processes | `ps aux` |
| `ping` | Network | Send ICMP ECHO_REQUEST to network hosts | `ping 127.0.0.1` |
| `systeminfo` | System | Windows OS configuration (dev host) | `systeminfo` |
| `ipconfig` | Network | Network adapter configuration | `ipconfig` |
| `netstat` | Network | Active TCP connections and ports | `netstat -an` |
| `tasklist` | Process | Active Windows processes (dev host) | `tasklist` |

---

## 🧪 Demonstration & Test Workflow

Follow this 5-minute tour to test all subsystems:

1. **Launch the Application**: Run `java -jar target/vanta-1.0.0-SNAPSHOT.jar` (or `target\vanta-1.0.0-SNAPSHOT.jar` on Windows).
2. **Explore Overview**: Click the interactive cards to jump directly into **Files**, **Console**, or **Monitor**.
3. **Test the Console**:
   - Type `help` and hit `Enter` to see the colored command suite.
   - Run `whoami` and `uptime` (or `systeminfo` on Windows).
   - Run `ping 127.0.0.1` and click **`■ Cancel`** to test background cancellation.
   - Try typing `calc` or `whoami ; id` to verify sandbox error blocking.
4. **Test the File Browser**:
   - Double-click folders to navigate.
   - Click `+ New Folder` → create `vanta_test`.
   - Click `+ New File` inside it → create `test.txt`.
   - Select `test.txt` and click `Delete` → verify the confirmation dialog.
5. **Inspect Hardware Telemetry**:
   - Switch to **Monitor** to see real-time CPU & RAM gauges.
   - Click **`⏸ Pause Telemetry`** and **`▶ Resume Telemetry`**.
6. **Verify Session Persistence**:
   - Close the app while in **Monitor**, relaunch it, and observe that it reopens on **Monitor** automatically.

---

## 📂 Project Structure

```
VANTA/
├── mvnw                                      # Linux/macOS Maven Wrapper script
├── mvnw.cmd                                  # Windows Maven Wrapper script
├── pom.xml                                   # Maven project & shade packaging definition
├── README.md                                 # Project documentation & user guide
├── IMPLEMENTATION_LOG.md                     # Phase-by-phase implementation audit log
├── LICENSE                                   # MIT License
└── src/
    ├── main/
    │   ├── java/dev/vanta/
    │   │   ├── Launcher.java                 # Classpath launcher
    │   │   ├── VantaApp.java                 # JavaFX application entry point & bootstrap
    │   │   ├── module/
    │   │   │   ├── VantaModule.java          # Module contract interface
    │   │   │   └── ModuleRegistry.java       # Compile-time module registry
    │   │   ├── shell/
    │   │   │   └── ShellView.java            # Shell layout (Header, Sidebar, Workspace)
    │   │   ├── modules/
    │   │   │   ├── overview/OverviewModule.java
    │   │   │   ├── files/FilesModule.java
    │   │   │   ├── console/ConsoleModule.java
    │   │   │   └── monitor/MonitorModule.java
    │   │   └── service/
    │   │       ├── fs/                       # NIO Filesystem services
    │   │       │   ├── FileItem.java
    │   │       │   └── FileService.java
    │   │       ├── process/                  # Sandboxed ProcessBuilder execution
    │   │       │   ├── CommandSpec.java
    │   │       │   └── ProcessService.java
    │   │       ├── platform/                 # Telemetry & Linux /proc adapters
    │   │       │   ├── PlatformAdapter.java
    │   │       │   ├── LinuxPlatformAdapter.java
    │   │       │   ├── GenericPlatformAdapter.java
    │   │       │   ├── PlatformService.java
    │   │       │   └── ... (Metrics records)
    │   │       └── pref/
    │   │           └── PreferenceService.java # Local user settings persistence
    │   └── resources/
    │       └── styles/
    │           └── theme.css                 # Modern dark theme stylesheet
    └── test/
        └── java/dev/vanta/
            ├── VantaAppTest.java
            ├── module/ModuleRegistryTest.java
            ├── service/fs/FileServiceTest.java
            ├── service/process/ProcessServiceTest.java
            └── service/platform/PlatformServiceTest.java
```

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

Copyright (c) 2026 **Aditya Lande**.
