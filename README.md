# Software engineering project – Polytechnic University of Milan, 2025/2026

## Maven project

The Maven project is inside the `Mesos` directory. Run Maven commands from there:

```bash
cd Mesos
```

## Build runnable jars

From `Mesos`, build the project with:

```bash
./mvnw -DskipTests package
```

or for the GUI:

```bash
./mvnw -q -Dmaven.test.skip=true package
```

This creates the main project jar and three runnable jars:

- `Mesos/target/Mesos-1.0-final.jar`
- `Mesos/target/Mesos-1.0-final-server.jar`
- `Mesos/target/Mesos-1.0-final-gui.jar`
- `Mesos/target/Mesos-1.0-final-cli.jar`

You can double-click the runnable client and server jars to open a small game window, or run them from a terminal.

Open the server jar first:

```bash
java -jar target/Mesos-1.0-final-server.jar
```

Then open the client jar:

```bash
java -jar target/Mesos-1.0-final-gui.jar
```

To launch the terminal CLI instead, run:

```bash
java -jar target/Mesos-1.0-final-cli.jar
```

By default the server listens on socket port `12345` and RMI port `1099`.

## Build Javadocs

From `Mesos`, generate the project Javadocs with:

```bash
./mvnw javadoc:javadoc
```

The generated documentation entry point is:

```text
Mesos/target/reports/apidocs/index.html
```
