package net.fretux.knockedback.client;

public class ClientKnockedState {
    private static int timeLeft = 0;

    public static void setTimeLeft(int ticks) {
        timeLeft = ticks;
    }

    public static int getTimeLeft() {
        return timeLeft;
    }

    public static boolean isKnocked() {
        return timeLeft > 0;
    }
}