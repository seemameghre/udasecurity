package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;


@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {
    private SecurityService securityService;
    @Mock
    private SecurityRepository securityRepository;
    @Mock
    private ImageService imageService;
    @BeforeEach
    public void init(){
        securityService = new SecurityService(securityRepository,imageService);
    }

    /* provides arguments with combination of sensor of each sensor type and each armed alarm status
     */
    public static Stream<Arguments> provideSensorsAndArmedStatus(){
        return Stream.of(SensorType.values()).flatMap(s ->
                    Stream.of(ArmingStatus.ARMED_HOME,ArmingStatus.ARMED_AWAY).map(a ->
                            Arguments.of(new Sensor(s.name(),s),a,s.name())
                    )
            );

    }
    /* Provide sensors */
    public static Stream<Arguments> provideSensors(){
        return Stream.of(
            Arguments.of(new Sensor("testSensor1",SensorType.DOOR)),
            Arguments.of(new Sensor("testSensor2",SensorType.WINDOW)),
            Arguments.of(new Sensor("testSensor3",SensorType.MOTION))
        );
    }
    @ParameterizedTest(name="Req 1:{index} {1} {2}")
    @MethodSource("provideSensorsAndArmedStatus")
    public void alarmArmed_sensorActivated_setsAlarmToPendingState(Sensor sensor,ArmingStatus armingStatus,
                                                                   String sensorName){
        securityRepository.addSensor(sensor);
        Mockito.when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor,true);
        Mockito.verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }
    @ParameterizedTest(name="Req 2:{index} {1} {2}")
    @MethodSource("provideSensorsAndArmedStatus")
    public void alarmArmed_sensorActivated_systemPending_setsAlarmToAlarmState(Sensor sensor,
                                                                               ArmingStatus armingStatus,
                                                                               String sensorName){
        securityRepository.addSensor(sensor);
        Mockito.when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor,true);
        Mockito.verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }
    @ParameterizedTest(name="Req 3: Sensor {index}")
    @MethodSource("provideSensors")
    public void alarmPending_allSensorsInactive_setsAlarmToNoAlarmState(Sensor sensor){
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        Mockito.when(securityRepository.isAnySensorActive()).thenReturn(false);
        sensor.setActive(true);
        securityRepository.addSensor(sensor);
        securityService.changeSensorActivationStatus(sensor,false);
        Mockito.verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
    @ParameterizedTest(name = "Req 4 {index} {0}")
    @ValueSource(booleans = {true,false})
    public void alarmActive_sensorChange_noChangeInAlarmState(boolean active){
        //using lenient as the following method is not called for sensor deactivation i.e. when argument=false
        Mockito.lenient().when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        Sensor sensor = new Sensor("testSensor",SensorType.WINDOW);
        securityRepository.addSensor(sensor);
        securityService.changeSensorActivationStatus(sensor,active);
        Mockito.verify(securityRepository,Mockito.never()).setAlarmStatus(any(AlarmStatus.class));
    }
    //Req 5
    @Test
    public void alarmPending_activeSensorActivated_setsAlarmState(){
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        Sensor sensor = new Sensor("testSensor",SensorType.WINDOW);
        sensor.setActive(true);
        securityRepository.addSensor(sensor);
        securityService.changeSensorActivationStatus(sensor,true);
        Mockito.verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }
    @ParameterizedTest(name="Req 6 {index} {0}")
    @EnumSource(AlarmStatus.class)
    public void sensorInactive_deactivate_noChangeInSystem(AlarmStatus alarmStatus){
        Mockito.lenient().when(securityRepository.getAlarmStatus()).thenReturn(alarmStatus);
        Sensor sensor = new Sensor("testSensor",SensorType.WINDOW);
        securityRepository.addSensor(sensor);
        securityService.changeSensorActivationStatus(sensor,false);
        Mockito.verify(securityRepository,Mockito.never()).setAlarmStatus(any(AlarmStatus.class));
    }
    //Req 9
    @Test
    public void disarmingSystem_setsNoAlarmStatus(){
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        Mockito.verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
    //Req10
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class,names = {"ARMED_AWAY","ARMED_HOME"})
    public void armingSystem_resetsAllSensorsInactive(ArmingStatus armingStatus){
        Mockito.when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        //System is getting armed, so alarm is not set
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        Sensor sensor1 = new Sensor("sensor1",SensorType.MOTION);
        Sensor sensor2 = new Sensor("sensor2",SensorType.WINDOW);
        Set<Sensor> sensors = new TreeSet<>();
        sensors.add(sensor1);
        sensors.add(sensor2);
        securityService.addSensor(sensor1);
        securityService.addSensor(sensor2);

        Mockito.when(securityRepository.getSensors()).thenReturn(sensors);
        securityService.changeSensorActivationStatus(sensor1,true);
        securityService.changeSensorActivationStatus(sensor2,true);
        securityService.setArmingStatus(armingStatus);
        assertAll(
                () -> assertFalse(sensor1.getActive()),
                () -> assertFalse(sensor2.getActive())
        );
    }
    //Image service tests start here
    //Req 7
    @Test
    public void systemArmedHome_catDetected_setsAlarmState(){
        BufferedImage bufferedImage = new BufferedImage(100,100,1);
        Mockito.when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        Mockito.when(imageService.imageContainsCat(any(BufferedImage.class),anyFloat())).thenReturn(true);
        securityService.processImage(bufferedImage);
        Mockito.verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }
    //Req 8
    @Test
    public void imageWithNoCat_noSensorActive_changesToNoAlarm(){
        BufferedImage bufferedImage = new BufferedImage(100,100,1);
        Mockito.when(imageService.imageContainsCat(any(BufferedImage.class),anyFloat())).thenReturn(false);
        Mockito.when(securityRepository.isAnySensorActive()).thenReturn(false);
        securityService.processImage(bufferedImage);
        Mockito.verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
    //Req11
    @Test
    public void catHasBeenDetected_systemArmedHome_setsAlarmState(){
        Mockito.when(securityRepository.isAnySensorActive()).thenReturn(false);
        BufferedImage bufferedImage = new BufferedImage(100,100,1);
        Mockito.when(imageService.imageContainsCat(any(BufferedImage.class),anyFloat())).thenReturn(true);

        securityService.processImage(bufferedImage);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        Mockito.verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }
}