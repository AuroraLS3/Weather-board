package com.djrapitops.weather;

import akka.actor.typed.ActorSystem;

/**
 * Main class.
 *
 * @author Rsl1122
 */
public class Start {

    public static void main(String[] args) {
        ActorSystem<Bootstrapper.Command> system = ActorSystem.create(Bootstrapper.create(), "Bootstrapper");

        system.tell(new Bootstrapper.Initialize());

    }
}