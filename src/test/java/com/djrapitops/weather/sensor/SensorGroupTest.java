package com.djrapitops.weather.sensor;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class SensorGroupTest {

    @ClassRule
    public static final TestKitJunitResource TEST_KIT = new TestKitJunitResource();

    private UUID groupId;
    private Behavior<SensorGroup.Command> underTest;

    @Before
    public void setUp() {
        groupId = UUID.randomUUID();
        underTest = SensorGroup.create(groupId);
    }

    @Test
    public void sensorsAreRegisteredSeparately() {
        TestProbe<SensorRegistry.Registered> probe = TEST_KIT.createTestProbe(SensorRegistry.Registered.class);
        ActorRef<SensorGroup.Command> group = TEST_KIT.spawn(underTest);

        group.tell(new SensorRegistry.Register(groupId, UUID.randomUUID(), probe.getRef()));
        SensorRegistry.Registered registered1 = probe.receiveMessage();

        group.tell(new SensorRegistry.Register(groupId, UUID.randomUUID(), probe.getRef()));
        SensorRegistry.Registered registered2 = probe.receiveMessage();

        assertNotEquals(registered1.sensor, registered2.sensor);

        // Check that the sensors are working
        TestProbe<TemperatureSensor.Measured> recordProbe = TEST_KIT.createTestProbe(TemperatureSensor.Measured.class);
        long requestId = 0L;
        registered1.sensor.tell(new TemperatureSensor.Measure(requestId, recordProbe.getRef()));
        assertEquals(requestId, recordProbe.receiveMessage().requestId);
        requestId++;
        registered2.sensor.tell(new TemperatureSensor.Measure(requestId, recordProbe.getRef()));
        assertEquals(requestId, recordProbe.receiveMessage().requestId);
    }

    @Test
    public void sameSensorIsOnlyRegisteredOnce() {
        TestProbe<SensorRegistry.Registered> probe = TEST_KIT.createTestProbe(SensorRegistry.Registered.class);
        ActorRef<SensorGroup.Command> group = TEST_KIT.spawn(underTest);

        UUID deviceId = UUID.randomUUID();
        group.tell(new SensorRegistry.Register(groupId, deviceId, probe.getRef()));
        SensorRegistry.Registered registered1 = probe.receiveMessage();
        group.tell(new SensorRegistry.Register(groupId, deviceId, probe.getRef()));
        SensorRegistry.Registered registered2 = probe.receiveMessage();

        assertEquals(registered1.sensor, registered2.sensor);
    }

    @Test
    public void incorrectGroupSensorsAreIgnored() {
        TestProbe<SensorRegistry.Registered> probe = TEST_KIT.createTestProbe(SensorRegistry.Registered.class);
        ActorRef<SensorGroup.Command> group = TEST_KIT.spawn(underTest);

        UUID wrongGroupId = UUID.randomUUID();
        group.tell(new SensorRegistry.Register(wrongGroupId, UUID.randomUUID(), probe.getRef()));
        probe.expectNoMessage();
    }

}