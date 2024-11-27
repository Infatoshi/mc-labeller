package com.example.network;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.example.ExampleMod;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ActionReceiver {
    private static final int PORT = 12345;
    private static final ConcurrentHashMap<String, Object> currentActions = new ConcurrentHashMap<>();
    private static final Gson gson = new Gson();
    private static boolean isRunning = true;
    private static ServerSocket serverSocket;
    private static long lastMessageTime = 0;
    private static final long CONNECTION_TIMEOUT_MS = 1000; // 1 second timeout

    public static void startListener() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                ExampleMod.LOGGER.info("Action receiver listening on port " + PORT);
                
                while (isRunning) {
                    try (Socket clientSocket = serverSocket.accept()) {
                        clientSocket.setTcpNoDelay(true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        
                        ExampleMod.LOGGER.info("Python client connected");
                        lastMessageTime = System.currentTimeMillis();
                        
                        String inputLine;
                        while (isRunning && (inputLine = in.readLine()) != null) {
                            processActions(inputLine);
                        }
                    } catch (Exception e) {
                        if (isRunning) {
                            ExampleMod.LOGGER.error("Client disconnected or error occurred", e);
                            currentActions.clear(); // Clear actions on disconnect
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                ExampleMod.LOGGER.error("Failed to start action receiver", e);
            }
        }, "ActionReceiver").start();
    }

    public static boolean shouldActivate(String action) {
        // Check if we haven't received a message in the timeout period
        if (System.currentTimeMillis() - lastMessageTime > CONNECTION_TIMEOUT_MS) {
            if (!currentActions.isEmpty()) {
                currentActions.clear();
                ExampleMod.LOGGER.info("Connection timeout - cleared all actions");
            }
            return false;
        }
        Object value = currentActions.getOrDefault(action, 0);
        return value instanceof Number && ((Number) value).intValue() == 1;
    }

    public static void shutdown() {
        isRunning = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (Exception e) {
            ExampleMod.LOGGER.error("Error closing server socket", e);
        }
    }

    private static void processActions(String inputLine) {
        try {
            Map<String, Integer> actions = gson.fromJson(inputLine, 
                new TypeToken<HashMap<String, Integer>>(){}.getType());
            currentActions.clear();
            currentActions.putAll(actions);
            lastMessageTime = System.currentTimeMillis();
            ExampleMod.LOGGER.debug("Received actions: " + actions);
        } catch (Exception e) {
            ExampleMod.LOGGER.error("Error parsing actions", e);
        }
    }

    public static float getFloatAction(String action, float defaultValue) {
        Object value = currentActions.get(action);
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return defaultValue;
    }
} 