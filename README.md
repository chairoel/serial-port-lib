# serial-port-lib

[![CI](https://github.com/chairoel/serial-port-lib/actions/workflows/ci.yml/badge.svg)](https://github.com/chairoel/serial-port-lib/actions/workflows/ci.yml)
[![Release](https://github.com/chairoel/serial-port-lib/actions/workflows/release-please.yml/badge.svg)](https://github.com/chairoel/serial-port-lib/actions/workflows/release-please.yml)
[![JitPack](https://jitpack.io/v/chairoel/serial-port-lib.svg)](https://jitpack.io/#chairoel/serial-port-lib)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**Android Serial Port Library** with native JNI integration, Kotlin-first API, and support for accessing serial devices via `/dev/tty*` nodes.  
Includes a [sample-app](./sample-app) to demonstrate usage.

## <br>

## 📑 Table of Contents

- [✨ Features](#-features)
- [📦 Installation](#-installation)
- [🚀 Usage Example](#-usage-example)
- [🔄 Multi-thread Example](#-multi-thread-example)
- [📂 Sample App](#-sample-app)
- [🖥️ Example Output](#️-example-output)
- [⚠️ Troubleshooting](#️-troubleshooting)
- [🤝 Contributing](#-contributing)
- [📄 License](#-license)

## <br>

## ✨ Features

- Kotlin-first API design
- Multiple baud rate options via `BaudRates` constants
- Native JNI integration using CMake
- AndroidX compatible
- Distributed via **JitPack**

## <br>

## 📦 Installation

Add **JitPack** repository in your root `settings.gradle(.kts)`:

```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Then add the dependency in your module (e.g., `app/build.gradle(.kts)`):

```gradle
dependencies {
    implementation("com.github.chairoel:serial-port-lib:LATEST_VERSION")
}
```

👉 Check the [JitPack badge](https://jitpack.io/#chairoel/serial-port-lib) for the latest version.

## <br>

## 🚀 Usage Example

Example based on the [sample-app](./sample-app):

```kotlin
import com.mascill.serialport.access.SerialControl
import com.mascill.serialport.constant.BaudRates

fun main() {
    val portName = "/dev/ttyS1"
    val baudRate = BaudRates.B9600

    val serial = SerialControl(portName)

    // Apply baud rate and other parameters if needed
    serial.setBaudRate(baudRate)

    serial.open()

    // Write data
    serial.sendTxt("Hello Serial!")

    // Read data is dispatched via onDataReceived() inside SerialControl
    // Implement your handling in SerialControl#onDataReceived

    // Close when done
    serial.close()
}
```

### Typical flow:

1. Create a `SerialControl` (or your own subclass of `SerialHelper`) with the desired port.
2. Set parameters: `setBaudRate()`, `setDataBits()`, `setStopBits()`, `setParity()` (if applicable).
3. Call `open()` to initialize the connection.
4. Use `sendTxt()` or `sendHex()` to transmit data.
5. Handle incoming data in `onDataReceived()`.
6. Call `close()` when done.

> **Note:** The library communicates directly with Linux device nodes (`/dev/tty*`). Ensure your Android device or board exposes the serial port you want to use.

## <br>

## 🔄 Multi-thread Example

```kotlin
import kotlin.concurrent.thread
import com.mascill.serialport.access.SerialControl
import com.mascill.serialport.constant.BaudRates

val serial = SerialControl("/dev/ttyS1").apply {
    setBaudRate(BaudRates.B9600)
    open()
}

// Reading data in a background thread (your SerialControl overrides onDataReceived)
thread(start = true) {
    while (serial.isOpen) {
        // Data will be dispatched via onDataReceived()
        Thread.sleep(50)
    }
}

// Sending heartbeat every second
thread(start = true) {
    while (serial.isOpen) {
        serial.sendTxt("Heartbeat")
        Thread.sleep(1000)
    }
}
```

## <br>

## 📂 Sample App

Check the [sample-app](./sample-app) for a full working Android project.  
It demonstrates how to:

- Open a serial port via `/dev/tty*`
- Send/receive data
- Handle callbacks with `onDataReceived`
- Manage thread dispatch for incoming data

## <br>

## 🖥️ Example Output

Running the [sample-app](./sample-app), you might see log output similar to:

```
I/SerialPort: Opened port /dev/ttyS1 at 9600 baud
I/SerialPort: >>> Sending: Hello Serial!
I/SerialPort: <<< Received: Hello Serial!
```

Or if you connect to an external device:

```
I/SerialPort: <<< Received: Temperature=27.3°C
I/SerialPort: <<< Received: Humidity=62%
```

> The actual output depends on your connected device and baud rate settings.

## <br>

## ⚠️ Troubleshooting

- **Permission denied when opening `/dev/tty*`**  
  On stock Android devices, access to `/dev/tty*` may be restricted by SELinux.  
  👉 Solution: use a rooted device or a custom Android build that exposes the serial port.

- **No such file or directory**  
  The specified port (e.g., `/dev/ttyS1`) does not exist.  
  👉 Run `adb shell ls /dev/tty*` to check available ports on your device.

- **Received garbled/incorrect data**  
  Make sure the baud rate and other parameters match the connected device (e.g., 9600, 115200).

- **App crash on unsupported hardware**  
  If the target Android device has no exposed serial node, the library cannot open the port.  
  👉 Verify hardware support first.

## <br>

## 🤝 Contributing

Contributions are welcome!  
For major changes, please open an issue first to discuss your ideas.

## <br>

## 📄 License

This project is licensed under the [MIT License](LICENSE).
