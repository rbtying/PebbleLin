#include <pebble.h>
#include "xprintf.h"

static Window *window;
static TextLayer *text_layer_1;

static int g[3];
static int a[3];
static int v[3];

static AppTimer *timer;

void send_msg() {
    DictionaryIterator *iter;

    if (app_message_outbox_begin(&iter) != APP_MSG_OK) {
        return;
    }

    dict_write_int16(iter, 0, v[0]);
    dict_write_int16(iter, 1, v[1]);
    dict_write_int16(iter, 2, v[2]);
    dict_write_int16(iter, 3, a[0]);
    dict_write_int16(iter, 4, a[1]);
    dict_write_int16(iter, 5, a[2]);
    dict_write_int16(iter, 6, g[0]);
    dict_write_int16(iter, 7, g[1]);
    dict_write_int16(iter, 8, g[2]);

    app_message_outbox_send();
}

void outbox_fail_callback(DictionaryIterator *iterator, AppMessageResult reason, void *context) 
{
   send_msg(); 
}

void outbox_success_callback(DictionaryIterator *iterator, void *context)
{
    send_msg();
}

void accel_handler(AccelData *data, uint32_t num_samples)
{
    // data is an array of num_samples elements.
    // num_samples was set when calling accel_data_service_subscribe.
    static char buf[32];

    float alpha = 0.5;

    g[0] = g[0] * alpha + data->x * (1 - alpha);
    g[1] = g[1] * alpha + data->y * (1 - alpha);
    g[2] = g[2] * alpha + data->z * (1 - alpha);

    a[0] = data[0].x - g[0];
    a[1] = data[0].y - g[1];
    a[2] = data[0].z - g[2];

    v[0] += a[0] * (10.0 / 50);
    v[1] += a[1] * (10.0 / 50);
    v[2] += a[2] * (10.0 / 50);

    float decay = 0.99;

    v[0] = decay * v[0];
    v[1] = decay * v[1];
    v[2] = decay * v[2];

    xsprintf(buf, "%d %d", v[1], data->y);

    text_layer_set_text(text_layer_1, buf);
}

void window_load(Window *window)
{
    Layer *window_layer = window_get_root_layer(window);

    text_layer_1 = text_layer_create(GRect(0, 0, 144, 144));
    layer_add_child(window_layer, text_layer_get_layer(text_layer_1));

    accel_data_service_subscribe(0, accel_handler);
    accel_service_set_sampling_rate(ACCEL_SAMPLING_50HZ);
}

void window_unload(Window *window)
{
    // Call this before destroying text_layer, because it can change the text
    // and this must only happen while the layer exists.
    accel_data_service_unsubscribe();
    text_layer_destroy(text_layer_1);
}

void timer_callback() {
    AccelData accel = {0, 0, 0, 0, 0};
    accel_service_peek(&accel);
    accel_handler(&accel, 1);
    timer = app_timer_register(20, timer_callback, NULL);
}

static void app_message_init(void) {
    // Reduce the sniff interval for more responsive messaging at the expense of
    // increased energy consumption by the Bluetooth module
    // The sniff interval will be restored by the system after the app has been
    // unloaded
    app_comm_set_sniff_interval(SNIFF_INTERVAL_REDUCED);

    timer = app_timer_register(20, timer_callback, NULL);
    app_message_register_outbox_failed(outbox_fail_callback);
    app_message_register_outbox_sent(outbox_success_callback);
    
    // Init buffers
    app_message_open(64, 16 * 9);
    send_msg();
}

int main()
{
    window = window_create();

    app_message_init();

    window_set_window_handlers(window, (WindowHandlers)
            {
            .load = window_load,
            .unload = window_unload,
            });
    window_stack_push(window, true);
    app_event_loop();
    app_message_deregister_callbacks();
    window_destroy(window);
}

