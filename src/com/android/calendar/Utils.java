/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calendar;

import static android.provider.Calendar.EVENT_BEGIN_TIME;

import com.android.calendar.CalendarController.ViewType;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.CalendarUtils.TimeZoneUtils;
import android.util.Log;

import java.util.Calendar;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class Utils {
    private static final boolean DEBUG = true;
    private static final String TAG = "CalUtils";
    // Set to 0 until we have UI to perform undo
    public static final long UNDO_DELAY = 0;

    // For recurring events which instances of the series are being modified
    public static final int MODIFY_UNINITIALIZED = 0;
    public static final int MODIFY_SELECTED = 1;
    public static final int MODIFY_ALL_FOLLOWING = 2;
    public static final int MODIFY_ALL = 3;

    // When the edit event view finishes it passes back the appropriate exit code.
    public static final int DONE_REVERT = 0;
    public static final int DONE_SAVE = 1;
    public static final int DONE_DELETE = 2;

    private static final int CLEAR_ALPHA_MASK = 0x00FFFFFF;
    private static final int HIGH_ALPHA = 255 << 24;
    private static final int MED_ALPHA = 180 << 24;
    private static final int LOW_ALPHA = 150 << 24;

    protected static final String OPEN_EMAIL_MARKER = " <";
    protected static final String CLOSE_EMAIL_MARKER = ">";
    /* The corner should be rounded on the top right and bottom right */
    private static final float[] CORNERS = new float[] {0, 0, 5, 5, 5, 5, 0, 0};

    public static final String INTENT_KEY_DETAIL_VIEW = "DETAIL_VIEW";
    public static final String INTENT_KEY_VIEW_TYPE = "VIEW";
    public static final String INTENT_VALUE_VIEW_TYPE_DAY = "DAY";

    // The name of the shared preferences file. This name must be maintained for
    // historical
    // reasons, as it's what PreferenceManager assigned the first time the file
    // was created.
    private static final String SHARED_PREFS_NAME = "com.android.calendar_preferences";

    private static final TimeZoneUtils mTZUtils = new TimeZoneUtils(SHARED_PREFS_NAME);

    public static int getViewTypeFromIntentAndSharedPref(Activity activity) {
        Intent intent = activity.getIntent();
        Bundle extras = intent.getExtras();
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(activity);

        if (TextUtils.equals(intent.getAction(),Intent.ACTION_EDIT)) {
            return ViewType.EDIT;
        }
        if (extras != null) {
            if (extras.getBoolean(INTENT_KEY_DETAIL_VIEW, false)) {
                // This is the "detail" view which is either agenda or day view
                return prefs.getInt(GeneralPreferences.KEY_DETAILED_VIEW,
                        GeneralPreferences.DEFAULT_DETAILED_VIEW);
            } else if (INTENT_VALUE_VIEW_TYPE_DAY.equals(extras.getString(INTENT_KEY_VIEW_TYPE))) {
                // Not sure who uses this. This logic came from LaunchActivity
                return ViewType.DAY;
            }
        }

        // Default to the last view
        return prefs.getInt(GeneralPreferences.KEY_START_VIEW,
                GeneralPreferences.DEFAULT_START_VIEW);
    }

    /**
     * Writes a new home time zone to the db.
     *
     * Updates the home time zone in the db asynchronously and updates
     * the local cache. Sending a time zone of **tbd** will cause it to
     * be set to the device's time zone. null or empty tz will be ignored.
     *
     * @param context The calling activity
     * @param timeZone The time zone to set Calendar to, or **tbd**
     */
    public static void setTimeZone(Context context, String timeZone) {
        mTZUtils.setTimeZone(context, timeZone);
    }

    /**
     * Gets the time zone that Calendar should be displayed in
     *
     * This is a helper method to get the appropriate time zone for Calendar. If this
     * is the first time this method has been called it will initiate an asynchronous
     * query to verify that the data in preferences is correct. The callback supplied
     * will only be called if this query returns a value other than what is stored in
     * preferences and should cause the calling activity to refresh anything that
     * depends on calling this method.
     *
     * @param context The calling activity
     * @param callback The runnable that should execute if a query returns new values
     * @return The string value representing the time zone Calendar should display
     */
    public static String getTimeZone(Context context, Runnable callback) {
        return mTZUtils.getTimeZone(context, callback);
    }

    /**
     * Formats a date or a time range according to the local conventions.
     *
     * @param context the context is required only if the time is shown
     * @param startMillis the start time in UTC milliseconds
     * @param endMillis the end time in UTC milliseconds
     * @param flags a bit mask of options See
     * {@link #formatDateRange(Context, Formatter, long, long, int, String) formatDateRange}
     * @return a string containing the formatted date/time range.
     */
    public static String formatDateRange(Context context, long startMillis,
            long endMillis, int flags) {
        return mTZUtils.formatDateRange(context, startMillis, endMillis, flags);
    }

    public static String getSharedPreference(Context context, String key, String defaultValue) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        return prefs.getString(key, defaultValue);
    }

    public static int getSharedPreference(Context context, String key, int defaultValue) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        return prefs.getInt(key, defaultValue);
    }

    /**
     * Asynchronously sets the preference with the given key to the given value
     *
     * @param context the context to use to get preferences from
     * @param key the key of the preference to set
     * @param value the value to set
     */
    public static void setSharedPreference(Context context, String key, String value) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        prefs.edit().putString(key, value).apply();
    }

    static void setSharedPreference(Context context, String key, boolean value) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    /**
     * Save default agenda/day/week/month view for next time
     *
     * @param context
     * @param viewId {@link CalendarController.ViewType}
     */
    static void setDefaultView(Context context, int viewId) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();

        if (viewId == CalendarController.ViewType.AGENDA
                || viewId == CalendarController.ViewType.DAY) {
            // Record the (new) detail start view only for Agenda and Day
            editor.putInt(GeneralPreferences.KEY_DETAILED_VIEW, viewId);
        }

        // Record the (new) start view
        editor.putInt(GeneralPreferences.KEY_START_VIEW, viewId);
        editor.apply();
    }

    public static MatrixCursor matrixCursorFromCursor(Cursor cursor) {
        MatrixCursor newCursor = new MatrixCursor(cursor.getColumnNames());
        int numColumns = cursor.getColumnCount();
        String data[] = new String[numColumns];
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            for (int i = 0; i < numColumns; i++) {
                data[i] = cursor.getString(i);
            }
            newCursor.addRow(data);
        }
        return newCursor;
    }

    /**
     * Compares two cursors to see if they contain the same data.
     *
     * @return Returns true of the cursors contain the same data and are not null, false
     * otherwise
     */
    public static boolean compareCursors(Cursor c1, Cursor c2) {
        if(c1 == null || c2 == null) {
            return false;
        }

        int numColumns = c1.getColumnCount();
        if (numColumns != c2.getColumnCount()) {
            return false;
        }

        if (c1.getCount() != c2.getCount()) {
            return false;
        }

        c1.moveToPosition(-1);
        c2.moveToPosition(-1);
        while(c1.moveToNext() && c2.moveToNext()) {
            for(int i = 0; i < numColumns; i++) {
                if(!TextUtils.equals(c1.getString(i), c2.getString(i))) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * If the given intent specifies a time (in milliseconds since the epoch),
     * then that time is returned. Otherwise, the current time is returned.
     */
    public static final long timeFromIntentInMillis(Intent intent) {
        // If the time was specified, then use that.  Otherwise, use the current time.
        Uri data = intent.getData();
        long millis = intent.getLongExtra(EVENT_BEGIN_TIME, -1);
        if (millis == -1 && data != null && data.isHierarchical()) {
            List<String> path = data.getPathSegments();
            if(path.size() == 2 && path.get(0).equals("time")) {
                try {
                    millis = Long.valueOf(data.getLastPathSegment());
                } catch (NumberFormatException e) {
                    Log.i("Calendar", "timeFromIntentInMillis: Data existed but no valid time " +
                            "found. Using current time.");
                }
            }
        }
        if (millis <= 0) {
            millis = System.currentTimeMillis();
        }
        return millis;
    }

    public static Drawable getColorChip(int color) {
        /*
         * We want the color chip to have a nice gradient using
         * the color of the calendar. To do this we use a GradientDrawable.
         * The color supplied has an alpha of FF so we first do:
         * color & 0x00FFFFFF
         * to clear the alpha. Then we add our alpha to it.
         * We use 3 colors to get a step effect where it starts off very
         * light and quickly becomes dark and then a slow transition to
         * be even darker.
         */
        color &= CLEAR_ALPHA_MASK;
        int startColor = color | HIGH_ALPHA;
        int middleColor = color | MED_ALPHA;
        int endColor = color | LOW_ALPHA;
        int[] colors = new int[] {startColor, middleColor, endColor};
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors);
        d.setCornerRadii(CORNERS);
        return d;
    }

    /**
     * Formats the given Time object so that it gives the month and year
     * (for example, "September 2007").
     *
     * @param time the time to format
     * @return the string containing the weekday and the date
     */
    public static String formatMonthYear(Context context, Time time) {
        return time.format(context.getResources().getString(R.string.month_year));
    }

    /**
     * Returns a list joined together by the provided delimiter, for example,
     * ["a", "b", "c"] could be joined into "a,b,c"
     *
     * @param things the things to join together
     * @param delim the delimiter to use
     * @return a string contained the things joined together
     */
    public static String join(List<?> things, String delim) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Object thing : things) {
            if (first) {
                first = false;
            } else {
                builder.append(delim);
            }
            builder.append(thing.toString());
        }
        return builder.toString();
    }

    /**
     * Get first day of week as android.text.format.Time constant.
     * @return the first day of week in android.text.format.Time
     */
    public static int getFirstDayOfWeek(Context context) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        String pref = prefs.getString(GeneralPreferences.KEY_WEEK_START_DAY,
                GeneralPreferences.WEEK_START_DEFAULT);

        int startDay;
        if (GeneralPreferences.WEEK_START_DEFAULT.equals(pref)) {
            startDay = Calendar.getInstance().getFirstDayOfWeek();
        } else {
            startDay = Integer.parseInt(pref);
        }

        if (startDay == Calendar.SATURDAY) {
            return Time.SATURDAY;
        } else if (startDay == Calendar.MONDAY) {
            return Time.MONDAY;
        } else {
            return Time.SUNDAY;
        }
    }

    /**
     * Determine whether the column position is Saturday or not.
     * @param column the column position
     * @param firstDayOfWeek the first day of week in android.text.format.Time
     * @return true if the column is Saturday position
     */
    public static boolean isSaturday(int column, int firstDayOfWeek) {
        return (firstDayOfWeek == Time.SUNDAY && column == 6)
            || (firstDayOfWeek == Time.MONDAY && column == 5)
            || (firstDayOfWeek == Time.SATURDAY && column == 0);
    }

    /**
     * Determine whether the column position is Sunday or not.
     * @param column the column position
     * @param firstDayOfWeek the first day of week in android.text.format.Time
     * @return true if the column is Sunday position
     */
    public static boolean isSunday(int column, int firstDayOfWeek) {
        return (firstDayOfWeek == Time.SUNDAY && column == 0)
            || (firstDayOfWeek == Time.MONDAY && column == 6)
            || (firstDayOfWeek == Time.SATURDAY && column == 1);
    }

    /**
     * Convert given UTC time into current local time.
     *
     * @param recycle Time object to recycle, otherwise null.
     * @param utcTime Time to convert, in UTC.
     */
    public static long convertUtcToLocal(Time recycle, long utcTime) {
        if (recycle == null) {
            recycle = new Time();
        }
        recycle.timezone = Time.TIMEZONE_UTC;
        recycle.set(utcTime);
        recycle.timezone = TimeZone.getDefault().getID();
        return recycle.normalize(true);
    }

    /**
     * Scan through a cursor of calendars and check if names are duplicated.
     *
     * This travels a cursor containing calendar display names and fills in the provided map with
     * whether or not each name is repeated.
     * @param isDuplicateName The map to put the duplicate check results in.
     * @param cursor The query of calendars to check
     * @param nameIndex The column of the query that contains the display name
     */
    public static void checkForDuplicateNames(Map<String, Boolean> isDuplicateName, Cursor cursor,
            int nameIndex) {
        isDuplicateName.clear();
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            String displayName = cursor.getString(nameIndex);
            // Set it to true if we've seen this name before, false otherwise
            if (displayName != null) {
                isDuplicateName.put(displayName, isDuplicateName.containsKey(displayName));
            }
        }
    }

    /**
     * Null-safe object comparison
     * @param s1
     * @param s2
     * @return
     */
    public static boolean equals(Object o1, Object o2) {
        return o1 == null ? o2 == null : o1.equals(o2);
    }
}
