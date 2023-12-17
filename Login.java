package com.accidentalspot.spot;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.accidentalspot.spot.Models.User;
import com.accidentalspot.spot.api.APIService;
import com.accidentalspot.spot.api.APIUrl;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Login extends AppCompatActivity {
    CardView btnlogin;
    private ProgressDialog pDialog;
    EditText edtpass,edtemail;
    String logginname;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        btnlogin = (CardView)findViewById(R.id.btnlogin);
        edtpass = (EditText) findViewById(R.id.edtpass);
        edtemail = (EditText) findViewById(R.id.edtemail);

        edtemail.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {

                String email = edtemail.getText().toString().trim();

                String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";

                if (!email.matches(emailPattern) && s.length() > 0) {
                    edtemail.setError("Invalid Email !");
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // other stuffs
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // other stuffs
            }
        });

        btnlogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String pass = edtpass.getText().toString();
                String email = edtemail.getText().toString();

//                Intent i = new Intent(getApplicationContext(), Home.class);
//                startActivity(i);
                if (TextUtils.isEmpty(email))
                {
                    edtemail.setError("Invalid email !");
                    edtemail.requestFocus();
                }
                else if (TextUtils.isEmpty(pass)){
                    edtpass.setError("Cannot be empty.");
                    edtpass.requestFocus();
                }else{
                    pDialog = ProgressDialog.show(Login.this, null, null, true);
                    pDialog.setContentView(R.layout.progress_dialog);
                    pDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    loginProcess(email,pass);
                }
            }
        });

    }

    private void loginProcess(String email,String pass) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(APIUrl.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create()) //Here we are using the GsonConverterFactory to directly convert json data to object
                .build();

        APIService api = retrofit.create(APIService.class);

        Call<User> call = api.userLogin(email,pass);

        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {

//                String TAG = "errrrorrrrrrrrrrr";
                User body = response.body();
                Snackbar.make(btnlogin,body.getMessage(), Snackbar.LENGTH_LONG).show();

                if (body != null) {
                    logginname = body.getName();
                    if (body.getMessage().equals("success")) {
                        Snackbar.make(btnlogin, "Login Success..", Snackbar.LENGTH_LONG).show();
                        startActivity(new Intent(getApplicationContext(),MapsActivity.class));
                        finish();
                    } else {
                        Snackbar.make(btnlogin, "Somthing went wrong", Snackbar.LENGTH_LONG).show();
//                    Log.e(TAG,"Somthing went wrong" +response);

                    }
                }else{Snackbar.make(btnlogin, "No user Found", Snackbar.LENGTH_LONG).show();}
                pDialog.dismiss();

            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
                pDialog.dismiss();
                Log.d("MY TAG","failed");
                Snackbar.make(btnlogin, t.getLocalizedMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
