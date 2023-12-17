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
import android.widget.TextView;
import android.widget.Toast;

import com.accidentalspot.spot.Models.User;
import com.accidentalspot.spot.api.APIService;
import com.accidentalspot.spot.api.APIUrl;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Register extends AppCompatActivity {
    CardView btnregister;
    TextView txtlogin;
    EditText edtuname,edtemail,edtpass,edtconfpass;
       private ProgressDialog pDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);
        btnregister = (CardView)findViewById(R.id.btnregister);
        txtlogin = (TextView) findViewById(R.id.txtlogin);
        edtuname = (EditText) findViewById(R.id.edtuname);
        edtemail = (EditText) findViewById(R.id.edtemail);
        edtpass = (EditText) findViewById(R.id.edtpass);
        edtconfpass = (EditText) findViewById(R.id.edtconfpass);


        btnregister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(),MainActivity.class));
            }
        });

        txtlogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(),Login.class));
            }
        });

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
        btnregister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = edtuname.getText().toString();
                String email = edtemail.getText().toString();
                String pass = edtpass.getText().toString();
                String confpass = edtconfpass.getText().toString();


                if(TextUtils.isEmpty(name))
                {
                    edtuname.setError("Cannot be empty.");
                    edtuname.requestFocus();
                }else if (TextUtils.isEmpty(email)){
                    edtemail.setError("Cannot be empty.");
                    edtemail.requestFocus();
                } else if (TextUtils.isEmpty(pass)){
                    edtpass.setError("Cannot be empty.");
                    edtpass.requestFocus();
                }else if (TextUtils.isEmpty(confpass)){
                    edtpass.setError("Cannot be empty.");
                    edtpass.requestFocus();
                }else {
                    if (!pass.equals(confpass)) {
                        Snackbar.make(view,"Password dosent match !",Snackbar.LENGTH_LONG).show();
                    }else{
                        pDialog = ProgressDialog.show(Register.this, null, null, true);
                        pDialog.setContentView(R.layout.progress_dialog);
                        pDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                        AddNewCustomer(name, email, pass);

                    }
                }

            }
        });
    }

    private void AddNewCustomer(String name,String email,String pass){

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(APIUrl.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create()) //Here we are using the GsonConverterFactory to directly convert json data to object
                .build();

        APIService api = retrofit.create(APIService.class);

        Call<User> call = api.createUser(name, email, pass);

        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                User resp = response.body();
                Snackbar.make(btnregister,resp.getMessage(), Snackbar.LENGTH_LONG).show();

                if(resp.getMessage().equals("success")){
                    Toast.makeText(Register.this, "Signup success", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(getApplicationContext(),MainActivity.class));
                    finish();
                }else if(resp.getMessage().equals("failure")){
                    Snackbar.make(btnregister,"Signup Failed", Snackbar.LENGTH_LONG).show();
                }else if(resp.getMessage().equals("user already exist"))
                {
                    Snackbar.make(btnregister,"Email is already registered", Snackbar.LENGTH_LONG).show();
                }
                pDialog.dismiss();

            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
                pDialog.dismiss();
                Log.d("MY TAG","failed");
                Snackbar.make(btnregister, t.getLocalizedMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
