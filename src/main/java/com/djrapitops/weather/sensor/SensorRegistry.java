package com.djrapitops.weather.sensor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.UUID;

public class SensorRegistry extends AbstractBehavior<SensorRegistry.Command> {

    public SensorRegistry(ActorContext<Command> context) {
        super(context);
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(SensorRegistry::new);
    }

    /* ----------------------------------------------------- */

    public interface Command {}

    public static final class Register implements Command, SensorGroup.Command {
        final UUID groupId;
        final UUID deviceId;
        final ActorRef<Registered> replyTo;

        public Register(UUID groupId, UUID deviceId, ActorRef<Registered> replyTo) {
            this.groupId = groupId;
            this.deviceId = deviceId;
            this.replyTo = replyTo;
        }
    }

    public static final class Registered implements Command {
        final ActorRef<TemperatureSensor.Command> sensor;

        public Registered(ActorRef<TemperatureSensor.Command> sensor) {
            this.sensor = sensor;
        }
    }

    /* ----------------------------------------------------- */

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .build();
    }
}