#include <Arduino.h>
#include <BleConfigAbsMouse.cpp>
#include "config.h"

#define BT_DEV_NAME        "Scroller Pedal"
#define PEDAL_PIN           4

enum states { STOP, WAIT_FOR_ACTION, PAGE_DOWN, PAGE_UP, SCROLL_DOWN, SCROLL_UP };

BleConfigAbsMouse bleMouse(BT_DEV_NAME);
states state = STOP;
unsigned long first_press_ms = 0L;
short x;
short y;
bool pedal_state = 0;
unsigned char presses = 0;
unsigned char releases = 0;
unsigned char page_scroll_step = 0;

void set_state_by_action();

void handle_pedal_on();
void handle_pedal_off();

void scroll_by_state();
void scroll(short add_y, short limit_y, short start_y);
void stop_scroll();

void setup() {
  Serial.begin(115200);
  pinMode(PEDAL_PIN, INPUT_PULLUP);
  bleMouse.begin();

  config::load();
  x = config::X;
  y = config::Y_BOTTOM;
}

void loop() {

  if(!bleMouse.isConnected()) {
    return;
  }

  // Init state when action delay has finished
  if(state == WAIT_FOR_ACTION && millis() - first_press_ms >= config::ACTION_TIME) {
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
  delay(config::SCROLL_DELAY);
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
        scroll(-config::SCROLL_AMOUNT, config::Y_TOP, config::Y_BOTTOM);
      }
      break;
    case PAGE_DOWN:
      Serial.print("Handling page down ");
      scroll(-config::PAGE_SCROLL_AMOUNT, config::Y_TOP, config::Y_BOTTOM);
      page_scroll_step++;
      if(page_scroll_step >= config::PAGE_SCROLL_STEPS) {
        stop_scroll();
      }
      break;
    case PAGE_UP:
      Serial.print("Handling page up ");
      scroll(config::PAGE_SCROLL_AMOUNT, config::Y_BOTTOM, config::Y_TOP);
      page_scroll_step++;
      if(page_scroll_step >= config::PAGE_SCROLL_STEPS) {
        stop_scroll();
      }
      break;
    case SCROLL_UP:
      Serial.print("Scrolling up ");
      scroll(config::SCROLL_AMOUNT, config::Y_BOTTOM, config::Y_TOP);
      break;
    case SCROLL_DOWN:
      Serial.print("Scrolling down ");
      scroll(-config::SCROLL_AMOUNT, config::Y_TOP, config::Y_BOTTOM);
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
    delay(config::SCROLL_DELAY);
  }
  y += y_amount;
  bleMouse.move(x, y);
}

void stop_scroll() {
  state = STOP;
}
