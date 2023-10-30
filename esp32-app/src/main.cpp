#include <Arduino.h>
#include <BleConfigAbsMouse.cpp>
#include "config.h"

#define BT_DEV_NAME        "Scroller Pedal"
#define PEDAL_PIN           1 

enum states { STOP, WAIT_FOR_ACTION, PAGE_SCROLL, PAGE_SCROLL_REVERSED, SCROLL, SCROLL_REVERSED };

BleConfigAbsMouse bleMouse(BT_DEV_NAME);
states state = STOP;
unsigned long first_press_ms = 0L;
bool pedal_state = 0;
unsigned char presses = 0;
unsigned char releases = 0;

int x = 0;
int y = 0;
int delta_x = 0;
int delta_y = 0;
double scroll_distance = 0;
int scroll_amount = 0;
int scroll_step = 0;
unsigned char page_scroll_step = 0;

void set_state_by_action();

void handle_pedal_on();
void handle_pedal_off();

void scroll_by_state();
void init_scroll(int scroll_amount);
void reverse_scroll();
void scroll();
void stop_scroll();

void setup() {
  Serial.begin(115200);
  pinMode(PEDAL_PIN, INPUT_PULLUP);
  bleMouse.begin();

  config::load();
  x = config::X1;
  y = config::Y1;
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
  // 1 click = Page jump
  if(presses == 1 && releases == 1) {
    state = PAGE_SCROLL;
    page_scroll_step = 0;
    init_scroll(config::PAGE_SCROLL_AMOUNT);
  }
  // 2 clicks = Page jump reversed
  else if(presses == 2 && releases == 2) {
    state = PAGE_SCROLL_REVERSED;
    page_scroll_step = 0;
    init_scroll(config::PAGE_SCROLL_AMOUNT);
  }
  // Pedal pressed down = scroll down
  else if(presses == 1 && releases == 0) {
    state = SCROLL;
  }
  // Pedal pressed down after 1 click = scroll reverse
  else if(presses == 2 && releases == 1) {
    state = SCROLL_REVERSED;
    init_scroll(config::SCROLL_AMOUNT);
  } 
}

void handle_pedal_on() {
  switch(state) {
    case STOP:
      Serial.println("Initial press");
      bleMouse.release();
      init_scroll(config::SCROLL_AMOUNT);
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
    case SCROLL: case SCROLL_REVERSED:
      stop_scroll();
      break;
    default:
      break;
  }
}

void scroll_by_state() {
  switch(state) {
    case WAIT_FOR_ACTION:
      // If pedal down, scroll even though final action is not yet set
      if(pedal_state == LOW) {
        Serial.print("Scrolling while waiting for action ");
        scroll();
      }
      break;
    case PAGE_SCROLL:
      Serial.print("Handling page scroll ");
      scroll();
      page_scroll_step++;
      if(page_scroll_step >= config::PAGE_SCROLL_STEPS) {
        stop_scroll();
      }
      break;
    case PAGE_SCROLL_REVERSED:
      Serial.print("Handling page scroll verversed ");
      reverse_scroll();
      page_scroll_step++;
      if(page_scroll_step >= config::PAGE_SCROLL_STEPS) {
        stop_scroll();
      }
      break;
    case SCROLL:
      Serial.print("Scrolling ");
      scroll();
      break;
    case SCROLL_REVERSED:
      Serial.print("Scrolling reversed ");
      reverse_scroll();
      break;
    default:
      break;
  }
}

void init_scroll(int s_amount) {
  int dx = config::X2 - config::X1;
  int dy = config::Y2 - config::Y1;
  int distance = sqrt(dx*dx + dy*dy);
  int moves = distance/s_amount;

  delta_x = dx/moves;
  delta_y = dy/moves;
  
  scroll_amount = s_amount;
  scroll_distance = distance;
}

void reverse_scroll() {
  scroll_step -= scroll_amount;
  if(scroll_step < 0) {
    x = config::X2;
    y = config::Y2;
    scroll_step = scroll_distance;
    bleMouse.release();
    bleMouse.move(x,y);
    delay(config::SCROLL_DELAY);    
  }
  x -= delta_x;
  y -= delta_y;
  bleMouse.move(x,y);
}

void scroll() {
  scroll_step += scroll_amount;
  if(scroll_step > scroll_distance) {
    x = config::X1;
    y = config::Y1;
    scroll_step = 0;
    bleMouse.release();
    bleMouse.move(x,y);
    delay(config::SCROLL_DELAY);
  }
  x += delta_x;
  y += delta_y;
  bleMouse.move(x,y);
}

void stop_scroll() {
  state = STOP;
}
