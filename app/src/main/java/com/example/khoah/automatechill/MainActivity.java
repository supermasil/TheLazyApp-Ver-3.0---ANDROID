package com.example.khoah.automatechill;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.view.View;
import android.widget.Spinner;
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

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import android.widget.ProgressBar;


public class MainActivity extends AppCompatActivity implements MultiSpinner.MultiSpinnerListener {
    Button go;
    Button stop;
    EditText emailInput;
    ProgressBar bar;
    Spinner spinner; // Drop down box
    MultiSpinner multiSpinner;
    TextView mTextView;
    TextView message;
    EditText editLink;

    String className;
    String classLink;
    String jobStatus;
    int jobID;

    String[] dataTypes;
    String[] classLinks;
    final int dataTypesLength = 10; // currently set up to 10 in UIPATH

    String tenantName = "JessieGross";
    String emailDefault = "automateandchill@gmail.com";
    String passwordDefault = "Automate&chill1";

    // URLs for authentication and orchestrator
    String urlAuth = "https://platform.uipath.com/api/account/authenticate";
    String urlStartJob = "https://platform.uipath.com/odata/Jobs/UiPath.Server.Configuration.OData.StartJobs";
    String urlStopJob = "https://platform.uipath.com/odata/Jobs/UiPath.Server.Configuration.OData.StopJobs";
    String urlJobStatus = "https://platform.uipath.com/odata/Jobs";

    JSONObject respondedParams; // Result from connecting to server
    HashMap<String, String> headers = new HashMap<String, String>(); // For authentication

    // This is from Odata/releases on Swagger
    String releaseKey = "c59d2434-0aa1-4eb7-aa02-f2528d28a6e2";
    String resultKey; // Result key from connecting to server

    private static final String TAG = MainActivity.class.getName();

    private TextWatcher emailInputWatcher = new TextWatcher() {
        // To disable view if no email is detected
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String email = emailInput.getText().toString();
            setView(!email.isEmpty());
        }
        @Override
        public void afterTextChanged(Editable s) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = new TextView(this);
        bar = findViewById(R.id.progressBar);
        bar.setVisibility(View.GONE);
        message = findViewById(R.id.message);
        editLink = findViewById(R.id.editLink);

        emailInput = findViewById(R.id.email);
        emailInput.addTextChangedListener(emailInputWatcher);

        dataTypes= getResources().getStringArray(R.array.dataTypes);
        classLinks= getResources().getStringArray(R.array.classLinks);

        // Class drop down menu
        spinner = findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.classNames, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                className = (String) parent.getItemAtPosition(position);
                editLink.setText(classLinks[position], TextView.BufferType.EDITABLE);
                message.setText("Please check if the link below is correct.");
                message.setVisibility(View.VISIBLE);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        go = findViewById(R.id.go);
        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Has to put it here to get the latest link
                classLink = editLink.getText().toString();
                setView(false);
                emailInput.setEnabled(false);
                stop.setVisibility(View.VISIBLE);
                go.setVisibility(View.GONE);
                startAJob(respondedParams);
                runProgressBar();
            }
        });

        stop = findViewById(R.id.stop);
        stop.setVisibility(View.GONE);
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopAJob();
            }
        });

        // Datatypes drop down menu
        multiSpinner = findViewById(R.id.multi_spinner);
        ArrayList<String> list = new ArrayList<String>();
        for (String type : dataTypes) {
            list.add(type);
        }
        multiSpinner.setItems(list, "Select file types", this);

        setView(false);
        connectToServer();
    }

    @Override
    public void onItemschecked(boolean[] checked) {
        // Nothing to do here for the multispinner
    }


    private void setView(boolean b) {
        /* To disable or enable views
         */
        go.setEnabled(b);
        editLink.setEnabled(b);
        spinner.setEnabled(b);
        multiSpinner.setEnabled(b);
    }

    private void connectToServer() {
        /* Connect to server and receive a response
         */
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("tenancyName", tenantName);
            requestBody.put("usernameOrEmailAddress", emailDefault);
            requestBody.put("password", passwordDefault);

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                    (Request.Method.POST, urlAuth, requestBody, new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            Log.i(TAG, "Connected successfully to server.");
                            respondedParams = response;
                            message.setText("Connected successfully to server.");
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e(TAG, "Cannot Connect to server: " + error);
                            message.setText("Oops! We can't connect to server, please contact admin.");
                        }
                    });

            // Access the RequestQueue through your singleton class.
            MySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest);

        } catch (JSONException e) {
            Log.e(TAG, "JSON error while connecting to server", e);
        }
    }

    private void startAJob(final JSONObject respondedParams) {
        /* Start a job using the result key from connecting to the server
         */

        try {
            resultKey = respondedParams.getString("result");
        } catch (JSONException e) {
            Log.e(TAG, "JSON error while getting result key", e);
        }

        String sentDataTypes = "";
        for (int i = 0; i < dataTypesLength; i++) {
            if(i < dataTypes.length && multiSpinner.checked[i]) {
                sentDataTypes += dataTypes[i] + ",";
            } else {
                sentDataTypes += ",";
            }
        }

        System.out.println(sentDataTypes + " selected");

        try {
            JSONObject requestBody = new JSONObject();
            JSONObject innerRequestBody = new JSONObject();
            JSONObject inputArguments = new JSONObject();

            inputArguments.put("email", emailInput.getText().toString());
            inputArguments.put("className", className);
            inputArguments.put("classLink", classLink);
            inputArguments.put("dataTypes", sentDataTypes);
            innerRequestBody.put("ReleaseKey", releaseKey);
            innerRequestBody.put("Strategy", "All");
            innerRequestBody.put("RobotIds",new JSONArray());
            innerRequestBody.put("InputArguments", inputArguments.toString());

            requestBody.put("startInfo", innerRequestBody);

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest (Request.Method.POST,
                    urlStartJob, requestBody, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Log.i(TAG, "Job started");
                    message.setText("We are working hard on it, please wait...");
                    message.setVisibility(View.VISIBLE);
                }
            }, new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "Error while starting a job", error);
                    message.setText("Oops! There's an error with this job.");
                    message.setVisibility(View.VISIBLE);
                }
            }) {
                @Override
                public Map<String, String> getHeaders()  {
                    // Has to be in this format, this has to be done to authorize the job
                    headers.put("Authorization", "Bearer " + resultKey);
                    return headers;
                }
            };

            // Access the RequestQueue through your singleton class.
            MySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest);
        } catch (JSONException e) {
            Log.e(TAG, "JSON Error while starting a job", e);
        }
    }

    private void stopAJob() {
        try {
            JSONObject requestBody = new JSONObject();
            JSONArray requestArray = new JSONArray();

            requestArray.put(jobID);
            requestBody.put("jobIds", requestArray);
            requestBody.put("strategy", "Kill");

            CustomJsonObjectRequest jsonObjectRequest = new CustomJsonObjectRequest (Request.Method.POST,
                    urlStopJob, requestBody, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Log.i(TAG, "Job stopped");
                    message.setText("Stopping jobs...");
                    message.setVisibility(View.VISIBLE);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "Error while stopping job", error);
                    message.setText("Unable to stop jobs...");
                    message.setVisibility(View.VISIBLE);
                }
            }) {
                @Override
                public Map<String, String> getHeaders() {
                    return headers;
                }
            };

            MySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest);
        } catch (JSONException e) {
            Log.e(TAG,"JSON error while stopping jobs", e);
        }
    }

    private void jobStatus() {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, urlJobStatus, new JSONObject(), new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray array = response.getJSONArray("value");
                    jobStatus = array.getJSONObject(array.length() - 1).getString("State");
                    jobID = array.getJSONObject(array.length() - 1).getInt("Id");
                    Log.i(TAG, "Job Status: " + jobStatus);
                    message.setText("Job status: " + jobStatus);
                    message.setVisibility(View.VISIBLE);
                } catch (JSONException e) {
                    Log.e(TAG, "JSON error while getting job status", e);
                }
            }}, new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "Error while getting job status ", error);
                    message.setText("Oops! We can't get job status, please contact admin.");
                    message.setVisibility(View.VISIBLE);
                }
            }) {
                @Override
                public Map<String, String> getHeaders() {
                    return headers;
                }
            };
        MySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest);
    }

    private void runProgressBar(){
        jobStatus = ""; //Reset
        bar.setVisibility(View.VISIBLE);
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    while (!jobStatus.equals("Stopped") && !jobStatus.equals("Successful") && !jobStatus.equals("Faulted")) {
                        sleep(3000);
                        jobStatus(); // Has to put it here to allow the server to update the status of new jobs
                    }
                    runOnUiThread(new Runnable() {
                        public void run() {
                            setView(true);
                            emailInput.setEnabled(true);
                            stop.setVisibility(View.GONE);
                            go.setVisibility(View.VISIBLE);
                            bar.setVisibility(View.GONE);
                        }
                    });
                } catch (InterruptedException e) {
                    Log.e(TAG, "Error while running progress bar", e);
                }
            }
        };
        thread.start();
    }
}
