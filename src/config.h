#ifndef CONFIG_H
#define CONFIG_H

#include <Preferences.h>
#include <string>

namespace config {
    extern int ACTION_TIME;
    extern int X;
    extern int Y_TOP;
    extern int Y_BOTTOM;
    extern int SCROLL_AMOUNT;
    extern int SCROLL_DELAY;
    extern int PAGE_SCROLL_AMOUNT;
    extern int PAGE_SCROLL_STEPS;
    extern Preferences prefs;

    enum keys {KEY_ACTION_TIME, KEY_X, KEY_Y_TOP, KEY_Y_BOTTOM, KEY_SCROLL_AMOUNT, KEY_SCROLL_DELAY, KEY_PAGE_SCROLL_AMOUNT, KEY_PAGE_SCROLL_STEPS};

    void load();
    void update(keys key, std::string value);
};

#endif