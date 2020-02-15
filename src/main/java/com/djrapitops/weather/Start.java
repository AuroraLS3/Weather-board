package com.djrapitops.weather;

import akka.actor.typed.ActorSystem;

import java.util.Scanner;

/**
 * Main class.
 *
 * @author Rsl1122
 */
public class Start {

    public static void main(String[] args) {
        ActorSystem<Bootstrapper.Initialize> system = ActorSystem.create(Bootstrapper.create(), "Bootstrapper");

        system.tell(new Bootstrapper.Initialize());

        runReadCommandLoop();

        system.terminate();
    }

    private static void runReadCommandLoop() {
        try (Scanner in = new Scanner(System.in)) {
            while (true) {
                if (in.hasNextLine()) {
                    String command = in.nextLine();
                    switch (command.toLowerCase().trim()) {
                        case "stop": return;
                        case "quit": return;
                        case "exit": return;
                        default: break;
                    }
                }
            }
        }
    }

}