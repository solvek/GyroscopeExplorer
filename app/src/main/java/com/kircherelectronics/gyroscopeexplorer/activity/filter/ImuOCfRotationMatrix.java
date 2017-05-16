package com.kircherelectronics.gyroscopeexplorer.activity.filter;

import android.content.Context;
import android.hardware.SensorManager;

/*
 * Copyright 2013-2017, Kaleb Kircher - Kircher Engineering, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * ImuOCf stands for inertial movement unit linear acceleration complementary
 * filter. Rotation Matrix is added because the filter applies the complementary
 * filter to the rotation matrices from the gyroscope and acceleration/magnetic
 * sensors, respectively.
 * <p>
 * The complementary filter is a frequency domain filter. In its strictest
 * sense, the definition of a complementary filter refers to the use of two or
 * more transfer functions, which are mathematical complements of one another.
 * Thus, if the data from one sensor is operated on by G(s), then the data from
 * the other sensor is operated on by I-G(s), and the sum of the transfer
 * functions is I, the identity matrix.
 * <p>
 * ImuOCfRotationMatrix attempts to fuse magnetometer, gravity and gyroscope
 * sensors together to produce an accurate measurement of the rotation of the
 * device.
 * <p>
 * The magnetometer and acceleration sensors are used to determine one of the
 * two orientation estimations of the device. This measurement is subject to the
 * constraint that the device must not be accelerating and hard and soft-iron
 * distortions are not present in the local magnetic field..
 * <p>
 * The gyroscope is used to determine the second of two orientation estimations
 * of the device. The gyroscope can have a shorter response time and is not
 * effected by linear acceleration or magnetic field distortions, however it
 * experiences drift and has to be compensated periodically by the
 * acceleration/magnetic sensors to remain accurate.
 * <p>
 * Rotation matrices are used to integrate the measurements of the gyroscope and
 * apply the rotations to each sensors measurements via complementary filter.
 * This is not ideal because rotation matrices suffer from singularities known
 * as gimbal lock.
 * <p>
 * The rotation matrix for the magnetic/acceleration sensor is only needed to
 * apply the weighted rotation to the gyroscopes weighted rotation via
 * complementary filter to produce the fused rotation. No integrations are
 * required.
 * <p>
 * The gyroscope provides the angular rotation speeds for all three axes. To
 * find the orientation of the device, the rotation speeds must be integrated
 * over time. This can be accomplished by multiplying the angular speeds by the
 * time intervals between sensor updates. The calculation produces the rotation
 * increment. Integrating these values again produces the absolute orientation
 * of the device. Small errors are produced at each iteration causing the gyro
 * to drift away from the true orientation.
 * <p>
 * To eliminate both the drift and noise from the orientation, the gyroscope
 * measurements are applied only for orientation changes in short time
 * intervals. The magnetometer/acceleration fusion is used for long time
 * intervals. This is equivalent to low-pass filtering of the accelerometer and
 * magnetic field sensor signals and high-pass filtering of the gyroscope
 * signals.
 *
 * @author Kaleb
 * @version %I%, %G%
 *          http://developer.android.com/reference/android/hardware/SensorEvent.html#values
 */

public class ImuOCfRotationMatrix extends Orientation {
    private static final String tag = ImuOCfRotationMatrix.class
            .getSimpleName();
    // The coefficient for the filter... 0.5 = means it is averaging the two
    // transfer functions (rotations from the gyroscope and
    // acceleration/magnetic, respectively).
    public float filterCoefficient = 0.5f;
    private float cosThetaOverTwo = 0;
    private boolean isInitialOrientationValid = false;
    private float omegaMagnitude = 0;
    // convert rotation vector into rotation matrix
    private float[] rmDeltaGyroscope = new float[9];
    // rotation matrix from gyro data
    private float[] rmOrientationGyroscope = new float[9];
    private float sinThetaOverTwo = 0;
    private float thetaOverTwo = 0;
    // copy the new gyro values into the gyro array
    // convert the raw gyro data into a rotation vector
    private float[] vDeltaGyroscope = new float[4];
    // final orientation angles from sensor fusion
    private float[] vFusedOrientation = new float[3];

    /**
     * Initialize a singleton instance.
     *
     * @param context the gravity subject.
     */
    public ImuOCfRotationMatrix(Context context) {
        super(context);

        // Initialize gyroMatrix with identity matrix
        rmOrientationGyroscope[0] = 1.0f;
        rmOrientationGyroscope[1] = 0.0f;
        rmOrientationGyroscope[2] = 0.0f;
        rmOrientationGyroscope[3] = 0.0f;
        rmOrientationGyroscope[4] = 1.0f;
        rmOrientationGyroscope[5] = 0.0f;
        rmOrientationGyroscope[6] = 0.0f;
        rmOrientationGyroscope[7] = 0.0f;
        rmOrientationGyroscope[8] = 1.0f;
    }

    /**
     * Get the orientation of the device. This method can be called *only* after
     * setAcceleration(), setMagnetic() and getGyroscope() have been called.
     *
     * @return float[] an array containing the linear acceleration of the device
     * where values[0]: azimuth, rotation around the Z axis. values[1]:
     * pitch, rotation around the X axis. values[2]: roll, rotation
     * around the Y axis. with respect to the Android coordinate system.
     */
    public float[] getOrientation() {
        return vFusedOrientation;
    }

    /**
     * Set the gyroscope rotation. Presumably from Sensor.TYPE_GYROSCOPE
     */
    @Override
    public void onGyroscopeChanged() {
        // Don't start until accelerometer/magnetometer orientation has
        // been calculated. We need that initial orientation to base our
        // gyroscope rotation off of.
        if (!isOrientationValidAccelMag) {
            return;
        }

        // Only integrate when we can measure a delta time, so one iteration
        // must pass to initialize the timeStamp.
        if (this.timeStampGyroscopeOld != 0) {
            dT = (this.timeStampGyroscope - this.timeStampGyroscopeOld) * NS2S;

            getRotationVectorFromGyro();
        }

        // measurement done, save current time for next interval
        this.timeStampGyroscopeOld = this.timeStampGyroscope;
    }

    /**
     * Reinitialize the sensor and filter.
     */
    public void reset() {
        omegaMagnitude = 0;

        thetaOverTwo = 0;
        sinThetaOverTwo = 0;
        cosThetaOverTwo = 0;

        // rotation matrix from gyro data
        rmOrientationGyroscope = new float[9];

        // final orientation angles from sensor fusion
        vFusedOrientation = new float[3];

        // copy the new gyro values into the gyro array
        // convert the raw gyro data into a rotation vector
        vDeltaGyroscope = new float[4];

        // convert rotation vector into rotation matrix
        rmDeltaGyroscope = new float[9];

        isInitialOrientationValid = false;
        isOrientationValidAccelMag = false;
    }

    /**
     * The complementary filter coefficient, a floating point value between 0-1,
     * exclusive of 0, inclusive of 1.
     *
     * @param filterCoefficient
     */
    public void setFilterCoefficient(float filterCoefficient) {
        this.filterCoefficient = filterCoefficient;
    }

    protected void calculateOrientationAccelMag() {
        super.calculateOrientationAccelMag();

        // Get an initial orientation vector from the acceleration and magnetic
        // sensors.
        if (isOrientationValidAccelMag && !isInitialOrientationValid) {
            rmOrientationGyroscope = rmOrientationAccelMag;

            isInitialOrientationValid = true;
        }
    }

    /**
     * Calculate the fused orientation. We apply the complementary filter to the
     * respective rotations of the gyroscope and accelerometer/magnetic.
     */
    private void calculateFusedOrientation() {
        // Create our scalar matrix for the gyroscope
        float[] alphaGyro = new float[]
                {filterCoefficient, 0, 0, 0, filterCoefficient, 0, 0, 0,
                        filterCoefficient};

        float oneMinusCoeff = (1.0f - filterCoefficient);

        // Create our scalar matrix for the acceleration/magnetic
        float[] alphaRotation = new float[]
                {oneMinusCoeff, 0, 0, 0, oneMinusCoeff, 0, 0, 0, oneMinusCoeff};

        // Apply the complementary filter. We multiply each rotation by their
        // coefficients (scalar matrices) and then add the two rotations
        // together.
        // output[0] = alpha * output[0] + (1 - alpha) * input[0];
        rmOrientationGyroscope = matrixAddition(
                matrixMultiplication(rmOrientationGyroscope, alphaGyro),
                matrixMultiplication(rmOrientationAccelMag, alphaRotation));

        // Finally, we get the fused orientation
        SensorManager.getOrientation(rmOrientationGyroscope, vFusedOrientation);
    }

    /**
     * Calculates a rotation vector from the gyroscope angular speed values.
     * <p>
     * http://developer.android
     * .com/reference/android/hardware/SensorEvent.html#values
     */
    private void getRotationVectorFromGyro() {

        // Calculate the angular speed of the sample
        omegaMagnitude = (float) Math.sqrt(Math.pow(vGyroscope[0], 2)
                + Math.pow(vGyroscope[1], 2) + Math.pow(vGyroscope[2], 2));

        // Normalize the rotation vector if it's big enough to get the axis
        if (omegaMagnitude > EPSILON) {
            vGyroscope[0] /= omegaMagnitude;
            vGyroscope[1] /= omegaMagnitude;
            vGyroscope[2] /= omegaMagnitude;
        }

        // Integrate around this axis with the angular speed by the timestep
        // in order to get a delta rotation from this sample over the timestep
        // We will convert this axis-angle representation of the delta rotation
        // into a quaternion before turning it into the rotation matrix.
        thetaOverTwo = omegaMagnitude * dT / 2.0f;
        sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
        cosThetaOverTwo = (float) Math.cos(thetaOverTwo);

        vDeltaGyroscope[0] = sinThetaOverTwo * vGyroscope[0];
        vDeltaGyroscope[1] = sinThetaOverTwo * vGyroscope[1];
        vDeltaGyroscope[2] = sinThetaOverTwo * vGyroscope[2];
        vDeltaGyroscope[3] = cosThetaOverTwo;

        // Get the rotation matrix from the gyroscope
        SensorManager.getRotationMatrixFromVector(rmDeltaGyroscope,
                vDeltaGyroscope);

        // Apply the new rotation interval on the gyroscope based rotation
        // matrix to form a composite rotation matrix. The product of two
        // rotation matricies is a rotation matrix...
        // Multiplication of rotation matrices corresponds to composition of
        // rotations... Which in this case are the rotation matrix from the
        // fused orientation and the rotation matrix from the current gyroscope
        // outputs.
        rmOrientationGyroscope = matrixMultiplication(rmOrientationGyroscope,
                rmDeltaGyroscope);

        calculateFusedOrientation();
    }

    /**
     * Add A by B.
     *
     * @param A
     * @param B
     * @return A+B
     */
    private float[] matrixAddition(float[] A, float[] B) {
        float[] result = new float[9];

        result[0] = A[0] + B[0];
        result[1] = A[1] + B[1];
        result[2] = A[2] + B[2];
        result[3] = A[3] + B[3];
        result[4] = A[4] + B[4];
        result[5] = A[5] + B[5];
        result[6] = A[6] + B[6];
        result[7] = A[7] + B[7];
        result[8] = A[8] + B[8];

        return result;
    }

    /**
     * Multiply A by B.
     *
     * @param A
     * @param B
     * @return A*B
     */
    private float[] matrixMultiplication(float[] A, float[] B) {
        float[] result = new float[9];

        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];

        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];

        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];

        return result;
    }

}
