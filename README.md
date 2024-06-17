# esp32-scroller-foot-pedal
Use a foot pedal to scroll text on a device over bluetooth. Example use case would be to scroll chords/lyrics on a tablet via the foot pedal whilst playing guitar.

The esp32-app code turns a NodeMCU ESP32 board into a BLE Absolute Mouse (using the excellent [ESP32-BLE-Abs-Mouse](https://github.com/sobrinho/ESP32-BLE-Abs-Mouse) library). After pairing the device (named "Scroller Pedal"), the device invokes scrolling by pressing the pedal. 

## Pedal behaviour
- Press + hold down = scroll up until pedal is released
- Press + release + press + hold down = scroll down until pedal is released
- Press + release (once) = Page Scroll down
- Press + release (twice) = Page Scroll up

Included is also a small Android app (android-config-app) for configuring the scrolling behaviour over BlueTooth (needs to be Paired with the device).

## Demo Video
[![Demo video](https://img.youtube.com/vi/rqGKLVZrP6g/0.jpg)](https://www.youtube.com/watch?v=rqGKLVZrP6g)
## Config App (Android)
![Config App Settings](https://github.com/leerikss/esp32-scroller-foot-pedal/blob/main/img/config_app1.jpg?raw=true)
![Scrolling Track Settings](https://github.com/leerikss/esp32-scroller-foot-pedal/blob/main/img/config_app2.jpg?raw=true)
