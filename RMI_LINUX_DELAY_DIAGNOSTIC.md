# RMI delay observed with a Linux client

## Situation

During a GUI game over the same hotspot, macOS clients worked normally with both Socket and RMI. When a Linux client joined through RMI, the server and the other players already saw that player in the game, but the Linux GUI stayed on the joining/busy phase for several minutes. Later, choices and board updates reached that Linux client very late unless the player clicked `Refresh`, which fetched the latest state immediately.

This pattern strongly suggests that normal client-to-server requests were working, but the live-update path from the server back to the Linux client was delayed or blocked.

## Most likely cause

The RMI implementation uses callbacks for live updates:

- `RmiGameClient.subscribe(...)` exports a local `RemoteGameUpdateListener`.
- The client sends that exported listener to the server.
- The server stores it as a subscriber.
- When something changes, `MesosServerCore.publish(...)` calls `subscriber.accept(update)`.
- For RMI subscribers, that eventually calls `RemoteGameUpdateListener.onUpdate(...)` on the client machine.

So RMI is not only:

```text
client -> server
```

It is also:

```text
server -> client callback
```

That second direction is the fragile part. A Linux firewall, wrong advertised client address, Docker/VPN interface, IPv6/IPv4 mismatch, or dropped inbound packets can make the callback connection hang until the operating system TCP timeout expires. A timeout of 2-4 minutes is very realistic when packets are dropped instead of rejected.

Manual refresh works because it is a normal request from the Linux client to the server:

```text
Linux client -> server -> Linux client response
```

It does not depend on the server opening a fresh callback connection toward the Linux client.

This means the problem can absolutely be on the client PC and not necessarily in the game logic. The code may be doing a standard RMI callback setup, while the Linux machine blocks or advertises that callback endpoint incorrectly. In that case, the application behavior would look broken even though the server and game state are updating correctly.

Client-side causes to suspect first:

- Linux firewall blocks inbound Java/RMI callback connections.
- The JVM advertises `127.0.0.1`, `127.0.1.1`, a Docker bridge address, a VPN address, or an IPv6 address instead of the hotspot/LAN address.
- The machine has multiple active network interfaces and Java RMI chooses the wrong one.
- The hotspot allows Linux-to-server traffic but delays or blocks server-to-Linux traffic.
- Security software or system policy silently drops inbound TCP packets, causing long TCP timeouts.

## Why the hotspot is probably not the main problem

The amount of data exchanged by the game is small: serialized requests, operation results, and game snapshots. Even a weak local Wi-Fi/hotspot setup should not turn these messages into 3-4 minute delays if basic connectivity is healthy.

The hotspot could still be involved if it applies client isolation, unstable routing, or aggressive packet filtering between connected devices. However, because the other clients were working and because refresh was instant, pure bandwidth overload is unlikely. The symptoms match a blocked or misrouted RMI callback much better than an overloaded 5G hotspot.

## Code details that support this diagnosis

Relevant files:

- `Mesos/src/main/java/it/polimi/ingsw/mesos/Network/RMI/RmiGameClient.java`
- `Mesos/src/main/java/it/polimi/ingsw/mesos/Network/RMI/RmiGameServer.java`
- `Mesos/src/main/java/it/polimi/ingsw/mesos/Network/MesosServerCore.java`

Important details:

- `RemoteGameUpdateListenerAdapter` extends `UnicastRemoteObject` with `super()`, so Java RMI chooses an anonymous callback port on the client.
- That anonymous callback port must be reachable from the server.
- The Linux client does not appear to configure `java.rmi.server.hostname`, so the JVM may advertise an address that is not reachable from the server, especially on Linux machines with Docker, VPNs, multiple network interfaces, IPv6, or unusual hostname mappings.
- `MesosServerCore.publish(...)` sends updates synchronously to subscribers. If one subscriber callback blocks on network timeout, the server-side operation can also wait until the callback fails.
- Socket mode does not have this same reverse-callback problem because the client keeps one TCP socket open to the server and receives updates on that connection.

## Things to test

### 1. Confirm it is specific to RMI callbacks

Run the same Linux machine using Socket instead of RMI.

Expected result:

- If Socket works normally but RMI is delayed, the problem is almost certainly the RMI callback path.
- If Socket is also delayed, investigate hotspot routing, Wi-Fi quality, JVM performance, or general network instability.

### 2. Check the Linux firewall

On Linux:

```bash
sudo ufw status verbose
sudo firewall-cmd --state 2>/dev/null
sudo iptables -S
```

As a temporary test only, disable the firewall and retry RMI:

```bash
sudo ufw disable
```

or, on firewalld systems:

```bash
sudo systemctl stop firewalld
```

If RMI starts working immediately, the callback port is being blocked. Re-enable the firewall after the test.

### 3. See which ports Java opens on the Linux client

Start the Linux client, connect with RMI, then run:

```bash
ss -ltnp | grep java
```

Look for listening Java ports besides the connection to the server. Because the callback listener is exported with an anonymous port, the port may change each run.

This is also why firewall rules are hard with the current implementation: you cannot simply allow one fixed RMI callback port unless the code is changed to export the listener on a fixed port.

### 4. Force the Linux client to advertise the correct hotspot IP

Find the Linux client's hotspot IP:

```bash
ip -4 addr
```

Then launch the client with the client-side RMI hostname explicitly set:

```bash
java -Djava.rmi.server.hostname=<LINUX_HOTSPOT_IP> \
     -jar target/Mesos-1.0.0-final-client.jar
```

Replace `<LINUX_HOTSPOT_IP>` with the address visible from the host/server machine, for example `172.20.10.5` or `192.168.x.y`.

If this fixes the delay, the Linux JVM was probably advertising an unreachable address for its RMI callback object.

### 5. Check whether the server can reach the Linux client

While the Linux client is running and subscribed, identify the callback listening port with:

```bash
ss -ltnp | grep java
```

From the server Mac, test connectivity to that Linux IP and port:

```bash
nc -vz <LINUX_HOTSPOT_IP> <CALLBACK_PORT>
```

If this hangs or times out, the server cannot reach the RMI callback listener.

### 6. Watch for blocked TCP connections

On the server Mac during the delay:

```bash
lsof -nP -iTCP -sTCP:SYN_SENT | grep java
```

On the Linux client:

```bash
ss -tanp | grep java
```

Connections stuck in `SYN-SENT` or similar states are evidence that one side is trying to open a TCP connection and receiving no answer.

### 7. Compare with a local LAN instead of hotspot

Repeat the same test on a normal Wi-Fi router or wired LAN.

Expected result:

- If RMI works on LAN but not on hotspot, the hotspot is filtering or isolating devices.
- If RMI is still delayed only for Linux, focus on Linux firewall/address advertisement.

## Possible fixes

### Operational fixes without changing code

Try these first:

1. Use Socket mode for Linux clients.
2. Launch the Linux client with `-Djava.rmi.server.hostname=<LINUX_HOTSPOT_IP>`.
3. Temporarily disable firewall rules during testing.
4. Avoid VPNs, Docker bridge interference, or multiple active network interfaces while testing RMI.
5. Use a normal LAN instead of phone hotspot if the hotspot blocks device-to-device traffic.

### Code-level fixes for later

These would require project changes, so they are only recommendations:

1. Export the RMI client callback listener on a fixed configurable port instead of the anonymous `super()` port.
2. Document that RMI clients need inbound access to that callback port.
3. Add a GUI or CLI setting for the client's advertised RMI hostname.
4. Send live updates asynchronously on the server so one slow callback cannot block a game operation.
5. Add logging when an RMI callback fails or times out, because right now callback failures are silently removed after `IOException`.
6. Consider preferring the socket transport for real multi-machine play, because it avoids RMI's bidirectional callback/firewall complexity.

## Short conclusion

The hotspot being overloaded is possible but unlikely. Three or four minutes of delay for tiny game messages is much more consistent with Java RMI trying to call back into the Linux client through an unreachable address or blocked anonymous callback port. The fastest confirmation test is: run the Linux client in Socket mode, then retry RMI with the Linux firewall disabled or with `-Djava.rmi.server.hostname=<LINUX_HOTSPOT_IP>`.
