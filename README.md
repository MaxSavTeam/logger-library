# Logger

This library was developed to log all messages into a standard log and at the same time write them to a separate file for each session.

## Integration
### Step 1
Add Jitpack dependency to your project-level build.gradle
``` 
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
### Step 2
Add library dependency to your module-level build.gradle
```
dependencies {
    implementation 'com.github.MaxSavTeam:logger-library:0.4.0'
}
```

## Usage
### Initilization
Firstly, you need to initialize Logger
```
Logger.initialize(Context context, String rsaPublicKey, boolean isDebug);
```
The first argument is application context. Context is used to get application external file  

The second argument is public key for RSA. All messages will be encrypted with this key.
You can use library's public (``Logger.DEFAULT_PUBLIC_KEY``) and private (``Logger.DEFAULT_PRIVATE_KEY``) keys.
Pass ``null`` to disable encryption.

The third argument determines whether to write to the standard log.

After initialization will be created log file with information about device and application

Now you can log messages.

### Logging
There are several log levels: info (``Logger.info``), warning (``Logger.warn``), verbose (``Logger.verbose``), debug (``Logger.debug``), error (``Logger.error``)  
Also you can use short commands ``Logger.i``, ``Logger.w``, ``Logger.v``, ``Logger.d``, ``Logger.e`` respectively  

In each command should be passed tag like first argument and message like second argument  

Messgaes in log look like  
Level name first character + : + time + process id + thread id + / + tag + : + message
