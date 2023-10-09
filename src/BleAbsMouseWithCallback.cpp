#include <BleAbsMouse.h>
#include <string>

#define  CONFIGURATION_SERVICE_UUID             "3dd8c2d5-1e16-4f87-b19f-c9bdcc37d164"
#define  CONFIGURATION_CHARACTERISTICS_UUID     "35e792b3-df3e-45a2-b23d-ea48c59472fd"

class Callback: public BLECharacteristicCallbacks {
    
    public:
        void (*callbackFn)(std::string);

    void onWrite(BLECharacteristic *pCharacteristic) {
        callbackFn(pCharacteristic->getValue());
    }
};

class BleAbsMouseWithCallback : public BleAbsMouse {

    Callback* callback;

    public:
        BleAbsMouseWithCallback(std::string deviceName, void (*callbackFn)(std::string)) : BleAbsMouse(deviceName) {
            callback = new Callback;
            callback->callbackFn = callbackFn;
        }
        
    void onStarted(BLEServer *pServer) {
        BLEService *configService = pServer->createService(CONFIGURATION_SERVICE_UUID);
        BLECharacteristic *configCharacteristic = configService->createCharacteristic(CONFIGURATION_CHARACTERISTICS_UUID, BLECharacteristic::PROPERTY_WRITE);
        configCharacteristic->setCallbacks(callback);
        configService->start();

        BLEAdvertising *pAdvertising = pServer->getAdvertising();
        pAdvertising->addServiceUUID(CONFIGURATION_SERVICE_UUID);
    }
};


