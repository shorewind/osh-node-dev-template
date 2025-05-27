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

        System.out.println("Creating KY017Sensor...");

        // initialize pi4j connection
        pi4j = Pi4J.newAutoContext();
        DigitalInputConfig inputConfig = DigitalInput.newConfigBuilder(pi4j)
                .id("KY017")
                .name("KY017 Tilt Sensor")
                .address(config.bcmPinNumber)
                .build();

        digitalInput = pi4j.create(inputConfig);
        System.out.println("pi4j configuration complete...");

        // create and initialize output
        output = new KY017Output(this);
        addOutput(output, false);
        output.doInit();
    }

    public DigitalInput getInputPin() {
        return digitalInput;
    }

    @Override
    public void doStart() throws SensorHubException {

        if (null != output) {

            // Allocate necessary resources and start outputs
//            output.doStart();
            System.out.println("Listening to KY017Sensor...");
            // bind digital state change listener on input to output
            digitalInput.addListener(output);
        }

    }

    @Override
    public void doStop() throws SensorHubException {

        if (null != output) {

            pi4j.shutdown();
        }

    }

    @Override
    public boolean isConnected() {

        // determine if sensor is connected
        return output.isAlive();
    }
}
