package com.djrapitops.weather.sensor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.Signal;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public class TemperatureSensor extends AbstractBehavior<TemperatureSensor.Command> {

    private final UUID groupId;
    private final UUID deviceId;

    private final Random random; // For generating temperature readings.

    public static Behavior<Command> create(UUID groupId, UUID deviceId) {
        return Behaviors.setup(context -> new TemperatureSensor(context, groupId, deviceId));
    }

    public TemperatureSensor(ActorContext<Command> context, UUID groupId, UUID deviceId) {
        super(context);

        this.groupId = groupId;
        this.deviceId = deviceId;
        this.random = new Random();

        context.getLog().info("{} {}-{} started", groupId, deviceId, getClass().getSimpleName());
    }

    /* ----------------------------------------------------- */

    public interface Command {}

    public static final class Read implements Command {
        final long requestId;
        final ActorRef<Reading> replyTo;

        public Read(long requestId, ActorRef<Reading> replyTo) {
            this.requestId = requestId;
            this.replyTo = replyTo;
        }
    }

    public static final class Reading implements Command {
        final long requestId;
        private final Double value;

        public Reading(long requestId, Double value) {
            this.requestId = requestId;
            this.value = value;
        }

        public Optional<Double> getValue() {
            return Optional.ofNullable(value);
        }
    }

    public static final class Measure implements Command {
        final long requestId;
        final ActorRef<Measured> replyTo;

        public Measure(long requestId, ActorRef<Measured> replyTo) {
            this.requestId = requestId;
            this.replyTo = replyTo;
        }
    }

    public static final class Measured implements Command {
        final long requestId;

        public Measured(long requestId) {
            this.requestId = requestId;
        }
    }

    /* ----------------------------------------------------- */

    private Double lastMeasurement;

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(Read.class, this::onRead)
                .onMessage(Measure.class, this::onMeasure)
                .onSignal(PostStop.class, this::onStop)
                .build();
    }

    private Behavior<Command> onMeasure(Measure msg) {
        double measurement = random.nextInt(49) + random.nextDouble();
        this.lastMeasurement = measurement;
        getContext().getLog().info("Recorded measurement {} with {}", measurement, msg.requestId);
        msg.replyTo.tell(new Measured(msg.requestId));
        return this;
    }

    private <M extends Signal> TemperatureSensor onStop(M msg) {
        getContext().getLog().info("{} {}-{} stopped", groupId, deviceId, getClass().getSimpleName());
        return this;
    }

    private Behavior<Command> onRead(Read msg) {
        msg.replyTo.tell(new Reading(msg.requestId, lastMeasurement));
        return this;
    }
}