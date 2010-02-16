/* Copyright 2002-2010 CS Communication & Systèmes
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
package org.orekit.frames;

import org.apache.commons.math.geometry.Vector3D;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeLaw;
import org.orekit.attitudes.NadirPointing;
import org.orekit.attitudes.YawSteering;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

public class SpacecraftFrameTest {

    @Test
    public void testPV() throws OrekitException {
        Vector3D sunSat = sun.getPVCoordinates(stopDate, scFrame).getPosition();
        Assert.assertEquals(0, (sunSat.getY()/sunSat.getNorm()), 1.0e-10);
    }    

    @Test
    public void testPropagator() throws OrekitException {
        SpacecraftState propagated = scFrame.getPropagator().propagate(stopDate);
        Assert.assertEquals(0, stopDate.durationFrom(propagated.getDate()), 1.0e-10);
    }    

    @Before
    public void setUp() {
        try {
            Utils.setDataRoot("regular-data");

            // Orbit
            double mu  = 3.986004415e14;
            double a  = 7178000.0;
            double ex = 0.5e-4;
            double ey = 0.5e-4;
            double i  = Math.toRadians(50.);
            double raan = Math.toRadians(220.);
            double alfa = Math.toRadians(5.300);

            AbsoluteDate iniDate = new AbsoluteDate(1970, 04, 07, 0, 0, 0.0,
                                                    TimeScalesFactory.getUTC());
            eme2000 = FramesFactory.getEME2000();
            Orbit orbit = new CircularOrbit(a, ex, ey, i, raan, alfa,
                                            CircularOrbit.MEAN_LONGITUDE_ARGUMENT,
                                            eme2000, iniDate, mu);

            // Target pointing attitude law over satellite nadir at date with yaw compensation
            double ae =  6378137.0;
            double f  =  1.0 / 298.257223563;
            Frame itrf2005 = FramesFactory.getITRF2005(true);
            BodyShape earth = new OneAxisEllipsoid(ae, f, itrf2005);
            sun = CelestialBodyFactory.getSun();

            AttitudeLaw attitudeLaw =
                new YawSteering(new NadirPointing(earth), sun, Vector3D.MINUS_I);

            // Propagator : Eckstein-Hechler analytic propagator
            double c20 = -1.08263e-3;
            double c30 = 2.54e-6;
            double c40 = 1.62e-6;
            double c50 = 2.3e-7;
            double c60 = -5.5e-7;

            Propagator propagator =
                new EcksteinHechlerPropagator(orbit, attitudeLaw, ae, mu,
                                              c20, c30, c40, c50, c60);

            // The spacecraft frame is associated with the propagator.
            scFrame = new SpacecraftFrame(propagator, "Spacecraft");
            
            stopDate = iniDate.shiftedBy(1000.0);

        } catch (OrekitException oe) {
            Assert.fail(oe.getLocalizedMessage());
        }
    }

    @After
    public void tearDown() {
        eme2000 = null;
        sun = null;
        scFrame = null;
        stopDate = null;
    }

    private Frame eme2000;
    private CelestialBody sun;
    private SpacecraftFrame scFrame;
    private AbsoluteDate stopDate;

}
