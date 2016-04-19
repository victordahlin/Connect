package com.ericsson.connect;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.linkedin.platform.APIHelper;
import com.linkedin.platform.LISessionManager;
import com.linkedin.platform.errors.LIApiError;
import com.linkedin.platform.errors.LIAuthError;
import com.linkedin.platform.listeners.ApiListener;
import com.linkedin.platform.listeners.ApiResponse;
import com.linkedin.platform.listeners.AuthListener;
import com.linkedin.platform.utils.Scope;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private TextView info;
    private LoginButton loginButton;
    private Button linkedLoginButton;
    private CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();
        setContentView(R.layout.activity_main);

        info = (TextView) findViewById(R.id.info_textview);
        loginButton = (LoginButton) findViewById(R.id.login_button);

        login_facebook();

        linkedLoginButton = (Button) findViewById(R.id.linkedin_login_button);
        linkedLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login_linkedin();
            }
        });
    }

    public void login_facebook() {
        loginButton.setReadPermissions(Arrays.asList(
                "public_profile", "email", "user_education_history"
        ));
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                // App code
                GraphRequest request = GraphRequest.newMeRequest(
                        loginResult.getAccessToken(),
                        new GraphRequest.GraphJSONObjectCallback() {
                            @Override
                            public void onCompleted(JSONObject object, GraphResponse response) {
                                try {
                                    String name = object.getString("name");
                                    String email = object.getString("email");
                                    JSONArray educationArray = object.getJSONArray("education");
                                    String cons = "";
                                    String edu = "";

                                    if (educationArray != null) {
                                        for (int i = 0; i < educationArray.length(); i++) {
                                            JSONObject e = educationArray.getJSONObject(i);

                                            if (e.toString().contains("concentration")) {
                                                JSONArray concentration = e.getJSONArray("concentration");

                                                for (int j = 0; j < concentration.length(); j++) {
                                                    JSONObject c = concentration.getJSONObject(j);
                                                    cons += c.getString("name") + "\n";
                                                }
                                            }

                                            JSONObject school = e.getJSONObject("school");
                                            String type = e.getString("type");

                                            if (type.equalsIgnoreCase("Graduate School")) {
                                                edu += school.getString("name");
                                            }
                                        }
                                    }

                                    getDataStartReviewIntent(name, email, edu, cons);

                                } catch (JSONException je) {
                                    je.getStackTrace();
                                }
                            }
                        });
                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,name,email,education");
                request.setParameters(parameters);
                request.executeAsync();

            }

            @Override
            public void onCancel() {
                info.setText("Login attempt canceled.");
            }

            @Override
            public void onError(FacebookException error) {
                info.setText("Login attempt failed.");
            }
        });
    }

    public void login_linkedin(){
        LISessionManager.getInstance(getApplicationContext()).init(this,
                buildScope(), new AuthListener() {
                    @Override
                    public void onAuthSuccess() {
                        final String url = "https://api.linkedin.com/v1/people/~:(id,formatted-name,email-address,phone-numbers)";

                        APIHelper apiHelper = APIHelper.getInstance(getApplicationContext());
                        apiHelper.getRequest(MainActivity.this, url, new ApiListener() {
                            @Override
                            public void onApiSuccess(ApiResponse result) {
                                try {
                                    JSONObject response = result.getResponseDataAsJson();
                                    String name = response.getString("formattedName");
                                    String email = response.getString("emailAddress");

                                    getDataStartReviewIntent(name, email, "", "");

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onApiError(LIApiError error) {

                            }
                        });
                    }

                    @Override
                    public void onAuthError(LIAuthError error) {
                        // TODO
                    }
                }, true);
    }

    private void getDataStartReviewIntent(String name, String email, String edu, String concentrations) {
        Intent reviewIntent = new Intent(MainActivity.this, ReviewActivity.class);
        reviewIntent.putExtra("name", name);
        reviewIntent.putExtra("email", email);
        reviewIntent.putExtra("school", edu);
        reviewIntent.putExtra("concentrations", concentrations);
        startActivity(reviewIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);

        LISessionManager.getInstance(getApplicationContext()).onActivityResult(this,
                requestCode, resultCode, data);
    }

    // This method is used to make permissions to retrieve data from linkedin
    private static Scope buildScope() {
        return Scope.build(Scope.R_BASICPROFILE, Scope.R_EMAILADDRESS);
    }
}
