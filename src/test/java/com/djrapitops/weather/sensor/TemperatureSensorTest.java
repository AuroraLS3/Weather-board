package com.djrapitops.weather.sensor;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TemperatureSensorTest {

    @ClassRule
    public static final TestKitJunitResource TEST_KIT = new TestKitJunitResource();

    private Behavior<TemperatureSensor.Command> underTest;

    @Before
    public void setUp() {
        underTest = TemperatureSensor.create(UUID.randomUUID(), UUID.randomUUID());
    }

    @Test
    public void readingIsEmptyIfNotMeasured() {
        TestProbe<TemperatureSensor.Reading> probe = TEST_KIT.createTestProbe(TemperatureSensor.Reading.class);
        ActorRef<TemperatureSensor.Command> sensor = TEST_KIT.spawn(underTest);

        long requestId = 42L;
        sensor.tell(new TemperatureSensor.Read(requestId, probe.getRef()));
        TemperatureSensor.Reading response = probe.receiveMessage();

        assertEquals(requestId, response.requestId);
        assertEquals(Optional.empty(), response.getValue());
    }

    @Test
    public void readingIsBelowFiftyIfMeasured() {
        TestProbe<TemperatureSensor.Reading> readProbe = TEST_KIT.createTestProbe(TemperatureSensor.Reading.class);
        TestProbe<TemperatureSensor.Measured> measureProbe = TEST_KIT.createTestProbe(TemperatureSensor.Measured.class);
        ActorRef<TemperatureSensor.Command> sensor = TEST_KIT.spawn(underTest);

        long requestId = 42L;
        sensor.tell(new TemperatureSensor.Measure(requestId, measureProbe.getRef()));
        assertEquals(requestId, measureProbe.receiveMessage().requestId);

        requestId++;
        sensor.tell(new TemperatureSensor.Read(requestId, readProbe.getRef()));
        TemperatureSensor.Reading reading = readProbe.receiveMessage();
        assertEquals(requestId, reading.requestId);
        assertTrue(reading.getValue().isPresent() && reading.getValue().get() <= 50.000000001);
    }
}