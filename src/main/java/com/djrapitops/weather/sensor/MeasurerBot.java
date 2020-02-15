package com.djrapitops.weather.sensor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class MeasurerBot extends AbstractBehavior<MeasurerBot.Command> {

    int requestId = 0;
    private final ActorRef<TemperatureSensor.Command> sensor;

    private MeasurerBot(ActorContext<MeasurerBot.Command> context, ActorRef<TemperatureSensor.Command> sensor) {
        super(context);
        this.sensor = sensor;
        context.getLog().info("Creating bot");
    }

    public static Behavior<MeasurerBot.Command> create(ActorRef<TemperatureSensor.Command> sensor) {
        return Behaviors.setup(context -> new MeasurerBot(context, sensor));
    }

    public interface Command {}

    public static final class Start implements Command {

    }

    @Override
    public Receive<MeasurerBot.Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(TemperatureSensor.Measured.class, this::onMeasured)
                .onMessage(Start.class, this::onStart)
                .build();
    }

    private Behavior<Command> onStart(Start message) {
        sensor.tell(new TemperatureSensor.Measure(requestId, getContext().getSelf().unsafeUpcast()));
        return this;
    }

    private Behavior<MeasurerBot.Command> onMeasured(TemperatureSensor.Measured message) {
        if (message.requestId == requestId) requestId++;
        sensor.tell(new TemperatureSensor.Measure(requestId, getContext().getSelf().unsafeUpcast()));
        return this;
    }
}