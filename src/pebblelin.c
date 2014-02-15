#include <pebble.h>
#include "xprintf.h"

Window *window;
TextLayer *text_layer_1, *text_layer_2;
char tap_text[3];

int g_x = 0, g_y = 0, g_z = 0;
int a_x = 0, a_y = 0, a_z = 0;

void accel_handler(AccelData *data, uint32_t num_samples)
{
    // data is an array of num_samples elements.
    // num_samples was set when calling accel_data_service_subscribe.
    static char buf[32];

    g_x = g_x * 0.9 + data[0].x * 0.1;
    g_y = g_y * 0.9 + data[0].y * 0.1;
    g_z = g_z * 0.9 + data[0].z * 0.1;

    xsprintf(buf, "%d", g_x);

    text_layer_set_text(text_layer_1, buf);
}

void tap_handler(AccelAxisType axis, int32_t direction)
{
    // Build a short message one character at a time to cover all possible taps.

    if (direction > 0)
    {
        tap_text[0] = '+';
    } else {
        tap_text[0] = '-';
    }

    if (axis == ACCEL_AXIS_X)
    {
        tap_text[1] = 'X';
    } else if (axis == ACCEL_AXIS_Y)
    {
        tap_text[1] = 'Y';
    } else if (axis == ACCEL_AXIS_Z)
    {
        tap_text[1] = 'Z';
    }

    // The last byte must be zero to indicate end of string.
    tap_text[2] = 0;

    text_layer_set_text(text_layer_2, tap_text);
}

void window_load(Window *window)
{
    Layer *window_layer = window_get_root_layer(window);

    text_layer_1 = text_layer_create(GRect(0, 0, 144, 20));
    layer_add_child(window_layer, text_layer_get_layer(text_layer_1));

    text_layer_2 = text_layer_create(GRect(0, 20, 144, 20));
    layer_add_child(window_layer, text_layer_get_layer(text_layer_2));

    accel_data_service_subscribe(1, accel_handler);
    accel_service_set_sampling_rate(ACCEL_SAMPLING_50HZ);

    accel_tap_service_subscribe(tap_handler);
}

void window_unload(Window *window)
{
    // Call this before destroying text_layer, because it can change the text
    // and this must only happen while the layer exists.
    accel_data_service_unsubscribe();
    accel_tap_service_unsubscribe();

    text_layer_destroy(text_layer_2);
    text_layer_destroy(text_layer_1);
}

int main()
{
    window = window_create();
    window_set_window_handlers(window, (WindowHandlers)
            {
            .load = window_load,
            .unload = window_unload,
            });
    window_stack_push(window, true);
    app_event_loop();
    window_destroy(window);
}

