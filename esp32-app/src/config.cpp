#include "config.h"

namespace config
{
    int ACTION_TIME = 500;
    int X1 = 5000;
    int Y1 = 2000;
    int X2 = 5000;
    int Y2 = 8000;
    int SCROLL_AMOUNT = 50;
    int SCROLL_DELAY = 50;
    int PAGE_SCROLL_AMOUNT = 800;
    int PAGE_SCROLL_STEPS = 10;

    Preferences prefs = Preferences();  
}

void config::load() {
    prefs.begin("scroller-pedal", false);

    X1 = prefs.getInt("x1", 5000);
    Y1 = prefs.getInt("y1", 2000);
    X2 = prefs.getInt("x2", 5000);
    Y2 = prefs.getInt("y2", 8000);
    ACTION_TIME = prefs.getInt("action_time", 500);
    SCROLL_AMOUNT = prefs.getInt("scroll_amount", 50);
    SCROLL_DELAY = prefs.getInt("scroll_delay", 50);
    PAGE_SCROLL_AMOUNT = prefs.getInt("p_scroll_amount", 800);
    PAGE_SCROLL_STEPS = prefs.getInt("p_scroll_steps", 10);
}

void config::update(keys key, std::string value) {

    int val = atoi(value.c_str());

    switch(key) {
        case KEY_X1:
            if(val >= 0 && val <= 10000) {
                X1 = val;
                prefs.putInt("x1", val);
            }
            break;
        case KEY_Y1:
            if(val >= 0 && val <= 10000) {
                Y1 = val;
                prefs.putInt("y1", val);
            }
            break;
        case KEY_X2:
            if(val >= 0 && val <= 10000) {
                X2 = val;
                prefs.putInt("x2", val);
            }
            break;
        case KEY_Y2:
            if(val >= 0 && val <= 10000) {
                Y2 = val;
                prefs.putInt("y2", val);
            }
            break;
        case KEY_ACTION_TIME:
            if(val > 100L && val < 5000L) {
                ACTION_TIME = val;
                prefs.putInt("action_time", val);
            }
            break;
        case KEY_SCROLL_AMOUNT:
            if(val >= 1 && val <= 500) {
                SCROLL_AMOUNT = val;
                prefs.putInt("scroll_amount", val);
            }
            break;
        case KEY_SCROLL_DELAY:
            if(val >= 1 && val <= 500) {
                SCROLL_DELAY = val;
                prefs.putInt("scroll_delay", val);
            }
            break;
        case KEY_PAGE_SCROLL_AMOUNT:
            if(val >= 10 && val <= 1000) {
                PAGE_SCROLL_AMOUNT = val;
                prefs.putInt("p_scroll_amount", val);
            }
            break;        
        case KEY_PAGE_SCROLL_STEPS:
            if(val >= 1 && val <= 50) {
                PAGE_SCROLL_STEPS = val;
                prefs.putInt("p_scroll_steps", val);
            }
            break;               
    }  
}