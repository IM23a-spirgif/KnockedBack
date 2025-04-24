package net.fretux.knockedback.client;

public class ClientExecutionState {
    private static int timeLeft = 0;

    public static void setTimeLeft(int ticks) {
        timeLeft = ticks;
    }

    public static int getTimeLeft() {
        return timeLeft;
    }

    public static boolean isExecuting() {
        return timeLeft > 0;
    }
}