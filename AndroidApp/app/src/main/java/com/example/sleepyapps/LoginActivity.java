package com.example.sleepyapps;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.time.Duration;
import java.time.Instant;

import javax.net.ssl.HttpsURLConnection;

public class LoginActivity extends AppCompatActivity {

    private static final RetryPolicy RETRY_POLICY =
            new DefaultRetryPolicy(1500, 3, 1);
    private static final String LOG_TAG = "LoginActivity";

    private SharedPreferences spref;
    private boolean higherPermissions = false; //TODO
    private RequestQueue rq;
    private Button butt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        spref = getSharedPreferences(getString(R.string.spref_file_name), Context.MODE_PRIVATE);
        long uid = spref.getLong(getString(R.string.spref_uid), -1);
        String startTimeString = spref.getString(
                getString(R.string.spref_login_token_start), null);
        String expireTimeString = spref.getString(
                getString(R.string.spref_login_token_expire), null);

        if (uid > -1 && startTimeString != null && expireTimeString != null) {
            Instant startTime = Instant.parse(startTimeString);
            Instant endTime = Instant.parse(expireTimeString);
            Duration leaseTime = Duration.between(startTime, endTime);

            if (startTime.plus(leaseTime.dividedBy(2)).isAfter(Instant.now())) {
                MainActivity.launch(this); //Not halfway through lease yet
                return;
            }

            //Time to renew
            String username = spref.getString(
                    getString(R.string.spref_cache_username), null);
            if (username != null)
                ((TextView) findViewById(R.id.text_username)).setText(username);

            TextView explanation = findViewById(R.id.text_relog_explanation);
            explanation.setMovementMethod(LinkMovementMethod.getInstance());
            explanation.setVisibility(View.VISIBLE);
        }

        rq = Volley.newRequestQueue(this);
        rq.start();

        butt = findViewById(R.id.butt_login);
        butt.setOnClickListener((View view) -> {
            butt.setEnabled(false);
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.GET, getString(R.string.api_url_login),
                    null,
                    onSuccess,
                    onFailure
            );
            request.setRetryPolicy(RETRY_POLICY);
            rq.add(request);
        });
    }

    private final Response.Listener<JSONObject> onSuccess = (JSONObject obj) -> {
        String begins = obj.optString("begins", null);
        String expires = obj.optString("expires", null);
        long uid = obj.optLong("uid", -1);
        String token = obj.optString("token", null);

        if (begins == null || expires == null || uid > 0 || token == null) {
            butt.setEnabled(true);
            Log.e(LOG_TAG, "Missing fields in api response!");
            return;
        }

        SharedPreferences.Editor editor = spref.edit();
        editor.putString(getString(R.string.spref_login_token_start), begins);
        editor.putString(getString(R.string.spref_login_token_expire), expires);
        editor.putLong(getString(R.string.spref_uid), uid);
        editor.putString(getString(R.string.spref_login_token), token);
        editor.commit();

        MainActivity.launch(this);
    };

    private final Response.ErrorListener onFailure = (VolleyError error) -> {
        butt.setEnabled(true);
        TextView explanation = findViewById(R.id.text_error_explanation);

        NetworkResponse response = error.networkResponse;
        if (response == null) {
            explanation.setText(getString(R.string.login_no_response_explanation));
        } else {
            switch (response.statusCode) {
                case HttpsURLConnection.HTTP_UNAUTHORIZED:
                    explanation.setText(getString(R.string.login_unauthorized_response_explanation));
                    break;
                case HttpsURLConnection.HTTP_INTERNAL_ERROR:
                    explanation.setText(R.string.login_interal_error_response_explanation);
                    break;
                default:
                    explanation.setText(getString(R.string.error_generic));
                    Log.e(LOG_TAG, "Got status code " + response.statusCode
                        + " from login request");
                    break;
            }
        }

        explanation.setVisibility(View.VISIBLE);
    };
}
