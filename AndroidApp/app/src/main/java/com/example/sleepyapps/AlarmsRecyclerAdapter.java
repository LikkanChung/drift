package com.example.sleepyapps;

import android.content.Context;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sleepyapps.ui.alarms.AlarmsFragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Instant;
import java.time.format.DateTimeParseException;

public class AlarmsRecyclerAdapter extends RecyclerView.Adapter<AlarmsRecyclerAdapter.ViewHolder> {

    private static final String LOG_TAG = "AlarmsRecyclerAdapter";
    private static final long ONE_DAY_IN_MILLIS = 1000 * 60 * 60 * 24;

    private JSONArray data;
    private AlarmsFragment clickListener;

    public AlarmsRecyclerAdapter(JSONArray data,
                                 AlarmsFragment clickListener) {
        this.data = data;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.alarms_recycler_item, parent,false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JSONObject alarm = data.optJSONObject(position);
        String timeString;
        if (alarm != null &&
                (timeString = alarm.optString("time")) != null) {
            try {
                long time = Instant.parse(timeString).toEpochMilli();
                holder.timeView.setText(DateUtils.formatDateTime(
                        holder.timeView.getContext(), time, DateUtils.FORMAT_SHOW_TIME));
                holder.dateView.setText(DateUtils.getRelativeDateTimeString(
                        holder.dateView.getContext(), time, DateUtils.MINUTE_IN_MILLIS,
                        ONE_DAY_IN_MILLIS,
                        DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_DATE));
                holder.timeView.setOnClickListener((View v) -> {
                    clickListener.onAlarmRecyclerItemTimeClick(alarm);
                });
                holder.dateView.setOnClickListener((View v) -> {
                    clickListener.onAlarmRecyclerItemDateClick(alarm);
                });
            } catch (DateTimeParseException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int getItemCount() {
        return data.length();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public View itemView;
        public TextView timeView;
        public TextView dateView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            timeView = itemView.findViewById(R.id.text_time);
            dateView = itemView.findViewById(R.id.text_date);
        }
    }

}
