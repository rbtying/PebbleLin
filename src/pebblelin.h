#ifndef PEBBLELIN_H_
#define PEBBLELIN_H_

AppMessageResult send_msg();

void outbox_fail_callback(DictionaryIterator *iterator, AppMessageResult reason, void *context);

void outbox_success_callback(DictionaryIterator *iterator, void *context);

void inbox_received_callback(DictionaryIterator *iterator, void *context);

void accel_handler(AccelData *data, uint32_t num_samples);

void accel_timer_callback();
void phone_timer_callback();

void window_load(Window *window);
void window_unload(Window *window);

static void app_message_init(void);
int main();

#endif
