GyroscopeExplorer
=================

![](http://www.kircherelectronics.com/bundles/keweb/css/images/gyroscope_explorer_phone_graphic.png?raw=true)

Gyroscope Explorer provides a working open source code example and Android application that demonstrates how to use the gyroscope sensor for measuring the rotation of an Android device. While this example is implemented with Android/Java, the jist of the algorithm can be applied to almost any hardware/language combination to determine linear acceleration.

Gyroscope Explorer contains Android classes that demonstrate how to use the Sensor.TYPE_GYROSCOPE and Sensor.TYPE_GYROSCOPE_UNCALIBRATED. This includes integrating the sensor outputs over time to describe the devices change in angles, initializing the rotation matrix, concatination of the new rotation matrix with the initial rotation matrix and providing an orientation for the concatenated rotation matrix. The Android developer documentation covers some of this information, but it is an incomplete example. Gyroscope Explorer provides an example that is fully implemented. Gyroscope Explorer provides the Earth frame orientation with the azimuth, pitch and roll and described in a clean graphical view.

Note that the gyroscope is subject to drift despite the fact that Sensor.TYPE_GYROSCOPE is supposed to compensate for drift. The gyroscope is also very drift sensitive to rapid rotations and external vibrations. Gyroscope Explorer also provides an implementation of a gyroscope sensor fusion that offers much more robust and reliable estimations of the device's roation. The sensor fusion used the acceleration sensor, magnetic sensor and gyroscope sensor to calculate rotation measurements that are not affected by rapid rotations or external vibrations.

Useful Links:

* [Gyroscope Explorer Homepage](http://www.kircherelectronics.com/gyroscopeexplorer/gyroscopeexplorer)
* [Gyroscope Explorer Community](http://kircherelectronics.com/forum/viewforum.php?f=12)
* [Gyroscope Explorer Blog Article](http://www.kircherelectronics.com/blog/index.php/11-android/sensors/15-android-gyroscope-basics)
* [Download Gyroscope Explorer from Google Play](https://play.google.com/store/apps/details?id=com.kircherelectronics.com.gyroscopeexplorer)

Written by [Kircher Electronics](https://www.kircherelectronics.com)

