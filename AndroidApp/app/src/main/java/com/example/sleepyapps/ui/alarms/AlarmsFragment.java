package com.example.sleepyapps.ui.alarms;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.Cache;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.example.sleepyapps.AlarmsRecyclerAdapter;
import com.example.sleepyapps.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class AlarmsFragment extends Fragment {

    private static final RetryPolicy RETRY_POLICY =
            new DefaultRetryPolicy(1500, 0, 1);

    private static final String LOG_TAG = "AlarmsFragment";
    private static final String BUNDLE_SAVED_JSON = "array";

    private SharedPreferences spref;
    private JSONArray data;
    private RequestQueue rq;
    private RecyclerView recycler;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Cache cache = new DiskBasedCache(context.getCacheDir());
        rq = new RequestQueue(cache, new BasicNetwork(new HurlStack()));
        rq.start();

        spref = context.getSharedPreferences(
                getString(R.string.spref_file_name), Context.MODE_PRIVATE);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_alarms, container, false);

        recycler = root.findViewById(R.id.alarms_recycler);
        RecyclerView.LayoutManager lm = new LinearLayoutManager(getActivity());
        recycler.setLayoutManager(lm);

        recycler.setItemAnimator(new DefaultItemAnimator());


        if (savedInstanceState != null) {
            String savedJson = savedInstanceState.getString(BUNDLE_SAVED_JSON);
            try {
                data = new JSONArray(data);
                onJsonDataReady();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            fetchData();
        }
        return root;
    }

    private void fetchData() {
        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET, getString(R.string.api_url_alarms), null,
                onRequestSuccess, onRequestFailure) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put(getString(R.string.api_request_header_login_token),
                        spref.getString(getString(R.string.spref_login_token), null));
                return headers;
            }
        };
        request.setRetryPolicy(RETRY_POLICY);
        rq.add(request);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(BUNDLE_SAVED_JSON, data.toString());
    }

    public void onJsonDataReady() {
        Log.d(LOG_TAG, "onJsonDataReady, array size=" + data.length());
        AlarmsRecyclerAdapter newAdapter =
                new AlarmsRecyclerAdapter(data, this);

        if (recycler.getAdapter() == null)
            recycler.setAdapter(newAdapter);
        else
            recycler.swapAdapter(newAdapter, false);

        newAdapter.notifyDataSetChanged();
    }

    private final Response.Listener<JSONArray> onRequestSuccess = (JSONArray array) -> {
        data = array;
        onJsonDataReady();
    };

    private final Response.ErrorListener onRequestFailure = (VolleyError error) -> {
        NetworkResponse response = error.networkResponse;

        Log.d(LOG_TAG, "Request failed" + error.getMessage());
        if (response != null)
            Log.d(LOG_TAG, "Response as utf-8: " + StandardCharsets.UTF_8.decode(
                    ByteBuffer.wrap(response.data)));
    };

    private void updateTime(JSONObject alarm, Instant newTime) {
        StringRequest request = new StringRequest(Request.Method.PATCH,
                getString(R.string.api_url_alarms),
                (String response) -> {
                    Log.v(LOG_TAG, "updateTime request succeeded");
                    fetchData();
                },
                (VolleyError error) -> {
                    Log.e(LOG_TAG, "updateTime request failed: " + error.getMessage());
                    fetchData();
                }) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                try {
                    JSONObject json = new JSONObject();
                    json.put("aid", alarm.optLong("aid"));
                    json.put("time", newTime.toString());
                    return json.toString().getBytes(StandardCharsets.UTF_8);
                } catch (JSONException e) {
                    e.printStackTrace();
                    return super.getBody();
                }
            }
        };
    }

    private class TimeModificationListener implements TimePickerDialog.OnTimeSetListener {
        private JSONObject alarm;

        public TimeModificationListener(JSONObject alarm) {this.alarm = alarm;}

        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            Instant curTime = Instant.parse(alarm.optString("time"));
            ZonedDateTime curTimeLocal = curTime.atZone(TimeZone.getDefault().toZoneId());
            Instant newTime = curTimeLocal.withHour(hourOfDay).withMinute(minute).toInstant();
            updateTime(alarm, newTime);
        }
    }

    private class DateModificationListener implements DatePickerDialog.OnDateSetListener {
        private JSONObject alarm;

        public DateModificationListener(JSONObject alarm) {this.alarm = alarm;}

        @Override
        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
            Instant curTime = Instant.parse(alarm.optString("time"));
            ZonedDateTime curTimeLocal = curTime.atZone(TimeZone.getDefault().toZoneId());
            Instant newTime = curTimeLocal.withYear(year)
                    .withMonth(month).withDayOfMonth(dayOfMonth).toInstant();
            updateTime(alarm, newTime);
        }
    }

    public void onAlarmRecyclerItemTimeClick(JSONObject alarm) {
        String timeString = alarm.optString("time");

        if (timeString == null)
            Log.e(LOG_TAG, "no time member");

        Instant curTime = Instant.parse(timeString);
        ZonedDateTime curTimeLocal = curTime.atZone(TimeZone.getDefault().toZoneId());
        TimePickerDialog dialog = new TimePickerDialog(getContext(),
                new TimeModificationListener(alarm), curTimeLocal.getHour(),
                curTimeLocal.getMinute(), true);
        dialog.show();
    }

    public void onAlarmRecyclerItemDateClick(JSONObject alarm) {
        String timeString = alarm.optString("time");

        if (timeString == null)
            Log.e(LOG_TAG, "no time member");

        Instant curTime = Instant.parse(timeString);
        ZonedDateTime curTimeLocal = curTime.atZone(TimeZone.getDefault().toZoneId());
        DatePickerDialog dialog = new DatePickerDialog(getContext(),
                new DateModificationListener(alarm), curTimeLocal.getYear(),
                curTimeLocal.getMonthValue(), curTimeLocal.getDayOfMonth());
        dialog.show();
    }
}
