#!/bin/sh

adb root
adb remount
adb push ./permissions/privapp-permissions-com.joshuagranick.btautopin.xml /system/etc/permissions/
adb reboot
