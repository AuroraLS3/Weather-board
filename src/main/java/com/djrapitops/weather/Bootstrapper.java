package com.djrapitops.weather;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

/**
 * Behavior to begin the process.
 *
 * @author Rsl1122
 */
public class Bootstrapper extends AbstractBehavior<Bootstrapper.Initialize> {

    public Bootstrapper(ActorContext<Initialize> context) {
        super(context);
    }

    public static Behavior<Initialize> create() {
        return Behaviors.setup(Bootstrapper::new);
    }

    @Override
    public Receive<Initialize> createReceive() {
        return newReceiveBuilder()
                .onMessage(Initialize.class, this::onInitialize)
                .build();
    }

    private Behavior<Initialize> onInitialize(Initialize command) {
        return this;
    }

    public static class Initialize {
    }
}