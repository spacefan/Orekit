/* Copyright 2002-2011 CS Communication & Systèmes
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

import org.apache.commons.math.geometry.euclidean.threed.Rotation;
import org.apache.commons.math.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math.util.FastMath;
import org.orekit.utils.Constants;

/** EME2000 frame : mean equator at J2000.0.
 * <p>This frame was the standard inertial reference prior to GCRF. It was defined
 * using Lieske precession-nutation model for Earth. This frame has been superseded
 * by GCRF which is implicitly defined from a few hundred quasars coordinates.<p>
 * <p>The transformation between GCRF and EME2000 is a constant rotation bias.</p>
 * @version $Revision$ $Date$
 * @author Luc Maisonobe
 */
class EME2000Frame extends FactoryManagedFrame {

    /** Serializable UID. */
    private static final long serialVersionUID = -5958683081464730407L;

    /** Obliquity of the ecliptic. */
    private static final double EPSILON_0 = 84381.448 * Constants.ARC_SECONDS_TO_RADIANS;

    /** Bias in longitude. */
    private static final double D_PSI_B = -0.041775 * Constants.ARC_SECONDS_TO_RADIANS;

    /** Bias in obliquity. */
    private static final double D_EPSILON_B = -0.0068192 * Constants.ARC_SECONDS_TO_RADIANS;

    /** Right Ascension of the 2000 equinox in ICRS frame. */
    private static final double ALPHA_0 = -0.0146 * Constants.ARC_SECONDS_TO_RADIANS;

    /** Simple constructor.
     * @param factoryKey key of the frame within the factory
     */
    protected EME2000Frame(final Predefined factoryKey) {

        super(FramesFactory.getGCRF(), null, true, factoryKey);

        // build the bias transform
        final Rotation r1 = new Rotation(Vector3D.PLUS_I, D_EPSILON_B);
        final Rotation r2 = new Rotation(Vector3D.PLUS_J, -D_PSI_B * FastMath.sin(EPSILON_0));
        final Rotation r3 = new Rotation(Vector3D.PLUS_K, -ALPHA_0);
        final Rotation bias = r1.applyTo(r2.applyTo(r3));

        // store the bias transform
        setTransform(new Transform(bias));

    }

}
