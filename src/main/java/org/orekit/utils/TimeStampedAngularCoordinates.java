/* Copyright 2002-2014 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.utils;

import java.util.Collection;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.analysis.interpolation.HermiteInterpolator;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathArrays;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;

/** {@link TimeStamped time-stamped} version of {@link AngularCoordinates}.
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @author Luc Maisonobe
 * @since 7.0
 */
public class TimeStampedAngularCoordinates extends AngularCoordinates implements TimeStamped {

    /** Serializable UID. */
    private static final long serialVersionUID = 20140723L;

    /** The date. */
    private final AbsoluteDate date;

    /** Builds a rotation/rotation rate pair.
     * @param date coordinates date
     * @param rotation rotation
     * @param rotationRate rotation rate Ω (rad/s)
     * @param rotationAcceleration rotation acceleration dΩ/dt (rad²/s²)
     */
    public TimeStampedAngularCoordinates(final AbsoluteDate date,
                                         final Rotation rotation,
                                         final Vector3D rotationRate,
                                         final Vector3D rotationAcceleration) {
        super(rotation, rotationRate, rotationAcceleration);
        this.date = date;
    }

    /** {@inheritDoc} */
    public AbsoluteDate getDate() {
        return date;
    }

    /** Revert a rotation/rotation rate pair.
     * Build a pair which reverse the effect of another pair.
     * @return a new pair whose effect is the reverse of the effect
     * of the instance
     */
    public TimeStampedAngularCoordinates revert() {
        return new TimeStampedAngularCoordinates(date,
                                                 getRotation().revert(),
                                                 getRotation().applyInverseTo(getRotationRate().negate()),
                                                 getRotation().applyInverseTo(getRotationAcceleration().negate()));
    }

    /** Get a time-shifted state.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on
     * a simple linear model. It is <em>not</em> intended as a replacement for
     * proper attitude propagation but should be sufficient for either small
     * time shifts or coarse accuracy.
     * </p>
     * @param dt time shift in seconds
     * @return a new state, shifted with respect to the instance (which is immutable)
     */
    public TimeStampedAngularCoordinates shiftedBy(final double dt) {
        final AngularCoordinates sac = super.shiftedBy(dt);
        return new TimeStampedAngularCoordinates(date.shiftedBy(dt),
                                                 sac.getRotation(), sac.getRotationRate(), sac.getRotationAcceleration());

    }

    /** Add an offset from the instance.
     * <p>
     * We consider here that the offset rotation is applied first and the
     * instance is applied afterward. Note that angular coordinates do <em>not</em>
     * commute under this operation, i.e. {@code a.addOffset(b)} and {@code
     * b.addOffset(a)} lead to <em>different</em> results in most cases.
     * </p>
     * <p>
     * The two methods {@link #addOffset(AngularCoordinates) addOffset} and
     * {@link #subtractOffset(AngularCoordinates) subtractOffset} are designed
     * so that round trip applications are possible. This means that both {@code
     * ac1.subtractOffset(ac2).addOffset(ac2)} and {@code
     * ac1.addOffset(ac2).subtractOffset(ac2)} return angular coordinates equal to ac1.
     * </p>
     * @param offset offset to subtract
     * @return new instance, with offset subtracted
     * @see #subtractOffset(AngularCoordinates)
     */
    @Override
    public TimeStampedAngularCoordinates addOffset(final AngularCoordinates offset) {
        return new TimeStampedAngularCoordinates(date,
                                                 getRotation().applyTo(offset.getRotation()),
                                                 getRotationRate().add(getRotation().applyTo(offset.getRotationRate())),
                                                 getRotationAcceleration().add(getRotation().applyTo(offset.getRotationAcceleration())));
    }

    /** Subtract an offset from the instance.
     * <p>
     * We consider here that the offset rotation is applied first and the
     * instance is applied afterward. Note that angular coordinates do <em>not</em>
     * commute under this operation, i.e. {@code a.subtractOffset(b)} and {@code
     * b.subtractOffset(a)} lead to <em>different</em> results in most cases.
     * </p>
     * <p>
     * The two methods {@link #addOffset(AngularCoordinates) addOffset} and
     * {@link #subtractOffset(AngularCoordinates) subtractOffset} are designed
     * so that round trip applications are possible. This means that both {@code
     * ac1.subtractOffset(ac2).addOffset(ac2)} and {@code
     * ac1.addOffset(ac2).subtractOffset(ac2)} return angular coordinates equal to ac1.
     * </p>
     * @param offset offset to subtract
     * @return new instance, with offset subtracted
     * @see #addOffset(AngularCoordinates)
     */
    @Override
    public TimeStampedAngularCoordinates subtractOffset(final AngularCoordinates offset) {
        return addOffset(offset.revert());
    }

    /** Interpolate angular coordinates.
     * <p>
     * The interpolated instance is created by polynomial Hermite interpolation
     * on Rodrigues vector ensuring rotation rate remains the exact derivative of rotation.
     * </p>
     * <p>
     * This method is based on Sergei Tanygin's paper <a
     * href="http://www.agi.com/downloads/resources/white-papers/Attitude-interpolation.pdf">Attitude
     * Interpolation</a>, changing the norm of the vector to match the modified Rodrigues
     * vector as described in Malcolm D. Shuster's paper <a
     * href="http://www.ladispe.polito.it/corsi/Meccatronica/02JHCOR/2011-12/Slides/Shuster_Pub_1993h_J_Repsurv_scan.pdf">A
     * Survey of Attitude Representations</a>. This change avoids the singularity at π.
     * There is still a singularity at 2π, which is handled by slightly offsetting all rotations
     * when this singularity is detected.
     * </p>
     * <p>
     * Note that even if first and second time derivatives (rotation rates and acceleration)
     * from sample can be ignored, the interpolated instance always includes
     * interpolated derivatives. This feature can be used explicitly to
     * compute these derivatives when it would be too complex to compute them
     * from an analytical formula: just compute a few sample points from the
     * explicit formula and set the derivatives to zero in these sample points,
     * then use interpolation to add derivatives consistent with the rotations.
     * </p>
     * @param date interpolation date
     * @param filter filter for derivatives from the sample to use in interpolation
     * @param sample sample points on which interpolation should be done
     * @return a new position-velocity, interpolated at specified date
     * @exception OrekitException if the number of point is too small for interpolating
     */
    public static TimeStampedAngularCoordinates interpolate(final AbsoluteDate date,
                                                            final AngularDerivativesFilter filter,
                                                            final Collection<TimeStampedAngularCoordinates> sample)
        throws OrekitException {

        // set up safety elements for 2π singularity avoidance
        final double epsilon   = 2 * FastMath.PI / sample.size();
        final double threshold = FastMath.min(-(1.0 - 1.0e-4), -FastMath.cos(epsilon / 4));

        // set up a linear model canceling mean rotation rate
        final Vector3D meanRate;
        if (filter != AngularDerivativesFilter.USE_R) {
            Vector3D sum = Vector3D.ZERO;
            for (final TimeStampedAngularCoordinates datedAC : sample) {
                sum = sum.add(datedAC.getRotationRate());
            }
            meanRate = new Vector3D(1.0 / sample.size(), sum);
        } else {
            if (sample.size() < 2) {
                throw new OrekitException(OrekitMessages.NOT_ENOUGH_DATA_FOR_INTERPOLATION,
                                          sample.size());
            }
            Vector3D sum = Vector3D.ZERO;
            TimeStampedAngularCoordinates previous = null;
            for (final TimeStampedAngularCoordinates datedAC : sample) {
                if (previous != null) {
                    sum = sum.add(estimateRate(previous.getRotation(), datedAC.getRotation(),
                                               datedAC.date.durationFrom(previous.date)));
                }
                previous = datedAC;
            }
            meanRate = new Vector3D(1.0 / (sample.size() - 1), sum);
        }
        TimeStampedAngularCoordinates offset =
            new TimeStampedAngularCoordinates(date, Rotation.IDENTITY, meanRate, Vector3D.ZERO);

        boolean restart = true;
        for (int i = 0; restart && i < sample.size() + 2; ++i) {

            // offset adaptation parameters
            restart = false;

            // set up an interpolator taking derivatives into account
            final HermiteInterpolator interpolator = new HermiteInterpolator();

            // add sample points
            double sign = +1.0;
            Rotation previous = Rotation.IDENTITY;

            for (final TimeStampedAngularCoordinates ac : sample) {

                // remove linear offset from the current coordinates
                final double dt = ac.date.durationFrom(date);
                final TimeStampedAngularCoordinates fixed = ac.subtractOffset(offset.shiftedBy(dt));

                // make sure all interpolated points will be on the same branch
                final double dot = MathArrays.linearCombination(fixed.getRotation().getQ0(), previous.getQ0(),
                                                                fixed.getRotation().getQ1(), previous.getQ1(),
                                                                fixed.getRotation().getQ2(), previous.getQ2(),
                                                                fixed.getRotation().getQ3(), previous.getQ3());
                sign = FastMath.copySign(1.0, dot * sign);
                previous = fixed.getRotation();

                // check modified Rodrigues vector singularity
                if (fixed.getRotation().getQ0() * sign < threshold) {
                    // the sample point is close to a modified Rodrigues vector singularity
                    // we need to change the linear offset model to avoid this
                    restart = true;
                    break;
                }

                final double[][] rodrigues = fixed.getModifiedRodrigues(sign);
                switch (filter) {
                case USE_RRA:
                    // populate sample with rotation, rotation rate and acceleration data
                    interpolator.addSamplePoint(dt, rodrigues[0], rodrigues[1], rodrigues[2]);
                    break;
                case USE_RR:
                    // populate sample with rotation and rotation rate data
                    interpolator.addSamplePoint(dt, rodrigues[0], rodrigues[1]);
                    break;
                case USE_R:
                    // populate sample with rotation data only
                    interpolator.addSamplePoint(dt, rodrigues[0]);
                    break;
                default :
                    // this should never happen
                    throw OrekitException.createInternalError(null);
                }
            }

            if (restart) {
                // interpolation failed, some intermediate rotation was too close to 2π
                // we need to offset all rotations to avoid the singularity
                offset = offset.addOffset(new AngularCoordinates(new Rotation(Vector3D.PLUS_I, epsilon),
                                                                 Vector3D.ZERO, Vector3D.ZERO));
            } else {
                // interpolation succeeded with the current offset
                final DerivativeStructure zero = new DerivativeStructure(1, 2, 0, 0.0);
                final DerivativeStructure[] p = interpolator.value(zero);
                final AngularCoordinates ac = createFromModifiedRodrigues(new double[][] {
                    {
                        p[0].getValue(),              p[1].getValue(),              p[2].getValue()
                    }, {
                        p[0].getPartialDerivative(1), p[1].getPartialDerivative(1), p[2].getPartialDerivative(1)
                    }, {
                        p[0].getPartialDerivative(2), p[1].getPartialDerivative(2), p[2].getPartialDerivative(2)
                    }
                });
                return new TimeStampedAngularCoordinates(offset.getDate(),
                                                         ac.getRotation(),
                                                         ac.getRotationRate(),
                                                         ac.getRotationAcceleration()).addOffset(offset);
            }

        }

        // this should never happen
        throw OrekitException.createInternalError(null);

    }

}
