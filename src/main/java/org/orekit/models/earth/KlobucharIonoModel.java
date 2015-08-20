/* Copyright 2011-2012 Space Applications Services
 * Licensed to CS Communication & Systèmes (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.models.earth;

import org.apache.commons.math3.util.FastMath;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

/**
 * Klobuchar ionospheric delay model.
 * Klobuchar ionospheric delay model is designed as a GNSS correction model.
 * The parameters for the model are provided by the GPS satellites in their broadcast
 * messsage.
 * This model is based on the assumption the electron content is concentrated
 * in 350 km layer.
 *
 * @author Joris Olympio
 *
 */
public class KlobucharIonoModel implements IonosphericDelayModel {

    /** Serializable UID. */
    private static final long serialVersionUID = 7277525837842061107L;

    /** The 4 coefficients of a cubic equation representing the amplitude of the vertical delay. Units are sec/semi-circle^(i-1) for the i-th coefficient, i=1,2,3,4. */
    private final double[] alpha;
    /** The 4 coefficients of a cubic equation representing the period of the model. Units are sec/semi-circle^(i-1) for the i-th coefficient, i=1,2,3,4. */
    private final double[] beta;

    /** Create a new Klobuchar ionospheric delay model, when a single frequency system is used.
     * This model accounts for at least 50 percent of RMS error due to ionospheric propagation effect (ICD-GPS-200)
     *
     * @param alpha coefficients of a cubic equation representing the amplitude of the vertical delay.
     * @param beta coefficients of a cubic equation representing the period of the model.
     */
    public KlobucharIonoModel(final double[] alpha, final double[] beta) {
        this.alpha = alpha;
        this.beta = beta;
    }

    /** {@inheritDoc} */
    @Override
    public double calculatePathDelay(final AbsoluteDate date, final GeodeticPoint geo,
                                     final double elevation, final double azimuth) {
        // degees to semisircles
        final double deg2semi =  1. / 180.;
        final double rad2semi =  1. / Math.PI;
        final double semi2rad =  Math.PI;
        final double deg2rad = Math.PI / 180.;

        // Earth Centered angle
        final double psi = 0.0137 / (elevation * deg2semi + 0.11) - 0.022;

        // Subionospheric latitude: the latitude of the IPP (Ionospheric Pierce Point)
        // in [-0.416, 0.416]
        final double lat_i = FastMath.min(
                                      FastMath.max(geo.getLatitude() * rad2semi + psi * FastMath.cos(azimuth * deg2rad), -0.416),
                                      0.416);

        // Subionospheric longitude: the longitude of the IPP
        final double long_i = geo.getLongitude() * rad2semi + (psi * FastMath.sin(azimuth * deg2rad) / Math.cos(lat_i * semi2rad));

        // Geomagnetic latitude
        final double lat_m = lat_i + 0.064 * FastMath.cos((long_i - 1.617) * semi2rad);

        // day of week and tow (sec)
        final DateTimeComponents dtc = date.getComponents(TimeScalesFactory.getGPS());
        final int dofweek = dtc.getDate().getDayOfWeek() - 1;
        final double secday = dtc.getTime().getSecondsInDay();
        final double tow = dofweek * 86400. + secday;

        final double t = 43200. * long_i + tow;
        final double tsec = t - FastMath.floor(t / 86400.) * 86400; // Seconds of day

        // Slant factor
        final double slantFactor = 1.0 + 16.0 * FastMath.pow(0.53 - elevation * deg2semi, 3);

        // Period of model
        final double period = FastMath.min(72000., beta[0] + (beta[1]  + (beta[2] + beta[3] * lat_m) * lat_m) * lat_m);

        // Phase of the model
        // (Max at 14.00 = 50400 sec local time)
        final double x = 2.0 * Math.PI * (tsec - 50400.0) / period;

        // Amplitude of the model
        final double amplitude = FastMath.max(0, alpha[0] + (alpha[1]  + (alpha[2] + alpha[3] * lat_m) * lat_m) * lat_m);

        // Ionospheric correction (L1)
        double ionoTimeDelayL1 = slantFactor * (5. * 1e-9);
        if (Math.abs(x) < 1.570) {
            ionoTimeDelayL1 += slantFactor * (amplitude * (1.0 - FastMath.pow(x, 2) / 2.0 + FastMath.pow(x, 4) / 24.0));
        }

        // Ionospheric delay for the L1 frequency, in meters, with slant correction.
        return Constants.SPEED_OF_LIGHT * ionoTimeDelayL1;
    }
}
