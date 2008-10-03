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

package fr.cs.examples.propagation;

import org.apache.commons.math.geometry.Vector3D;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.ElevationDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.UTCScale;
import org.orekit.utils.PVCoordinates;

import fr.cs.examples.Autoconfiguration;

/** Orekit tutorial for special event detection.
 * <p>This tutorial shows how to easily check for visibility between a satellite and a ground station.<p>
 * @author Pascal Parraud
 * @version $Revision: 2043 $ $Date: 2008-09-24 10:57:23 +0200 (mer., 24 sept. 2008) $
 */
public class VisibilityCheck {

    /** Program entry point.
     * @param args program arguments (unused here)
     */
    public static void main(String[] args) {
        try {

            // configure Orekit
            Autoconfiguration.configureOrekit();

            //  Initial state definition : date, orbit
            AbsoluteDate initialDate = new AbsoluteDate(2004, 01, 01, 23, 30, 00.000, UTCScale.getInstance());
            double mu =  3.9860064e+14; // gravitation coefficient
            Frame inertialFrame = Frame.getEME2000(); // inertial frame for orbit definition
            Vector3D position  = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
            Vector3D velocity  = new Vector3D(505.8479685, 942.7809215, 7435.922231);
            PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
            Orbit initialOrbit = new KeplerianOrbit(pvCoordinates, inertialFrame, initialDate, mu);

            // Propagator : consider a simple keplerian motion (could be more elaborate)
            Propagator kepler = new KeplerianPropagator(initialOrbit);

            // Earth and frame  
            double ae =  6378137.0; // equatorial radius in meter
            double f  =  1.0 / 298.257223563; // flattening
            Frame ITRF2005 = Frame.getITRF2005(); // terrestrial frame at an arbitrary date
            BodyShape earth = new OneAxisEllipsoid(ae, f, ITRF2005);

            // Station
            final double longitude = Math.toRadians(45.);
            final double latitude  = Math.toRadians(25.);
            final double altitude  = 0.;
            final GeodeticPoint station1 = new GeodeticPoint(longitude, latitude, altitude);
            final TopocentricFrame sta1Frame = new TopocentricFrame(earth, station1, "station1");

            // Event definition 
            final double maxcheck  = 1.;
            final double elevation = Math.toRadians(5.);
            final EventDetector sta1Visi = new VisibilityDetector(maxcheck, elevation, sta1Frame);

            // Add event to be detected
            kepler.addEventDetector(sta1Visi);

            // Extrapolate from the initial date to the first raising or until the fixed duration
            SpacecraftState finalState = kepler.propagate(new AbsoluteDate(initialDate, 1500.));

            System.out.println(" Final state : " + finalState.getDate());

        } catch (OrekitException oe) {
            System.err.println(oe.getMessage());
        }
    }

    /** Finder for visibility event.
     * <p>This class extends the elevation detector modifying the event handler.<p>
     * @author Pascal Parraud
     */
    private static class VisibilityDetector extends ElevationDetector {

        /** Serializable UID. */
        private static final long serialVersionUID = -8909135870522456842L;

        public VisibilityDetector(double maxCheck, double elevation, TopocentricFrame topo) {
            super(maxCheck, elevation, topo);
        }

        public int eventOccurred(final SpacecraftState s) throws OrekitException {
            final double zVelocity = s.getPVCoordinates(getTopocentricFrame()).getVelocity().getZ();
            if (zVelocity > 0) {
                System.out.println(" Visibility on " + getTopocentricFrame().getName()
                                                     + " begins at " + s.getDate());
                return CONTINUE;
            } else {
                System.out.println(" Visibility on " + getTopocentricFrame().getName()
                                                     + " ends at " + s.getDate());
                return STOP;
            }
        }

    }

}