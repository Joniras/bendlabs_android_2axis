# Android Library for development with a 2-Axis Bend-Sensor
This project's purpose was to build an Android library for future work with a sepecific sensor. The used sensor is a 2-Axis Bend-Sensor from Bendlabs.
With their permission i therefore offer a library for easier development with the sensor.

To use it, simply add the following dependency to your root gradle

``` gradle
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}

```

Additionally, get the newest Version (or a specific commit) with:

``` gradle
	dependencies {
	        implementation 'com.github.Joniras:bendlabs_android_2axis:VERSION'
	}

```

replace `VERSION` with "-SNAPSHOT" for the newest or a Tag-Name or a hash from a commit.
