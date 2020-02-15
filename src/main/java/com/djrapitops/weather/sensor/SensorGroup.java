package com.djrapitops.weather.sensor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.Signal;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.HashMap;
import java.util.UUID;

public class SensorGroup extends AbstractBehavior<SensorGroup.Command> {

    private final UUID groupId;
    private final SensorsById sensors;

    public SensorGroup(ActorContext<Command> context, UUID groupId) {
        super(context);

        this.groupId = groupId;
        this.sensors = new SensorsById();
        context.getLog().info("SensorGroup {} started", groupId);
    }

    public static Behavior<Command> create(UUID groupId) {
        return Behaviors.setup(context -> new SensorGroup(context, groupId));
    }

    private static class SensorsById extends HashMap<UUID, ActorRef<TemperatureSensor.Command>> {}

    /* ----------------------------------------------------- */

    public interface Command {}

    private static final class SensorTerminated implements Command {
        final ActorRef<TemperatureSensor.Command> sensor;
        final ActorRef<MeasurerBot.Command> bot;
        final UUID deviceId;

        public SensorTerminated(ActorRef<TemperatureSensor.Command> sensor, ActorRef<MeasurerBot.Command> bot, UUID deviceId) {
            this.sensor = sensor;
            this.bot = bot;
            this.deviceId = deviceId;
        }
    }

    /* ----------------------------------------------------- */

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(SensorRegistry.Register.class, this::onRegister)
                .onMessage(SensorGroup.SensorTerminated.class, this::onUnregister)
                .onSignal(PostStop.class, this::onStop)
                .build();
    }

    private Behavior<Command> onRegister(SensorRegistry.Register msg) {
        if (groupId.equals(msg.groupId)) {
            ActorRef<TemperatureSensor.Command> sensor = sensors.computeIfAbsent(msg.deviceId, this::createNewSensor);
            ActorRef<MeasurerBot.Command> bot = getContext().spawn(MeasurerBot.create(sensor), "sensor-bot-" + msg.deviceId);
            getContext().watchWith(sensor, new SensorTerminated(sensor, bot, msg.deviceId));
            msg.replyTo.tell(new SensorRegistry.Registered(sensor));
            bot.tell(new MeasurerBot.Start());
        } else {
            getContext().getLog().warn(
                    "Ignoring Register for {}. This group is responsible for {}.", msg.groupId, this.groupId
            );
        }
        return this;
    }

    private ActorRef<TemperatureSensor.Command> createNewSensor(UUID deviceId) {
        ActorContext<Command> context = getContext();
        context.getLog().info("Creating new sensor for {}", deviceId);
        return context.spawn(TemperatureSensor.create(groupId, deviceId), "sensor-" + deviceId);
    }

    private Behavior<Command> onUnregister(SensorTerminated msg) {
        getContext().getLog().info("Sensor for {} has been terminated", msg.deviceId);
        sensors.remove(msg.deviceId);
        getContext().stop(msg.bot);
        return this;
    }

    private <M extends Signal> Behavior<Command> onStop(M msg) {
        getContext().getLog().info("SensorGroup {} stopped", groupId);
        return this;
    }
}