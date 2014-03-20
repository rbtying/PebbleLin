#include <pebble.h>
#include "pebblelin.h"

static const uint32_t INBOX_BUFFER_SIZE = 64;
static const uint32_t OUTBOX_BUFFER_SIZE = 128;

/*!
 * Graphics elements
 */
static Window *window;
static BitmapLayer *image_layer;
static BitmapLayer *note_layer;
static GBitmap *bitmap;
static GBitmap *notes[8];

enum note_t {
    NOTE_C = 0,
    NOTE_D = 1,
    NOTE_E = 2,
    NOTE_F = 3,
    NOTE_G = 4,
    NOTE_A = 5,
    NOTE_B = 6,
    NOTE_HI_C = 7
};

/*!
 * Used to store the gravity vector, acceleration vector, and velocity vector
 */
static int g[3];
static int a[3];
static int v[3];

/*!
 * Integrates acceleration to get velocity on timer
 */
static AppTimer *accel_timer;

/*!
 * Phone timeout timer
 */
static AppTimer *phone_timer;
static const uint32_t PHONE_TIMEOUT = 2500;

/*!
 * Sends the accelerometer and velocity data to the phone
 *
 * \return Result of outbox success
 */
AppMessageResult send_msg() {
    DictionaryIterator *iter;

    AppMessageResult ret;

    if ((ret = app_message_outbox_begin(&iter)) != APP_MSG_OK) {
        return ret;
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

    return app_message_outbox_send();
}

/*!
 * Send the next message once an ack is received, so that phone is kept as up
 * to date as possible.
 *
 * \param iterator ignored
 * \param reason ignored
 * \param context ignored
 */
void outbox_fail_callback(DictionaryIterator *iterator, AppMessageResult reason, void *context) 
{
    send_msg(); 
}

/*!
 * Send the next message once an ack is received, so that phone is kept as up
 * to date as possible.
 *
 * \param iterator ignored
 * \param context ignored
 */
void outbox_success_callback(DictionaryIterator *iterator, void *context)
{
    send_msg();
}

/*!
 * Processes the message received from the phone
 *
 * \param iterator the dictionary sent to the pebble
 * \param context unused
 */
void inbox_received_callback(DictionaryIterator *iterator, void *context)
{
    // set the timeout
    if (!(app_timer_reschedule(phone_timer, PHONE_TIMEOUT))) {
        phone_timer = app_timer_register(PHONE_TIMEOUT, phone_timer_callback,
            NULL);
    }

    Tuple* tup = dict_find(iterator, 0);

    if (tup) {
        if (tup->type == TUPLE_UINT) {
            int val = -1;
            switch(tup->length) {
                case 1:
                    val = tup->value->uint8;
                    break;
                case 2:
                    val = tup->value->uint16;
                    break;
                case 4:
                    val = tup->value->uint32;
                    break;
            }
            if (val >= 0 && val < 8) {
                bitmap_layer_set_bitmap(note_layer, notes[val]);
                bitmap_layer_set_alignment(note_layer, GAlignRight);
            } else {
                bitmap_layer_set_bitmap(note_layer, bitmap);
                bitmap_layer_set_alignment(note_layer, GAlignCenter);
            }
        }
    }
}

/*!
 * Callback function for the accelerometer service, calculates the
 * low-pass-filtered and integrated data results
 *
 * \param data the accelerometer data
 * \param num_samples the number of samples in the dataset, should be 1
 */
void accel_handler(AccelData *data, uint32_t num_samples)
{
    // data is an array of num_samples elements.
    // num_samples was set when calling accel_data_service_subscribe.
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
}

/*!
 * Initializes graphic elements and subscribes to accelerometer
 * 
 * \param window the app's main window
 */
void window_load(Window *window)
{
    Layer *window_layer = window_get_root_layer(window);

    bitmap = gbitmap_create_with_resource(RESOURCE_ID_IMAGE_MAIN);

    GRect bounds = layer_get_frame(window_layer);
    image_layer = bitmap_layer_create(bounds);
    bitmap_layer_set_alignment(image_layer, GAlignCenter);

    note_layer = bitmap_layer_create(bounds);
    bitmap_layer_set_compositing_mode(note_layer, GCompOpAnd);
    bitmap_layer_set_alignment(note_layer, GAlignCenter);
    bitmap_layer_set_bitmap(note_layer, bitmap);

    bitmap_layer_set_bitmap(image_layer, bitmap);

    notes[NOTE_C] = gbitmap_create_with_resource(RESOURCE_ID_NOTE_C);
    notes[NOTE_D] = gbitmap_create_with_resource(RESOURCE_ID_NOTE_D);
    notes[NOTE_E] = gbitmap_create_with_resource(RESOURCE_ID_NOTE_E);
    notes[NOTE_F] = gbitmap_create_with_resource(RESOURCE_ID_NOTE_F);
    notes[NOTE_G] = gbitmap_create_with_resource(RESOURCE_ID_NOTE_G);
    notes[NOTE_A] = gbitmap_create_with_resource(RESOURCE_ID_NOTE_A);
    notes[NOTE_B] = gbitmap_create_with_resource(RESOURCE_ID_NOTE_B);
    notes[NOTE_HI_C] = gbitmap_create_with_resource(RESOURCE_ID_NOTE_HI_C);

    layer_add_child(window_layer, bitmap_layer_get_layer(image_layer));
    /* layer_add_child(window_layer, bitmap_layer_get_layer(note_layer)); */
    layer_insert_above_sibling(bitmap_layer_get_layer(note_layer), bitmap_layer_get_layer(image_layer));

    accel_data_service_subscribe(0, accel_handler);
    accel_service_set_sampling_rate(ACCEL_SAMPLING_50HZ);
}

/*!
 * Destroys elements when app goes out of scope, and unsubscribes from
 * the accelerometer.
 *
 * \param window the app's main window
 */
void window_unload(Window *window)
{
    // Call this before destroying text_layer, because it can change the text
    // and this must only happen while the layer exists.
    accel_data_service_unsubscribe();
    gbitmap_destroy(bitmap);

    for (int i = 0; i < 8; i++) {
        gbitmap_destroy(notes[i]);
    }
    app_timer_cancel(accel_timer);
    app_timer_cancel(phone_timer);

    bitmap_layer_destroy(image_layer);
    bitmap_layer_destroy(note_layer);
}

/*!
 * Asks for data from the accelerometer, and schedules itself for the future
 */
void accel_timer_callback()
{
    AccelData accel = {0, 0, 0, 0, 0};
    accel_service_peek(&accel);
    accel_handler(&accel, 1);
    accel_timer = app_timer_register(20, accel_timer_callback, NULL);
}

/*!
 * Resets the background image
 */
void phone_timer_callback()
{
    bitmap_layer_set_bitmap(note_layer, bitmap);
    bitmap_layer_set_alignment(note_layer, GAlignCenter);
}

/*!
 * Initializes the AppMessage communications, and registers callbacks.
 * 
 * Starts the send_msg() loop
 */
static void app_message_init(void)
{
    // Reduce the sniff interval for more responsive messaging at the expense of
    // increased energy consumption by the Bluetooth module
    // The sniff interval will be restored by the system after the app has been
    // unloaded
    app_comm_set_sniff_interval(SNIFF_INTERVAL_REDUCED);

    accel_timer = app_timer_register(20, accel_timer_callback, NULL);
    phone_timer = app_timer_register(PHONE_TIMEOUT, phone_timer_callback,
        NULL);
    app_message_register_outbox_failed(outbox_fail_callback);
    app_message_register_outbox_sent(outbox_success_callback);
    app_message_register_inbox_received(inbox_received_callback);
    
    // Init buffers
    app_message_open(INBOX_BUFFER_SIZE, OUTBOX_BUFFER_SIZE);

    // kick off send_msg
    send_msg();
}

/*!
 * Main function 
 */
int main()
{
    window = window_create();
    window_set_fullscreen(window, true);

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

