/* Copyright 2002-2008 CS Communication & Systèmes
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
package org.orekit.forces.radiation;

import java.io.FileNotFoundException;
import java.text.ParseException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.math.ode.DerivativeException;
import org.apache.commons.math.ode.IntegratorException;
import org.apache.commons.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.apache.commons.math.ode.nonstiff.DormandPrince853Integrator;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.bodies.SolarSystemBody;
import org.orekit.data.DataDirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.forces.SphericalSpacecraft;
import org.orekit.frames.Frame;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.UTCScale;


public class SolarRadiationPressureTest extends TestCase {

    public void testLightning() throws OrekitException, ParseException, DerivativeException, IntegratorException{
        // Initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 3, 21),
                                             new TimeComponents(13, 59, 27.816),
                                             UTCScale.getInstance());
        Orbit orbit = new EquinoctialOrbit(42164000,10e-3,10e-3,
                                           Math.tan(0.001745329)*Math.cos(2*Math.PI/3), Math.tan(0.001745329)*Math.sin(2*Math.PI/3),
                                           0.1, 2, Frame.getEME2000(), date, mu);
        CelestialBody sun = SolarSystemBody.getSun();
        OneAxisEllipsoid earth =
            new OneAxisEllipsoid(6378136.46, 1.0 / 298.25765,
                                 Frame.getITRF2005());
        SolarRadiationPressure SRP = 
            new SolarRadiationPressure(sun, earth.getEquatorialRadius(),
                                       (RadiationSensitive) new SphericalSpacecraft(50.0, 0.5, 0.5, 0.5));

        double period = 2*Math.PI*Math.sqrt(orbit.getA()*orbit.getA()*orbit.getA()/orbit.getMu());
        assertEquals(86164, period,1);

        // creation of the propagator
        KeplerianPropagator k = new KeplerianPropagator(orbit);

        // intermediate variables
        AbsoluteDate currentDate;
        double changed = 1;
        int count=0;

        for(int t=1;t<3*period;t+=1000) {
            currentDate = new AbsoluteDate(date , t);
            try {

                double ratio = SRP.getLightningRatio(k.propagate(currentDate).getPVCoordinates().getPosition(),
                                                     Frame.getEME2000(), currentDate);

                if(Math.floor(ratio)!=changed) {
                    changed = Math.floor(ratio);
                    if(changed == 0) {
                        count++;
                    }
                }
            } catch (OrekitException e) {
                e.printStackTrace();
            }
        }
        assertTrue(3==count);
    }

    public void testRoughOrbitalModifs() throws ParseException, OrekitException, DerivativeException, IntegratorException, FileNotFoundException {

        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 7, 1),
                                             new TimeComponents(13, 59, 27.816),
                                             UTCScale.getInstance());
        Orbit orbit = new EquinoctialOrbit(42164000,10e-3,10e-3,
                                           Math.tan(0.001745329)*Math.cos(2*Math.PI/3),
                                           Math.tan(0.001745329)*Math.sin(2*Math.PI/3),
                                           0.1, 2, Frame.getEME2000(), date, mu);
        final double period = orbit.getKeplerianPeriod();
        assertEquals(86164, period, 1);
        CelestialBody sun = SolarSystemBody.getSun();

        // creation of the force model
        OneAxisEllipsoid earth =
            new OneAxisEllipsoid(6378136.46, 1.0 / 298.25765,
                                 Frame.getITRF2005());
        SolarRadiationPressure SRP =
            new SolarRadiationPressure(sun, earth.getEquatorialRadius(),
                                       (RadiationSensitive) new SphericalSpacecraft(500.0, 0.7, 0.7, 0.7));

        // creation of the propagator
        double[] absTolerance = {
            0.1, 1.0e-9, 1.0e-9, 1.0e-5, 1.0e-5, 1.0e-5, 0.001
        };
        double[] relTolerance = {
            1.0e-4, 1.0e-4, 1.0e-4, 1.0e-6, 1.0e-6, 1.0e-6, 1.0e-7
        };
        AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(900.0, 60000, absTolerance, relTolerance);
        integrator.setInitialStepSize(3600);
        final NumericalPropagator calc = new NumericalPropagator(integrator);
        calc.addForceModel(SRP);

        // Step Handler
        calc.setMasterMode(Math.floor(15 * period), new SolarStepHandler());
        AbsoluteDate finalDate = new AbsoluteDate(date, 20 * period);
        calc.setInitialState(new SpacecraftState(orbit, 1500.0));
        calc.propagate(finalDate);
        assertTrue(calc.getCalls() < 7100);
    }

    public static void checkRadius(double radius , double min , double max) {
        assertTrue(radius >= min);
        assertTrue(radius <= max);
    }

    private double mu = 3.98600E14;

    private static class SolarStepHandler implements OrekitFixedStepHandler {

        /** Serializable UID. */
        private static final long serialVersionUID = -2346826010279512941L;

        private SolarStepHandler() {
        }

        public void handleStep(double t, double[]y, boolean isLastStep) {
        }

        public void handleStep(SpacecraftState currentState, boolean isLast) {
            final double dex = currentState.getEquinoctialEx() - 0.00940313;
            final double dey = currentState.getEquinoctialEy() - 0.013679;
            checkRadius(Math.sqrt(dex * dex + dey * dey), 0.00351, 0.00394);
        }

    }

    public void setUp() {
        System.setProperty(DataDirectoryCrawler.DATA_ROOT_DIRECTORY_FS, "");
        System.setProperty(DataDirectoryCrawler.DATA_ROOT_DIRECTORY_CP, "regular-data");
    }

    public static Test suite() {
        return new TestSuite(SolarRadiationPressureTest.class);
    }

}
