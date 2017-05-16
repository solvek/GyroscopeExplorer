package com.kircherelectronics.gyroscopeexplorer.activity.filter;

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
 * An interface for linear acceleration filters.
 *
 * @author Kaleb
 */
public interface OrientationInterface {
    /**
     * Get the orientation of the device. This method can be called *only* after
     * setAcceleration(), setMagnetic() and getGyroscope() have been called.
     *
     * @return float[] an array containing the linear acceleration of the device
     * where values[0]: azimuth, rotation around the Z axis. values[1]:
     * pitch, rotation around the X axis. values[2]: roll, rotation
     * around the Y axis. with respect to the Android coordinate system.
     */
    float[] getOrientation();

    /**
     * The complementary filter coefficient, a floating point value between 0-1,
     * exclusive of 0, inclusive of 1.
     *
     * @param filterCoefficient
     */
    void setFilterCoefficient(float filterCoefficient);
}
