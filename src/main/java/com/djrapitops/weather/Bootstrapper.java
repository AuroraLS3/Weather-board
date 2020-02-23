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
public class Bootstrapper extends AbstractBehavior<Bootstrapper.Command> {

    public Bootstrapper(ActorContext<Command> context) {
        super(context);
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(Bootstrapper::new);
    }

    public interface Command {}

    public static class Initialize implements Command {
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(Initialize.class, this::onInitialize)
                .onMessage(SystemInReader.Publish.class, this::onCmd)
                .onSignal(PostStop.class, this::onStop)
                .build();
    }

    private Behavior<Command> onCmd(SystemInReader.Publish msg) {
        String line = msg.line;
        if (line.equals("stop")) return Behaviors.stopped();
        return this;
    }

    private <M extends Signal> Behavior<Command> onStop(M m) {
        return this;
    }

    private Behavior<Command> onInitialize(Initialize command) {
        ActorRef<SensorRegistry.Command> sensorRegistry = getContext().spawn(SensorRegistry.create(), "sensor-registry");

        UUID groupId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        sensorRegistry.tell(new SensorRegistry.Register(groupId, deviceId, sensorRegistry.unsafeUpcast()));

        ActorRef<SystemInReader.Command> reader = getContext().spawn(SystemInReader.create(), "system-in-reader");
        reader.tell(new SystemInReader.Subscribe(getContext().getSelf().unsafeUpcast()));
        reader.tell(new SystemInReader.Read());
        return this;
    }

}