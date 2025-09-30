package com.gameengine.client.player;

import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Manages client-side prediction and server reconciliation
 */
public class PredictionManager {

    private static final Logger logger = LoggerFactory.getLogger(PredictionManager.class);

    private final Deque<PendingInput> pendingInputs;
    private int inputSequence;

    // Interpolation
    private long lastUpdateTime;
    private float renderAlpha;

    public PredictionManager() {
        this.pendingInputs = new ArrayDeque<>();
        this.inputSequence = 0;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * Get next input sequence number
     */
    public int getNextSequence() {
        return ++inputSequence;
    }

    /**
     * Store a pending input for prediction reconciliation
     */
    public void addPendingInput(int sequence, float moveX, float moveY, float moveZ, float yaw, float pitch, float deltaTime) {
        PendingInput pendingInput = new PendingInput(sequence, moveX, moveY, moveZ, yaw, pitch, deltaTime);
        pendingInputs.addLast(pendingInput);

        // Limit pending inputs queue size
        while (pendingInputs.size() > 100) {
            pendingInputs.removeFirst();
        }
    }

    /**
     * Update prediction and reconcile with server state
     */
    public void update(float deltaTime, ClientPlayer localPlayer) {
        if (localPlayer == null) return;

        // Reconcile prediction with server state
        int lastProcessedInput = localPlayer.getLastReceivedInputSequence();

        // Remove inputs that have been processed by the server
        while (!pendingInputs.isEmpty() && pendingInputs.peekFirst().sequence <= lastProcessedInput) {
            pendingInputs.removeFirst();
        }

        // Re-apply pending inputs on top of server state
        if (!pendingInputs.isEmpty()) {
            Vector3f predicted = new Vector3f(localPlayer.getPosition());

            for (PendingInput input : pendingInputs) {
                predicted.add(
                    input.moveX * input.deltaTime,
                    input.moveY * input.deltaTime,
                    input.moveZ * input.deltaTime
                );
            }

            localPlayer.getPredictedPosition().set(predicted);
        } else {
            localPlayer.getPredictedPosition().set(localPlayer.getPosition());
        }

        // Interpolation for other players
        long currentTime = System.currentTimeMillis();
        float interpolationDelay = 100.0f; // 100ms behind for smooth interpolation
        renderAlpha = Math.min(1.0f, (currentTime - lastUpdateTime) / interpolationDelay);
    }

    /**
     * Stores pending input for client-side prediction reconciliation
     */
    private static class PendingInput {
        int sequence;
        float moveX, moveY, moveZ;
        float yaw, pitch;
        float deltaTime;

        PendingInput(int sequence, float moveX, float moveY, float moveZ, float yaw, float pitch, float deltaTime) {
            this.sequence = sequence;
            this.moveX = moveX;
            this.moveY = moveY;
            this.moveZ = moveZ;
            this.yaw = yaw;
            this.pitch = pitch;
            this.deltaTime = deltaTime;
        }
    }
}
