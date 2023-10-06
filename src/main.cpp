#include <Arduino.h>
#include <BleAbsMouse.h>
#include <Preferences.h>
#include <esp_wifi.h>

#define PEDAL_PIN           4

#define ACTION_TIME         (500L)
#define X                   10000
#define Y_TOP               1000
#define Y_BOTTOM            9000
#define SCROLL_DELAY        50
#define SCROLL_AMOUNT       50
#define PAGE_SCROLL_AMOUNT  800
#define PAGE_SCROLL_STEPS   10

unsigned long action_time;
short x;
short y_top;
short y_bottom;
unsigned short scroll_amount;
unsigned short scroll_delay;
unsigned short page_scroll_amount;
unsigned short page_scroll_steps;

enum states { STOP, WAIT_FOR_ACTION, PAGE_DOWN, PAGE_UP, SCROLL_DOWN, SCROLL_UP };

BleAbsMouse bleAbsMouse("Scroller Pedal");
Preferences prefs;

states state = STOP;
unsigned long first_press_ms = 0L;
short y = Y_BOTTOM;
bool pedal_state = 0;
unsigned char presses = 0;
unsigned char releases = 0;
unsigned char page_scroll_step = 0;

void read_prefs();
void save_prefs();

void set_state_by_action();

void handle_pedal_on();
void handle_pedal_off();

void scroll_by_state();
void scroll(short add_y, short limit_y, short start_y);
void stop_scroll();

void ws_setup();

void ws_setup() {

}

void setup() {
  Serial.begin(115200);
  pinMode(PEDAL_PIN, INPUT_PULLUP);
  bleAbsMouse.begin();
  prefs.begin("scroller-pedal", false);
  read_prefs();
}

void loop() {

  if(!bleAbsMouse.isConnected()) {
    return;
  }

  // Init state when action delay has finished
  if(state == WAIT_FOR_ACTION && millis() - first_press_ms >= action_time) {
    set_state_by_action();
  }

  // Handle pedal on/off state
  pedal_state = digitalRead(PEDAL_PIN);
  if(pedal_state == LOW) {
    handle_pedal_on();
  } else {
    handle_pedal_off();
  }

  // Handle scrolling
  scroll_by_state();

  // Too fast looping seems to confuse BT...
  delay(scroll_delay);
}

void read_prefs() {
  action_time = prefs.getULong("action_time", ACTION_TIME);
  x = prefs.getShort("x", X);
  y_top = prefs.getShort("y_top", Y_TOP);
  y_bottom = prefs.getShort("y_bottom", Y_BOTTOM);
  scroll_amount = prefs.getUShort("scroll_amount", SCROLL_AMOUNT);
  scroll_delay = prefs.getUShort("scroll_delay", SCROLL_DELAY);
  page_scroll_amount = prefs.getUShort("page_scroll_amount", PAGE_SCROLL_AMOUNT);
  page_scroll_steps = prefs.getUShort("page_scroll_steps", PAGE_SCROLL_STEPS);

  y = y_bottom;
}

void save_prefs() {
  // TODO
}

void set_state_by_action() {
  // 1 click = Page down
  if(presses == 1 && releases == 1) {
    state = PAGE_DOWN;
    page_scroll_step = 0;
  }
  // 2 clicks = Page up
  else if(presses == 2 && releases == 2) {
    state = PAGE_UP;
    page_scroll_step = 0;
  }
  // Pedal pressed down = scroll down
  else if(presses == 1 && releases == 0) {
    state = SCROLL_DOWN;
  }
  // Pedal pressed down after 1 click = scroll up
  else if(presses == 2 && releases == 1) {
    state = SCROLL_UP;
  } 
}

void handle_pedal_on() {
  switch(state) {
    case STOP:
      Serial.println("Initial press");
      bleAbsMouse.release();
      state = WAIT_FOR_ACTION;
      first_press_ms = millis();
      presses = 1;
      releases = 0;
      break;
    case WAIT_FOR_ACTION:
      if(releases == 1) {
        presses = 2;
      }
      break;
  }
}

void handle_pedal_off() {
  switch(state) {
    case WAIT_FOR_ACTION:
      releases = (presses == 1) ? 1 : 2;
      break;
    case SCROLL_UP: case SCROLL_DOWN:
      stop_scroll();
      break;
  }
}

void scroll_by_state() {
  switch(state) {
    case WAIT_FOR_ACTION:
      // If pedal down, scroll down even though final action is not yet set
      if(pedal_state == LOW) {
        Serial.print("Scrolling down while waiting for action");
        scroll(-scroll_amount, y_top, y_bottom);
      }
      break;
    case PAGE_DOWN:
      Serial.print("Handling page down ");
      scroll(-page_scroll_amount, y_top, y_bottom);
      page_scroll_step++;
      if(page_scroll_step >= page_scroll_steps) {
        stop_scroll();
      }
      break;
    case PAGE_UP:
      Serial.print("Handling page up ");
      scroll(page_scroll_amount, y_bottom, y_top);
      page_scroll_step++;
      if(page_scroll_step >= page_scroll_steps) {
        stop_scroll();
      }
      break;
    case SCROLL_UP:
      Serial.print("Scrolling up ");
      scroll(scroll_amount, y_bottom, y_top);
      break;
    case SCROLL_DOWN:
      Serial.print("Scrolling down ");
      scroll(-scroll_amount, y_top, y_bottom);
      break;
  }
}

void scroll(short y_amount, short y_limit, short y_start) {
  Serial.println(y);
  if(y_amount < 0 && y + y_amount < y_limit || y_amount > 0 && y + y_amount > y_limit) {
    y = y_start;
    bleAbsMouse.release(); // TODO: Need to delay after release?
    bleAbsMouse.move(x, y);
    delay(prefs.getUInt("scroll_delay", scroll_delay));
  }
  y += y_amount;
  bleAbsMouse.move(x, y);
}

void stop_scroll() {
  state = STOP;
}