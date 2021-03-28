# Logger

[![Codacy Badge](https://app.codacy.com/project/badge/Grade/232b57274f444d22b61441247cd82a18)](https://www.codacy.com/gh/MaxSavTeam/logger-library/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=MaxSavTeam/logger-library&amp;utm_campaign=Badge_Grade)

This library was developed to log all messages into a standard log and at the same time write them to a separate file for each session.

## Integration
### Step 1
Add JitPack dependency to your project-level build.gradle
``` groovy
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```
### Step 2
Add library dependency to your module-level build.gradle
``` groovy
dependencies {
    implementation 'com.github.MaxSavTeam:logger-library:0.5.0'
}
```

## Usage
### Initialization
Firstly, you need to initialize Logger
``` java
Logger.initialize(Context context, String rsaPublicKey, boolean isDebug, int timerPeriod, boolean autoFlushOnException, boolean printErrorOnException);
```
`Context context` is application context. Context is used to get application external file  

`String rsaPublicKey` is public key for RSA. All messages will be encrypted with this key.
You can use library's public (``Logger.DEFAULT_PUBLIC_KEY``) and private (``Logger.DEFAULT_PRIVATE_KEY``) keys.
Pass ``null`` to disable encryption.

`boolean isDebug` determines whether to write to the standard log.

`int timerPeriod` is timer period in seconds after which Logger will clear buffer each time.
Pass <= 0 if tou don't wont autoflushing.
Default is 30.

`boolean autoFlushOnException`. If true, Logger will override Thread.defaultUncaughtExceptionHandler.
Default is true.

`boolean printErrorOnException`. If true, then when an exception occurs, the Logger will record it automatically with tag "Logger" and throw the exception further.
Also overrides Thread.defaultUncaughtExceptionHandler.
Default is true.

**Note:** if `autoFlushOnException` = true or `printErrorOnException` = true, Logger will override exception handler. **This means that if you override handler yourself, Logger will not work.**

After initialization will be created log file with information about device and application

Now you can log messages.

### Logging
There are several log levels: info (``Logger.info``), warning (``Logger.warn``), verbose (``Logger.verbose``), debug (``Logger.debug``), error (``Logger.error``)  
Also you can use short commands ``Logger.i``, ``Logger.w``, ``Logger.v``, ``Logger.d``, ``Logger.e`` respectively  

In each command should be passed tag like first argument and message like second argument  

Messages in log look like  
Level name first character + : + time + process id + thread id + / + tag + : + message
