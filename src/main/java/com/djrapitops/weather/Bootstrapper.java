package com.djrapitops.weather;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.Signal;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.djrapitops.weather.sensor.SensorRegistry;

import java.util.UUID;

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
                .onSignal(PostStop.class, this::onStop)
                .build();
    }

    private <M extends Signal> Behavior<Initialize> onStop(M m) {
        return this;
    }

    private Behavior<Initialize> onInitialize(Initialize command) {
        ActorRef<SensorRegistry.Command> sensorRegistry = getContext().spawn(SensorRegistry.create(), "sensor-registry");

        UUID groupId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        sensorRegistry.tell(new SensorRegistry.Register(groupId, deviceId, sensorRegistry.unsafeUpcast()));
        return this;
    }

    public static class Initialize {
    }
}