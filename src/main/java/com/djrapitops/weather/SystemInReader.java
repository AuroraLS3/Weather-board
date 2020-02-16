package com.djrapitops.weather;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.Signal;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.HashMap;
import java.util.Scanner;
import java.util.UUID;

public class SystemInReader extends AbstractBehavior<SystemInReader.Command> {

    private final SystemInSubscribers subscribers;
    private final Scanner scanner;

    private SystemInReader(ActorContext<Command> context) {
        super(context);
        subscribers = new SystemInSubscribers();
        scanner = new Scanner(System.in);
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(SystemInReader::new);
    }

    public static class SystemInSubscribers extends HashMap<UUID, ActorRef<FromSystemIn>> {}

    /* ----------------------------------------------------- */

    public interface Command {}

    public static final class Read implements Command {}

    public static final class Subscribe implements Command {
        final ActorRef<FromSystemIn> subscriber;

        public Subscribe(ActorRef<FromSystemIn> subscriber) {
            this.subscriber = subscriber;
        }
    }

    private static final class Unsubscribe implements Command {
        final UUID subscriberID;

        public Unsubscribe(UUID subscriberID) {
            this.subscriberID = subscriberID;
        }
    }

    public static final class FromSystemIn implements Bootstrapper.Command {
        public final String line;

        public FromSystemIn(String line) {
            this.line = line;
        }
    }

    /* ----------------------------------------------------- */

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(Read.class, this::onRead)
                .onMessage(Subscribe.class, this::onSubscribe)
                .onMessage(Unsubscribe.class, this::onUnsubscribe)
                .onSignal(PostStop.class, this::onStop)
                .build();
    }

    private <M extends Signal> Behavior<Command> onStop(M msg) {
        scanner.close();
        subscribers.clear();
        getContext().getLog().info("Press enter to continue.");
        return this;
    }

    private Behavior<Command> onRead(Read msg) {
        if (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            handleLine(line);
            if (line.equals("stop")) return Behaviors.stopped();
        }
        getContext().getSelf().tell(msg);
        return this;
    }

    private void handleLine(String line) {
        getContext().getLog().info("Read line from system in: '{}'", line);
        for (ActorRef<FromSystemIn> subscriber : subscribers.values()) {
            subscriber.tell(new FromSystemIn(line));
        }
    }

    private Behavior<Command> onUnsubscribe(Unsubscribe msg) {
        subscribers.remove(msg.subscriberID);
        return this;
    }

    private Behavior<Command> onSubscribe(Subscribe msg) {
        UUID subscriberID = UUID.randomUUID();
        subscribers.put(subscriberID, msg.subscriber);
        getContext().watchWith(msg.subscriber, new Unsubscribe(subscriberID));
        return this;
    }
}