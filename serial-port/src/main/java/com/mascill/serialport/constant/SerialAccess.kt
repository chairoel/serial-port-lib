package com.mascill.serialport.constant

object SerialAccess {
   const val USB_DEVICE_PATH =
        "devices/platform/soc/4e00000.ssusb/4e00000.dwc3/xhci-hcd.2.auto/usb1/1-1/1-1.5/1-1.5:1.0"
    const val LIST_TTY_COMMAND = "ls -la /sys/class/tty/"
    const val TTY_USB_PATTERN = "ttyUSB(\\d+)"
    const val LIST_PORT_COMMAND_EXEC = "ls -l /sys/class/tty/tty*"
    const val LIST_PORT_PATTERN = "/sys/class/tty/(\\w+) -> .*?/tty/(\\w+)"
}