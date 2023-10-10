#include <BleAbsMouse.h>
#include <string>
#include "config.h"

#define  UUID_SERVICE               "a89d0000-543c-4855-be30-f2270a00a83b"
#define  UUID_ACTION_TIME           "a89d0001-543c-4855-be30-f2270a00a83b"
#define  UUID_X                     "a89d0002-543c-4855-be30-f2270a00a83b"
#define  UUID_Y_TOP                 "a89d0003-543c-4855-be30-f2270a00a83b"
#define  UUID_Y_BOTTOM              "a89d0004-543c-4855-be30-f2270a00a83b"
#define  UUID_SCROLL_AMOUNT         "a89d0005-543c-4855-be30-f2270a00a83b"
#define  UUID_SCROLL_DELAY          "a89d0006-543c-4855-be30-f2270a00a83b"
#define  UUID_PAGE_SCROLL_AMOUNT    "a89d0007-543c-4855-be30-f2270a00a83b"
#define  UUID_PAGE_SCROLL_STEPS     "a89d0008-543c-4855-be30-f2270a00a83b"

class Callback: public BLECharacteristicCallbacks {
    private:
        config::keys confKey;

    public:
        Callback(config::keys key) {
            this->confKey = key;
        }
        void onWrite(BLECharacteristic *pCharacteristic) {
            config::update(confKey, pCharacteristic->getValue());
        }
};

class BleConfigAbsMouse : public BleAbsMouse {

    private:
        void addCharactersistic(BLEService *configService, const char* uuid, config::keys configKey, std::string value) {
            BLECharacteristic *characteristic = configService->createCharacteristic(uuid, BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_WRITE);
            characteristic->setCallbacks(new Callback(configKey));
            characteristic->setValue(value);
        }

    public:
        BleConfigAbsMouse(std::string deviceName) : BleAbsMouse(deviceName) {
        }
        
        void onStarted(BLEServer *pServer) {
            BLEService *configService = pServer->createService(BLEUUID(String(UUID_SERVICE).c_str()), 32); // 8 characteristics takes at least 16 handles

            this->addCharactersistic(configService, UUID_ACTION_TIME, config::keys::KEY_ACTION_TIME, String(config::ACTION_TIME).c_str());
            this->addCharactersistic(configService, UUID_X, config::keys::KEY_X, String(config::X).c_str());
            this->addCharactersistic(configService, UUID_Y_TOP, config::keys::KEY_Y_TOP, String(config::Y_TOP).c_str());
            this->addCharactersistic(configService, UUID_Y_BOTTOM, config::keys::KEY_Y_BOTTOM, String(config::Y_BOTTOM).c_str());
            this->addCharactersistic(configService, UUID_SCROLL_AMOUNT, config::keys::KEY_SCROLL_AMOUNT, String(config::SCROLL_AMOUNT).c_str());
            this->addCharactersistic(configService, UUID_SCROLL_DELAY, config::keys::KEY_SCROLL_DELAY, String(config::SCROLL_DELAY).c_str());
            this->addCharactersistic(configService, UUID_PAGE_SCROLL_AMOUNT, config::keys::KEY_PAGE_SCROLL_AMOUNT, String(config::PAGE_SCROLL_AMOUNT).c_str());
            this->addCharactersistic(configService, UUID_PAGE_SCROLL_STEPS, config::keys::KEY_PAGE_SCROLL_STEPS, String(config::PAGE_SCROLL_STEPS).c_str());
            
            configService->start();
            BLEAdvertising *pAdvertising = pServer->getAdvertising();
            pAdvertising->addServiceUUID(UUID_SERVICE);
        }

};


