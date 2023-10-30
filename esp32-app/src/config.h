#ifndef CONFIG_H
#define CONFIG_H

#include <Preferences.h>
#include <string>

namespace config {
    extern int X1;
    extern int Y1;
    extern int X2;
    extern int Y2;
    extern int ACTION_TIME;
    extern int SCROLL_AMOUNT;
    extern int SCROLL_DELAY;
    extern int PAGE_SCROLL_AMOUNT;
    extern int PAGE_SCROLL_STEPS;

    extern Preferences prefs;

    enum keys {KEY_ACTION_TIME, KEY_X1, KEY_Y1, KEY_X2, KEY_Y2, KEY_SCROLL_AMOUNT, KEY_SCROLL_DELAY, KEY_PAGE_SCROLL_AMOUNT, KEY_PAGE_SCROLL_STEPS};

    void load();
    void update(keys key, std::string value);
};

#endif