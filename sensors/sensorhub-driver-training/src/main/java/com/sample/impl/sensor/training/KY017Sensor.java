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

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalInputConfig;

/**
 * KY017Sensor driver providing sensor description, output registration, initialization and shutdown of driver and outputs.
 *
 * @author your_name
 * @since date
 */
public class KY017Sensor extends AbstractSensorModule<KY017Config> {

    private static final Logger logger = LoggerFactory.getLogger(KY017Sensor.class);

    KY017Output output;

    Context pi4j;
    DigitalInput digitalInput;

    @Override
    public void doInit() throws SensorHubException {

        super.doInit();

        // Generate identifiers
        generateUniqueID("urn:osh:sensor:training", config.serialNumber);
        generateXmlID("TRAINING_SENSOR", config.serialNumber);

        // TODO: Perform other initialization
        System.out.println("Creating KY017Sensor...");

        // Create Pi4J Context
        pi4j = Pi4J.newAutoContext();
        // Create a DigitalInput Configuration
        DigitalInputConfig inputConfig = DigitalInput.newConfigBuilder(pi4j)
                .id("KY017")
                .name("KY017 Tilt Sensor")
                .address(config.bcmPinNumber)
                .build();

        // CREATE A DigitalInput Instance using inputConfig configuration to store the Raspberry Pi's input
        digitalInput = pi4j.create(inputConfig);
        System.out.println("pi4j configuration complete...");

        // Create and initialize output
        output = new KY017Output(this);
        addOutput(output, false);
        output.doInit();
    }

    @Override
    public void doStart() throws SensorHubException {

        if (null != output) {

            // Allocate necessary resources and start outputs
//            output.doStart();
            System.out.println("Listening to KY017Sensor...");
            // Add listener requires a Digital State Change Listener to register an object. Bind that to Output class
            digitalInput.addListener(output);
        }

        // TODO: Perform other startup procedures
    }

    @Override
    public void doStop() throws SensorHubException {

        if (null != output) {

            pi4j.shutdown();
        }

        // TODO: Perform other shutdown procedures
    }

    @Override
    public boolean isConnected() {

        // Determine if sensor is connected
        return output.isAlive();
    }
}
