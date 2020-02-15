package com.djrapitops.weather.sensor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.HashMap;
import java.util.UUID;

public class SensorRegistry extends AbstractBehavior<SensorRegistry.Command> {

    private final GroupsById groupsById;

    public SensorRegistry(ActorContext<Command> context) {
        super(context);
        groupsById = new GroupsById();
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(SensorRegistry::new);
    }

    private static class GroupsById extends HashMap<UUID, ActorRef<SensorGroup.Command>> {}

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
                .onMessage(Register.class, this::onRegister)
                .build();
    }

    private Behavior<Command> onRegister(Register msg) {
        ActorRef<SensorGroup.Command> group = groupsById.computeIfAbsent(msg.groupId, this::createNewGroup);
        group.tell(msg);
        return this;
    }

    private ActorRef<SensorGroup.Command> createNewGroup(UUID groupId) {
        return getContext().spawn(SensorGroup.create(groupId), "sensor-group-" + groupId);
    }
}