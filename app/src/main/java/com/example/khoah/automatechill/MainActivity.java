package com.example.khoah.automatechill;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.view.View;
import android.widget.TextView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.AuthFailureError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;

import java.util.Map;
import java.util.HashMap;
import android.widget.ProgressBar;


public class MainActivity extends AppCompatActivity {
    Button CS61A;
    Button CS61B;
    Button CS70;
    EditText emailInput;
    ProgressBar spinner;

    String usernameKhoa = "supermasil";
    String emailKhoa = "khoa.hoang@berkeley.edu";
    String userNameJessie = "JessieGross";
    String JessieEmail = "jvgross@berkeley.edu";


    String tenantName = userNameJessie;
    String emailDefault = "automateandchill@gmail.com";
    String passwordDefault = "Automate&chill1";

    private static final String TAG = MainActivity.class.getName();

    String url = "https://platform.uipath.com/api/account/authenticate";
    String urlJob = "https://platform.uipath.com/odata/Jobs/UiPath.Server.Configuration.OData.StartJobs";
    JSONObject respondedParams;

    // This is from Odata/releases on Swagger
    String releaseKey = "c59d2434-0aa1-4eb7-aa02-f2528d28a6e2";

    String resultKey;
    TextView mTextView;

    String userClassChoice;

    private TextWatcher emailInputWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String email = emailInput.getText().toString();
            CS61A.setEnabled(!email.isEmpty());
            CS61B.setEnabled(!email.isEmpty());
            CS70.setEnabled(!email.isEmpty());
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //final RelativeLayout rView = new RelativeLayout(this);
        mTextView = new TextView(this);
        spinner = findViewById(R.id.progressBar);

        emailInput = findViewById(R.id.email);
        CS61A = findViewById(R.id.CS61A);
        CS61B = findViewById(R.id.CS61B);
        CS70 = findViewById(R.id.CS70);

        // Add the request to the RequestQueue;
        sendRequestAndPrintResponse();

        emailInput.addTextChangedListener(emailInputWatcher);



        CS61A.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("CS61A Clicked");
                userClassChoice = "Case1";
                startAJob(respondedParams);
                runProgressBar();
            }
        });

        CS61B.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("CS61B Clicked");
                userClassChoice = "Case2";
                startAJob(respondedParams);
                runProgressBar();
            }
        });

        CS70.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("CS70 Clicked");
                userClassChoice = "Case3";
                startAJob(respondedParams);
                runProgressBar();
            }
        });
    }

    private void sendRequestAndPrintResponse() {
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("tenancyName", tenantName);
            requestBody.put("usernameOrEmailAddress", emailDefault);
            requestBody.put("password", passwordDefault);

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                    (Request.Method.POST, url, requestBody, new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            //mTextView.setText("Response: " + response.toString());
                            Log.i(TAG, "SUCCESS: " + response);

                            respondedParams = response;
                            //startAJob(respondedParams);
                        }
                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // TODO: Handle error
                            Log.i(TAG, "ERROR: " + error);

                        }
                    });

            // Access the RequestQueue through your singleton class.
            MySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest);

        } catch (JSONException e) {
            Log.e("ExceptionError", "unexpected JSON exception", e);
            // Do something to recover ... or kill the app.
        }

    }

    private void startAJob(JSONObject respondedParams) {
        try {
            // The key got back from the server
            resultKey = respondedParams.getString("result");
        } catch (JSONException e) {
            Log.e("ExceptionError", "unexpected JSON exception", e);
        }

        JSONObject requestBody = new JSONObject();
        JSONObject innerRequestBody = new JSONObject();
        JSONObject inputArguments = new JSONObject();

        try {
            System.out.println(emailInput.getText().toString());

            // 2 arguments to the flowchart
            inputArguments.put("email", emailInput.getText().toString());
            inputArguments.put("userClassChoice", userClassChoice);

            innerRequestBody.put("ReleaseKey", releaseKey);
            innerRequestBody.put("Strategy", "All");
            innerRequestBody.put("RobotIds",new JSONArray());
            innerRequestBody.put("InputArguments", inputArguments.toString());

            requestBody.put("startInfo", innerRequestBody);
            System.out.println(requestBody);;

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest (Request.Method.POST, urlJob, requestBody, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        //mTextView.setText("Response: " + response.toString());
                        Log.i(TAG, "START JOB SUCCESSFULLY: " + response);
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Handle error
                        Log.i(TAG, "ERROR: " + error);
                    }
                }) {

                    /**
                     * Passing some request headers
                     */
                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        HashMap<String, String> headers = new HashMap<String, String>();
                        //headers.put("Content-Type", "application/json");
                        headers.put("Authorization", "Bearer " + resultKey);
                        System.out.println(resultKey);
                        return headers;
                    }
                };

            // Access the RequestQueue through your singleton class.
            MySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest);
        } catch (JSONException e) {
            Log.e("ExceptionError", "unexpected JSON exception", e);
            // Do something to recover ... or kill the app.
        }
    }

    private void runProgressBar(){
        Thread thread = new Thread() {
            @Override
            public void run() {
                for (int i = 0; i <= 100; i++) {
                    try {
                        java.lang.Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Log.i("Interupted", "Sleep interupted the program");
                    }
                    spinner.setProgress(i);
                }
            }
        };
        thread.start();
    }
}
