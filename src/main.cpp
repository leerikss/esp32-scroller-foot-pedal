#include <Arduino.h>
#include <ArduinoJson.h>
#include <BleAbsMouseWithCallback.cpp>
#include <Preferences.h>

#define NETWORK_NAME        "Scroller Pedal"
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
unsigned short p_scroll_amount;
unsigned short p_scroll_steps;

enum states { STOP, WAIT_FOR_ACTION, PAGE_DOWN, PAGE_UP, SCROLL_DOWN, SCROLL_UP };

Preferences prefs;
DynamicJsonDocument doc(1024);
states state = STOP;
unsigned long first_press_ms = 0L;
short y = Y_BOTTOM;
bool pedal_state = 0;
unsigned char presses = 0;
unsigned char releases = 0;
unsigned char page_scroll_step = 0;

void read_prefs();
void save_prefs();
void update_configs(std::string data);

void set_state_by_action();

void handle_pedal_on();
void handle_pedal_off();

void scroll_by_state();
void scroll(short add_y, short limit_y, short start_y);
void stop_scroll();

BleAbsMouseWithCallback bleMouse(NETWORK_NAME, update_configs);

void setup() {
  Serial.begin(115200);
  pinMode(PEDAL_PIN, INPUT_PULLUP);
  bleMouse.begin();
  prefs.begin("scroller-pedal", false);
  read_prefs();
}

void loop() {

  if(!bleMouse.isConnected()) {
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
  p_scroll_amount = prefs.getUShort("p_scroll_amount", PAGE_SCROLL_AMOUNT);
  p_scroll_steps = prefs.getUShort("p_scroll_steps", PAGE_SCROLL_STEPS);

  y = y_bottom;
}

void save_prefs() {
  prefs.putULong("action_time", action_time);
  prefs.putShort("x", x);
  prefs.putShort("y_top", y_top);
  prefs.putShort("y_bottom", y_bottom);
  prefs.putUShort("scroll_amount", scroll_amount);
  prefs.putUShort("scroll_delay", scroll_delay);
  prefs.putUShort("p_scroll_amount", p_scroll_amount);
  prefs.putUShort("p_scroll_steps", p_scroll_steps);
  
  Serial.println("Saving preferences..");
  Serial.print("action_time ");
  Serial.println(action_time);
  Serial.print("x ");
  Serial.println(x);
  Serial.print("y_top ");
  Serial.println(y_top);
  Serial.print("y_bottom ");
  Serial.println(y_bottom);
  Serial.print("scroll_amount ");
  Serial.println(scroll_amount);
  Serial.print("scroll_delay ");
  Serial.println(scroll_delay);
  Serial.print("p_scroll_amount ");
  Serial.println(p_scroll_amount);
  Serial.print("p_scroll_steps ");
  Serial.println(p_scroll_steps);

}

void update_configs(std::string json) {
  deserializeJson(doc, json);

  action_time = doc["action_time"];
  if(action_time < 100L || action_time > 5000L) {
    action_time = ACTION_TIME;
  }

  x = doc["x"];

  y_top = doc["y_top"];
  y_bottom = doc["y_bottom"];
  if(y_bottom <= y_top) {
    y_top = Y_TOP;
    y_bottom = Y_BOTTOM;
  }

  scroll_amount = doc["scroll_amount"];
  if(scroll_amount <= 0 || scroll_amount > ( y_bottom - y_top)) {
    scroll_amount = SCROLL_AMOUNT;
  }

  scroll_delay = doc["scroll_delay"];
  if(scroll_delay <= 0 || scroll_delay > 1000) {
    scroll_delay = SCROLL_DELAY;
  }

  p_scroll_amount = doc["p_scroll_amount"];
  if(p_scroll_amount <= 0 || p_scroll_amount > (y_bottom - y_top)) {
    p_scroll_amount = PAGE_SCROLL_AMOUNT;
  }

  p_scroll_steps = doc["p_scroll_steps"];
  if(p_scroll_steps <= 0 || p_scroll_steps > 1000) {
    p_scroll_steps = PAGE_SCROLL_STEPS;
  }

  save_prefs();
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
      bleMouse.release();
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
    default:
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
    default:
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
      scroll(-p_scroll_amount, y_top, y_bottom);
      page_scroll_step++;
      if(page_scroll_step >= p_scroll_steps) {
        stop_scroll();
      }
      break;
    case PAGE_UP:
      Serial.print("Handling page up ");
      scroll(p_scroll_amount, y_bottom, y_top);
      page_scroll_step++;
      if(page_scroll_step >= p_scroll_steps) {
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
    default:
      break;
  }
}

void scroll(short y_amount, short y_limit, short y_start) {
  Serial.println(y);
  if((y_amount < 0 && y + y_amount < y_limit) || 
  (y_amount > 0 && y + y_amount > y_limit)) {
    y = y_start;
    bleMouse.release();
    bleMouse.move(x, y);
    delay(prefs.getUInt("scroll_delay", scroll_delay));
  }
  y += y_amount;
  bleMouse.move(x, y);
}

void stop_scroll() {
  state = STOP;
}
