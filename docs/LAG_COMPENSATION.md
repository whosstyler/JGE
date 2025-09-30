# Lag Compensation System

## Overview

Server-side lag compensation with time rewind for fair hit validation. This system allows players with high ping to have their hits validated fairly by rewinding the game state to when they fired.

## How It Works

1. **Client sends timestamp**: When a player performs an action (e.g., shoots), the client sends their local timestamp with the input packet
2. **Server rewinds time**: Server rewinds all other players to that historical timestamp (within limits)
3. **Hit validation**: Server performs hit detection at that rewound state
4. **Restore**: Server restores all players to present time

## Architecture

### Core Components

#### 1. **PlayerInputPacket** (`shared/network/PlayerInputPacket.java`)
- Added `clientTimestamp` field
- Automatically sent by client with current time: `System.currentTimeMillis()`

#### 2. **Player State History** (`server/entity/Player.java`)
- Stores last 20 ticks of player state (1000ms at 20Hz)
- `saveStateSnapshot()`: Called every tick to save position/velocity/rotation
- `getStateAtTime(long timestamp)`: Retrieves historical state closest to timestamp

#### 3. **LagCompensation** (`server/combat/LagCompensation.java`)
Main lag compensation manager:
- `rewindToTimestamp()`: Rewinds all players (except shooter) to timestamp
- `restoreToPresent()`: Restores players after validation
- `performLagCompensatedRaycast()`: Performs hit check with auto rewind/restore
- `calculateRayDirection()`: Helper to convert yaw/pitch to ray vector
- `getEyePosition()`: Helper to get player eye position

#### 4. **HitValidationExample** (`server/combat/HitValidationExample.java`)
Example implementation showing how to:
- Validate hitscan weapons (guns, lasers)
- Validate projectiles (rockets, arrows)
- Apply damage based on hit results

## Configuration

In `ServerConfig.java`:
```java
public static final int MAX_REWIND_TIME_MS = 200;    // Max 200ms rewind
public static final int STATE_HISTORY_SIZE = 20;     // 20 ticks = 1000ms history
```

## Usage Example

### Basic Hitscan Weapon

```java
import com.gameengine.server.combat.LagCompensation;

// In your packet handler when receiving a "shoot" action
public void handlePlayerShoot(int playerId, long clientTimestamp) {
    Player shooter = players.get(playerId);

    // Get shooter's view position and direction
    Vector3f eyePos = LagCompensation.getEyePosition(shooter);
    Vector3f rayDir = LagCompensation.calculateRayDirection(
        shooter.getYaw(),
        shooter.getPitch()
    );

    // Perform lag-compensated raycast
    LagCompensation lagComp = new LagCompensation(players);
    RaycastHit hit = lagComp.performLagCompensatedRaycast(
        shooter,
        eyePos,
        rayDir,
        100.0f,  // max range
        clientTimestamp
    );

    if (hit != null) {
        Player target = (Player) hit.getCollider().getUserData();
        System.out.println("Hit player " + target.getId() + " at distance " + hit.getDistance());
        // Apply damage...
    }
}
```

### Manual Rewind/Restore

```java
LagCompensation lagComp = new LagCompensation(players);

// Rewind all players except shooter
lagComp.rewindToTimestamp(clientTimestamp, shooterId);

try {
    // Do your custom hit detection here
    // Players are at historical positions

} finally {
    // ALWAYS restore in finally block
    lagComp.restoreToPresent();
}
```

## Why This Matters

### Without Lag Compensation:
- Player with 100ms ping sees enemy at position A
- Player shoots at position A
- By the time server receives shot, enemy is at position B (100ms later)
- Shot misses even though player aimed correctly

### With Lag Compensation:
- Player with 100ms ping sees enemy at position A
- Player shoots at position A, sends timestamp T
- Server rewinds enemy to position A (at time T)
- Server validates hit at position A
- Player gets fair hit detection

## Implementation Details

### State Storage
- Every tick (50ms at 20Hz), player state is saved to `stateHistory` deque
- Stores: timestamp, position, velocity, yaw, pitch, onGround
- Limited to last 20 snapshots (1000ms of history)

### Rewind Limits
- Maximum rewind: 200ms (configurable)
- Prevents abuse from players with extremely high ping
- Clamps timestamp if older than limit

### Thread Safety
- Rewind/restore must happen on main game thread
- No concurrent modifications during rewind window

### Performance
- Minimal overhead: O(n) per rewind where n = number of players
- History storage: ~20 Vector3f per player (~480 bytes per player)

## Testing

To test lag compensation:
1. Add artificial latency to client
2. Fire at moving targets
3. Verify hits register correctly despite latency
4. Check server logs for rewind/restore confirmation

```java
// Add artificial 100ms delay to client packets
Thread.sleep(100);
```

## Advanced Features

### Headshot Detection
```java
if (hit != null) {
    Player target = (Player) hit.getCollider().getUserData();
    float headHeight = target.getPosition().y + ServerConfig.PLAYER_HEIGHT * 0.9f;

    if (hit.getPoint().y >= headHeight) {
        // Headshot! Apply 2x damage
    }
}
```

### Distance Falloff
```java
float baseDamage = 100.0f;
float distance = hit.getDistance();
float damageFalloff = Math.max(0.5f, 1.0f - (distance / maxRange));
float finalDamage = baseDamage * damageFalloff;
```

## Troubleshooting

### Issue: Hits registering behind cover
**Cause**: Player was behind cover in present, but in the open when client fired
**Solution**: Working as intended - this is the tradeoff of lag compensation

### Issue: Players "dying around corners"
**Cause**: Shooter saw victim in open (rewound state), but victim moved to cover
**Solution**: Normal lag compensation behavior. Reduce MAX_REWIND_TIME_MS if too frustrating

### Issue: No hits registering
**Check**:
- Is client sending timestamp? Check packet.getClientTimestamp()
- Is history being saved? Check Player.saveStateSnapshot() called every tick
- Is rewind working? Add debug logs in rewindToTimestamp()

## Further Reading

- Valve's Source Engine Lag Compensation: https://developer.valvesoftware.com/wiki/Lag_compensation
- Overwatch Netcode Analysis: https://www.youtube.com/watch?v=W3aieHjyNvw
- Quake 3 Networking Model: https://fabiensanglard.net/quake3/network.php
