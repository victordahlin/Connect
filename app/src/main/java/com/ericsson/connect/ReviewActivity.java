package com.ericsson.connect;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class ReviewActivity extends AppCompatActivity {
    private EditText mEtName;
    private EditText mEtEmail;
    private EditText mEtSchool;
    private EditText mEtConcentrations;

    private Button mSend;
    private CheckBox mConditions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        Intent intent = getIntent();

        final String name = intent.getStringExtra("name");
        final String email = intent.getStringExtra("email");
        final String school = intent.getStringExtra("school");
        final String concentrations = intent.getStringExtra("concentrations");

        init();

        mEtName.setText(name);
        mEtEmail.setText(email);
        mEtSchool.setText(school);
        mEtConcentrations.setText(concentrations);

        mSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new SendContactTask(name, email, school, concentrations).execute();
            }
        });

        mConditions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "You clicked med :D", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void init() {
        mEtName = (EditText) findViewById(R.id.name_edittext);
        mEtEmail = (EditText) findViewById(R.id.email_edittext);
        mEtSchool = (EditText) findViewById(R.id.school_edittext);
        mEtConcentrations = (EditText) findViewById(R.id.concentration_edittext);

        mSend = (Button) findViewById(R.id.send_button);

        mConditions = (CheckBox) findViewById(R.id.conditions_checkbox);
    }

    /**
     *
     */
    private class SendContactTask extends AsyncTask<Void, Void, String> {
        private final String mEmail;
        private final String mName;
        private final String mSchool;
        private final String mConcentration;

        SendContactTask(String name, String email, String school, String concentration) {
            mName = name;
            mEmail = email;
            mSchool = school;
            mConcentration = concentration;
        }

        @Override
        protected String doInBackground(Void... params) {
            HttpURLConnection connection;
            int responseCode;
            String response = "";

            try {
                final String address = "http://151.177.24.7:8000/contact";

                URL url = new URL(address);
                connection = (HttpURLConnection)url.openConnection();
                connection.setReadTimeout(10000);
                connection.setConnectTimeout(15000);
                connection.setRequestMethod("POST");
                connection.setDoInput(true);
                connection.setDoOutput(true);

                Uri.Builder uriParams = new Uri.Builder()
                        .appendQueryParameter("name", mName)
                        .appendQueryParameter("email", mEmail)
                        .appendQueryParameter("school", mSchool)
                        .appendQueryParameter("concentration", mConcentration);
                String query = uriParams.build().getEncodedQuery();

                OutputStream os = connection.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(query);
                writer.flush();
                writer.close();
                os.close();

                responseCode = connection.getResponseCode();

                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    response = readFromStream(connection.getInputStream());
                } else if(responseCode == HttpsURLConnection.HTTP_UNAUTHORIZED) {
                    response = responseCode + "";
                }
                connection.connect();

            } catch(IOException e) {
                e.printStackTrace();
            }
            return response;
        }

        /**
         *
         * @param in
         * @return
         */
        private String readFromStream(InputStream in) {
            BufferedReader br = null;
            StringBuilder response = new StringBuilder();

            try {
                br = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return response.toString();
        }

        @Override
        protected void onPostExecute(final String msg) {
            new AlertDialog.Builder(ReviewActivity.this)
                    .setTitle("Title")
                    .setMessage(msg)
                    .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .show();
        }
    }
}
