#include "config.h"

namespace config
{
    int ACTION_TIME = 500;
    int X = 10000;
    int Y_TOP = 1000;
    int Y_BOTTOM = 9000;
    int SCROLL_AMOUNT = 50;
    int SCROLL_DELAY = 50;
    int PAGE_SCROLL_AMOUNT = 800;
    int PAGE_SCROLL_STEPS = 10;

    Preferences prefs = Preferences();  
}

void config::load() {
    prefs.begin("scroller-pedal", false);

    ACTION_TIME = prefs.getInt("action_time", 500);
    X = prefs.getInt("x", 10000);
    Y_TOP = prefs.getInt("y_top", 1000);
    Y_BOTTOM = prefs.getInt("y_bottom", 9000);
    SCROLL_AMOUNT = prefs.getInt("scroll_amount", 50);
    SCROLL_DELAY = prefs.getInt("scroll_delay", 50);
    PAGE_SCROLL_AMOUNT = prefs.getInt("p_scroll_amount", 800);
    PAGE_SCROLL_STEPS = prefs.getInt("p_scroll_steps", 10);
}

void config::update(keys key, std::string value) {

    int val = atoi(value.c_str());

    switch(key) {
        case KEY_ACTION_TIME:
            if(val > 100L && val < 5000L) {
                ACTION_TIME = val;
                prefs.putInt("action_time", val);
            }
            break;
        case KEY_X:
            if(val >= 0 && val <= 10000) {
                X = val;
                prefs.putInt("x", val);
            }
            break;
        case KEY_Y_TOP:
            if(val < Y_BOTTOM) {
                Y_TOP = val;
                prefs.putInt("y_top", val);
            }
            break;        
        case KEY_Y_BOTTOM:
            if(val > Y_TOP) {
                Y_BOTTOM = val;
                prefs.putInt("y_bottom", val);
            }
            break;
        case KEY_SCROLL_AMOUNT:
            if(val > 0 && val < ( Y_BOTTOM - Y_TOP)) {
                SCROLL_AMOUNT = val;
                prefs.putInt("scroll_amount", val);
            }
            break;
        case KEY_SCROLL_DELAY:
            if(val >= 0 && val < 1000) {
                SCROLL_DELAY = val;
                prefs.putInt("scroll_delay", val);
            }
            break;
        case KEY_PAGE_SCROLL_AMOUNT:
            if(val >= 0 && val < (Y_BOTTOM - Y_TOP)) {
                PAGE_SCROLL_AMOUNT = val;
                prefs.putInt("p_scroll_amount", val);
            }
            break;        
        case KEY_PAGE_SCROLL_STEPS:
            if(val >= 0 && val < 1000) {
                PAGE_SCROLL_STEPS = val;
                prefs.putInt("p_scroll_steps", val);
            }
            break;               
    }  
}