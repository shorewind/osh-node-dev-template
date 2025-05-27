/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.sample.impl.sensor.training;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEHelper;

// pi4J imports
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.gpio.digital.DigitalStateChangeEvent;
import com.pi4j.io.gpio.digital.DigitalStateChangeListener;

/**
 * KY017Output specification and provider for {@link KY017Sensor}.
 *
 * @author your_name
 * @since date
 */
public class KY017Output extends AbstractSensorOutput<KY017Sensor> implements DigitalStateChangeListener {

    private static final String SENSOR_OUTPUT_NAME = "KY017";
    private static final String SENSOR_OUTPUT_LABEL = "KY017";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "KY017 Tilt Sensor Measurements";

    private static final Logger logger = LoggerFactory.getLogger(KY017Output.class);

    private DataRecord dataStruct;
    private DataEncoding dataEncoding;

    private static final int MAX_NUM_TIMING_SAMPLES = 10;
    private int setCount = 0;
    private final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];
    private final Object histogramLock = new Object();

    private long lastSetTimeMillis = System.currentTimeMillis();
    private volatile boolean sensorDetectionReading;

    /**
     * Constructor
     *
     * @param parentSensor KY017Sensor driver providing this output
     */
    KY017Output(KY017Sensor parentSensor) {

        super(SENSOR_OUTPUT_NAME, parentSensor);

        logger.debug("KY017Output created");
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering,
     * and data types.
     */
    void doInit() {

        logger.debug("Initializing KY017Output");

        // get instance of SWE Factory suitable to build components
        SWEHelper sweFactory = new SWEHelper();

        // define data structure
        dataStruct = sweFactory.createRecord()
                .name(SENSOR_OUTPUT_NAME)
                .label(SENSOR_OUTPUT_LABEL)
                .definition(SWEHelper.getPropertyUri("KY017Output"))
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .addField("sampleTime", sweFactory.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sample Time")
                        .description("Time of data collection"))
                .addField("isTilted", sweFactory.createBoolean()
                        .label("Is Tilted"))
                        .definition(SWEHelper.getPropertyUri("tilted"))  // sensorML property name
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");

        logger.debug("Initializing KY017Output Complete");

        // get initial state
        // TODO: retrieve data on module restart
        DigitalInput inputPin = parentSensor.getInputPin();
//        System.out.println(inputPin.state());
        if (inputPin != null) {
            DigitalState currentState = inputPin.state();
            sensorDetectionReading = currentState == DigitalState.LOW;

            DataBlock dataBlock = dataStruct.createDataBlock();
            double timestamp = System.currentTimeMillis() / 1000d;

            dataBlock.setDoubleValue(0, timestamp);
            dataBlock.setBooleanValue(1, sensorDetectionReading);

            latestRecord = dataBlock;
            latestRecordTime = System.currentTimeMillis();

            logger.debug("Initial reading isTilted: " + sensorDetectionReading);
            eventHandler.publish(new DataEvent(latestRecordTime, KY017Output.this, dataBlock));

            inputPin.addListener(this);
        }
    }

    /**
     * Check to validate data processing is still running
     *
     * @return true if worker thread is active, false otherwise
     */
    public boolean isAlive() {

        return true;
    }

    @Override
    public DataComponent getRecordDescription() {

        return dataStruct;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {

        return dataEncoding;
    }

    @Override
    public double getAverageSamplingPeriod() {

        long accumulator = 0;

        synchronized (histogramLock) {

            for (int idx = 0; idx < MAX_NUM_TIMING_SAMPLES; ++idx) {

                accumulator += timingHistogram[idx];
            }
        }

        return accumulator / (double) MAX_NUM_TIMING_SAMPLES;
    }

    @Override
    public void onDigitalStateChange(DigitalStateChangeEvent digitalStateChangeEvent) {

        // tilted if signal is LOW
        sensorDetectionReading = digitalStateChangeEvent.state() == DigitalState.LOW;

        DataBlock dataBlock;
        dataBlock = dataStruct.createDataBlock();

        // replace existing data block
//        if (latestRecord == null) {
//            dataBlock = dataStruct.createDataBlock();
//            dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000d);
//            dataBlock.setBooleanValue(1, false);
//        } else {
//            dataBlock = latestRecord.renew();
//        }
        synchronized (histogramLock) {
            int setIndex = setCount % MAX_NUM_TIMING_SAMPLES;

            // Get a sampling time for latest set based on previous set sampling time
            timingHistogram[setIndex] = System.currentTimeMillis() - lastSetTimeMillis;

            // Set latest sampling time to now
            lastSetTimeMillis = timingHistogram[setIndex];
        }

        ++setCount;
        double timestamp = System.currentTimeMillis() / 1000d;

        dataBlock.setDoubleValue(0, timestamp);
        dataBlock.setBooleanValue(1, sensorDetectionReading);

        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();

        logger.debug("Publishing isTilted: " + sensorDetectionReading);
        eventHandler.publish(new DataEvent(latestRecordTime, KY017Output.this, dataBlock));
    }
}
