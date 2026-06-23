# Mesos — Software Engineering Final Project (Politecnico di Milano, 2025/2026)

A networked, multiplayer software implementation of the **Mesos** board game,
built with Java SE and the Model–View–Controller (MVC) pattern. The system is a
client–server distributed application: one server process hosts the games, and
each player runs their own client (textual or graphical), connecting over Socket
(TCP/IP) or RMI.

**Team:** Roda · Rota · Sandrini · Tagako

---

## What's under `deliverables/`

| Path | Contents                                                                                                                                                                     |
|------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `deliverables/jars/` | The three self-contained executable jars: `Mesos-server.jar`, `Mesos-gui.jar` (GUI client), `Mesos-cli.jar` (TUI client). These are what you run — see §3.                   |
| `deliverables/javadoc/` | Generated API documentation. Open `deliverables/javadoc/index.html` in a browser.                                                                                            |
| `deliverables/UML-Diagrams/Sequence-Diagrams/` | Sequence diagrams (SVG) for the main networking/multiplayer scenarios — connection, lobby, offer/card phases, disconnection & reconnection, multiple simultaneous games, etc. |
| `deliverables/UML-Diagrams/Structural-Diagrams/` | Structural (class) diagrams (SVG) of the system, broken down by package (`model`, `controller`, `network`, `view`, `dto`).                                                    |
| `deliverables/Testing/` | Test-coverage results: the full HTML report (`Coverage-Report/index.html`) and a summary screenshot — see §5.                                                               |

---

## 1. What we implemented

This section maps the project onto the official grading table (*Tabella 1*,
page 5 of `requirements_mesos.pdf`).

### Implementation status

| Area | Item | Implemented |
|------|------|:-----------:|
| **Rules** | Complete rules (character cards — inventors & artists — and the cave-paintings event, 2–5 players) | ✅ |
| **Client interfaces** | TUI (command-line client) | ✅ |
| | GUI (JavaFX client) | ✅ |
| **Network** | Socket (TCP/IP) | ✅ |
| | RMI | ✅ |
| | Mixed protocol (Socket and RMI players in the same game) | ✅ |
| **Advanced functionality** | Multiple simultaneous games | ✅ |
| | Game rankings on database | ✅ |
| | Resilience to disconnections | ✅ |
| | Persistence | ❌ |

### Grading-table coverage (*Tabella 1*)

The rows whose requirements are fully satisfied by this delivery are marked ✅
(FA = advanced functionality):

| Requirements | Satisfied |
|--------------|:---------:|
| Simplified rules + TUI + RMI or Socket | ✅ |
| Complete rules + TUI + RMI or Socket | ✅ |
| Complete rules + TUI + RMI or Socket + 1 FA | ✅ |
| Complete rules + TUI + GUI + RMI or Socket + 1 FA | ✅ |
| Complete rules + TUI + GUI + RMI + Socket + 1 FA | ✅ |
| Complete rules + TUI + GUI + RMI + Socket + 2 FA | ✅ |
| Complete rules + TUI + GUI + RMI + Socket + 3 FA | ✅ |

In short: **complete rules**, **both** client interfaces (TUI + GUI), **both**
transports (Socket + RMI, including mixed-protocol games), and **three** advanced
functionalities (multiple simultaneous games, database-backed rankings, and
resilience to disconnections).

---

## 2. Prerequisites

- A **Java 23** runtime (JRE/JDK 23 or newer) on the `PATH`.

The deliverable jars under `deliverables/` are **self-contained executable jars**:
JavaFX, Jackson and the database driver are bundled inside them. You do **not**
need Maven or a separate JavaFX SDK to run them — just `java`.

Check your Java version:

```bash
java -version
```

---

## 3. Running the application (deliverable jars)

All commands below are run from the **repository root**, using the jars in
`deliverables/jars/`:

- `deliverables/jars/Mesos-server.jar` — the server
- `deliverables/jars/Mesos-gui.jar` — the graphical (GUI) client
- `deliverables/jars/Mesos-cli.jar` — the textual (TUI/CLI) client

> **Choosing the interface** is simply a matter of which client jar you launch
> (`Mesos-gui.jar` vs `Mesos-cli.jar`).
> **Choosing the transport** (Socket or RMI) is done inside the client, at startup.

### 3.1 Start the server

```bash
java -jar deliverables/jars/Mesos-server.jar
```

The server starts **both** transports at once and prints the host and ports it is
listening on, for example:

```
Socket server listening on port 12345
RMI registry listening on port 1099 with service 'MesosGameService' on host 192.168.1.10
```

Default ports: **Socket `12345`**, **RMI registry `1099`**.

The server also creates a `game_leaderboard.db` file (the rankings database) in
the directory where you launch it.

**Optional arguments** (positional — provide them in order, only as far as you
need):

```bash
java -jar deliverables/jars/Mesos-server.jar [socketPort] [rmiPort] [rmiServiceName] [rmiHost]
```

| Position | Argument | Default |
|:--------:|----------|---------|
| 1 | Socket port | `12345` |
| 2 | RMI registry port | `1099` |
| 3 | RMI service name | `MesosGameService` |
| 4 | Advertised RMI host | auto-detected LAN IPv4 address |

> The clients connect on the **standard ports** (`12345` / `1099`). For simple
> testing, keep the server on its default ports.

### 3.2 Start a GUI client

```bash
java -jar deliverables/jars/Mesos-gui.jar
```

On the start screen, pick the **transport** (Socket or RMI) and the **server
host** (defaults to `127.0.0.1`), then connect and choose your nickname.

### 3.3 Start a CLI client

```bash
java -jar deliverables/jars/Mesos-cli.jar
```

The client prompts, in order, for:

1. the **transport** (Socket or RMI),
2. the **server host** (e.g. `localhost` / `127.0.0.1`, or the LAN IP printed by
   the server),
3. your **nickname**.

You can mix clients freely: in the same game, some players may use the GUI and
others the CLI, and some may connect over Socket while others use RMI.

---

## 4. Testing tips

> ⚠️ **LAN only.** The game is meant to be played over a **local network (LAN)**;
> public Wi-Fi networks are not tested and not required. **Turn off any firewall
> and disconnect any VPN** on every machine before connecting, as they can
> silently block the connection (especially RMI).

### 4.1 Running games

- **All on one machine:** start the server, then launch several clients
  (any mix of `Mesos-gui.jar` and `Mesos-cli.jar`), all connecting to
  `localhost` / `127.0.0.1`. You can run as many clients as players.
- **Across a LAN:** start the server on one machine and use the **LAN IP address
  it prints at startup** as the host on the other machines' clients.
- **Mixed protocol:** connect some clients via Socket and others via RMI to the
  same game — both are supported simultaneously.
- **Players & game start:** games support **2–5 players**. The first player to
  create a game chooses how many players it will have; the game starts
  automatically once that many players have joined.
- **Multiple games:** the lobby lists the open games; players can create a new
  game or join an existing one that has not started yet.
- **Disconnection & reconnection:** if a client drops (network loss or crash),
  the game continues and skips that player's turns. The player can **reconnect
  using the same nickname** to resume. If only one player remains connected, the
  game is suspended until another player reconnects or a **60-second** timeout
  awards the win to the last remaining player.
- **Rankings:** completed games are recorded in a leaderboard, grouped by number
  of players; it can be consulted from the lobby and is shown at the end of a game.

### 4.2 Network scenarios (sequence diagrams)

The main networking and multiplayer scenarios are documented as sequence diagrams
under `deliverables/UML-Diagrams/Sequence-Diagrams/`.

---

## 5. Test coverage

The test suite thoroughly covers the application's core logic — `model`,
`controller`, `network`, and `database`; the thin `gui` and `launcher` layers are
tested manually, hence their lower automated coverage.

<img src="deliverables/Testing/Test-Coverage-Screenshot.png" alt="Test coverage by package" width="900">

The full interactive report is available at
`deliverables/Testing/Coverage-Report/index.html`.

---

## 6. Building from source (optional)

The Maven project lives in the `Mesos/` directory. To rebuild the executable jars
yourself:

```bash
cd Mesos
./mvnw clean package
```

This produces, under `Mesos/target/`:

- `Mesos-server.jar` — server
- `Mesos-gui.jar` — GUI client
- `Mesos-cli.jar` — CLI client

To generate the
Javadoc:

```bash
cd Mesos
./mvnw javadoc:javadoc
```

The documentation entry point is `Mesos/target/reports/apidocs/index.html`.

These are the same artifacts published under `deliverables/`. 
