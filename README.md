aerogear-android [![Build Status](https://travis-ci.org/aerogear/aerogear-android.png)](https://travis-ci.org/aerogear/aerogear-android)
================

This project is the core Aerogear library for Android.  It is build using Maven as an apk lib.

## Get Started With AeroGear on Android 

### Introduction

AeroGear's Android libraries are built as apklib projects using Maven and the [android-maven-plugin](http://code.google.com/p/maven-android-plugin/). The project follows the standard Android project layout so sources will be in /src instead of /src/main/java and can be imported directly into Eclipse as an Android project.

After the library has been built, we will clone and run the [AeroGear Android Cookbook application](https://github.com/aerogear/aerogear-android-cookbook) to confirm it is working.

If you are unfamiliar with Maven, developerWorks has a detailed [introduction](http://www.ibm.com/developerworks/java/tutorials/j-mavenv2/).  This guide will cargo cult everything you need to build and test the AeroGear library and real familiarity isn't necessary.

### Android AeroGear Library

#### Prerequisites

* Java 7.0 (Java SDK 1.7)+
** Note Android versions less than 19 do not support try-with-resources but all other Java 7 features are available.
* Maven 3.1.1+
* Git
* Android SDK
* Need to have an AVD image running 2.3.3 (API level 10) or higher.
* Need to include the Android Support Library.
* Need to include the Google Play services.

![Android SDK Manager](http://f.cl.ly/items/0J0m3H1m440I0Y173A1G/Android%20SDK%20-%20AeroGear.png)

* You may use any IDE that supports Maven, but this guide currently focuses on the command line.


#### Setup Maven Android SDK Deployer

We need to have the [Android Maven SDK Deployer](https://github.com/mosabua/maven-android-sdk-deployer) installed and configured. These are only quickstart instructions, the Android Maven SDK Deployer GitHub has a much more in depth install guide.

Before you run the SDK Deployer, you should have installed ALL PACKAGES in the Android SDK.  Once that has finished, you may run the following.

    export ANDROID_HOME=YOUR_ANDROID_SDK_DIRECTORY
    git clone https://github.com/mosabua/maven-android-sdk-deployer.git
    cd maven-android-sdk-deployer/extras/
    mvn clean install

Now Maven will be able to include all versions of Android and its libraries as dependencies.

#### Install aerogear-android

From the command line run the following. This will clone the aerogear-android git repository (thus creating a aerogear-android directory) and then build the library and install it in your local maven repository.

    git clone https://github.com/aerogear/aerogear-android.git
    cd aerogear-android/
    mvn install

#### If your build fails with "Could not find tool 'aapt'"

Android SDK version r17 broke our Maven build tool, android-maven-plugin.  This is a known issue and will be fixed with version 3.7.0 of the tool.  However, as a workaround (in Linux and Mac envrionments), you can create a symbolic link to the missing binaries.

If you have Jelly Bean MR1 (API Level 17) installed:

     cd $ANDROID_HOME/platform-tools
     ln -s ../build-tools/android-4.2.2/aapt aapt
     ln -s ../build-tools/android-4.2.2/lib lib
     ln -s ../build-tools/android-4.2.2/aidl aidl

Some installations use a slightly different structure such that the following will work instead:

     cd $ANDROID_HOME/platform-tools
     ln -s ../build-tools/17.0.0/aapt aapt
     ln -s ../build-tools/17.0.0/lib lib
     ln -s ../build-tools/17.0.0/aidl aidl

If you are having troubles feel free to contact us via IRC #aerogear or our mailing list aerogear-dev@lists.jboss.org.
