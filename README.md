aerogear-android [![Build Status](https://travis-ci.org/aerogear/aerogear-android.png)](https://travis-ci.org/aerogear/aerogear-android)
================

AeroGear's Android libraries are built as jar, apklib and aar (as experimental) using [Maven](http://maven.apache.org/) and the [android-maven-plugin](https://github.com/jayway/maven-android-plugin). The project follows the standard Android project layout so sources will be in /src instead of /src/main/java and can be imported directly into IDE as an Android project.

## Build

Please take a look of the [step by step on our website](http://aerogear.org/docs/guides/aerogear-android/HowToBuildAeroGearAndroidLibrary/)

## Usage

### Referencing a library project

Download [apklib from Maven central](http://search.maven.org/#search%7Cga%7C1%7Caerogear-android) and follow the [Google Android document](http://developer.android.com/tools/projects/projects-eclipse.html#ReferencingLibraryProject)

### Maven

```
<dependency>
  <groupId>org.jboss.aerogear</groupId>
  <artifactId>aerogear-android</artifactId>
  <version>1.3.1</version>
  <scope>provided</scope>
  <type>jar</type>
</dependency>

<dependency>
  <groupId>org.jboss.aerogear</groupId>
  <artifactId>aerogear-android</artifactId>
  <version>1.3.1</version>
  <type>apklib</type>
</dependency>
```

### Gradle (as experimental)
```
dependencies {
  compile 'org.jboss.aerogear:aerogear-android:1.3.1@aar'
}
```

## Demo and Documentation

Take a look in our example apps and docs about our features

| Feature / Doc  |  Example |
|:--------------:|:--------:|
| [Pipe](http://aerogear.org/docs/guides/aerogear-android/pipe)       | [Cookbook](https://github.com/aerogear/aerogear-android-cookbook) |
| [Store](http://aerogear.org/docs/guides/aerogear-android/store)     | [Cookbook](https://github.com/aerogear/aerogear-android-cookbook) |
| [Authentication](http://aerogear.org/docs/guides/aerogear-android/) | [Cookbook](https://github.com/aerogear/aerogear-android-cookbook) |


If you are having troubles feel free to contact us via IRC #aerogear or our mailing list aerogear-dev@lists.jboss.org.