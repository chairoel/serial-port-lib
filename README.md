# serial-port-lib

[![CI](https://github.com/chairoel/serial-port-lib/actions/workflows/ci.yml/badge.svg)](https://github.com/chairoel/serial-port-lib/actions/workflows/ci.yml)
[![Release](https://github.com/chairoel/serial-port-lib/actions/workflows/release-please.yml/badge.svg)](https://github.com/chairoel/serial-port-lib/actions/workflows/release-please.yml)
[![JitPack](https://jitpack.io/v/chairoel/serial-port-lib.svg)](https://jitpack.io/#chairoel/serial-port-lib)

Android serial port library with native (JNI) support.  
Built for use with RS232/RS485 converters and USB serial chips (CH34x, Prolific, CP210x, FTDI, CDC, etc.).
<br>
<br>

---

## 📦 Installation

Add **JitPack** repository in your root `settings.gradle(.kts)`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### Then add the dependency in your module build.gradle:

```
dependencies {
    implementation("com.github.chairoel:serial-port-lib:v0.2.1")
}
```

### 👉 Check the JitPack badge above for the latest version.

<br>

## 🚀 Usage

Open a serial port

```
import com.mascill.serialport.helper.SerialHelper
import com.mascill.serialport.constant.BaudRates

val helper = object : SerialHelper("/dev/ttyS0", BaudRates.B9600) {
    override fun onDataReceived(buffer: ByteArray?) {
        buffer?.let {
            val hex = it.joinToString(" ") { b -> "%02X".format(b) }
            println("Received: $hex")
        }
    }
}

helper.open()   // open serial port
helper.sendHex("AA BB CC DD") // send hex command
```

### Close port

```
helper.close()
```

<br>

## 🛠 Features

- Kotlin-first API
- Supports multiple baud rates via BaudRates constants
- JNI integration with CMake
- Compatible with AndroidX
- Ready for JitPack distribution
  <br>
  <br>

## 🤝 Contributing

Pull requests are welcome!
For major changes, please open an issue first to discuss what you would like to change.
<br>
<br>

## 📄 License

Kalau kamu copy ini langsung ke `README.md`, repo-mu akan terlihat profesional banget: ada badge CI, Release, dan JitPack; plus ada contoh penggunaan siap pakai 🚀

Mau aku tambahin juga **contoh untuk send/receive via thread (SendThread/ReadThread)** biar user langsung ngerti use case real?
