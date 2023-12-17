package com.accidentalspot.spot.api;

import com.accidentalspot.spot.Models.LocationObjects;
import com.accidentalspot.spot.Models.User;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;


public interface APIService {

//    @GET("")
//    Call<LocationObjects> getData();

    @FormUrlEncoded
    @POST("AppData/register.php")
    Call<User> createUser(
            @Field("name") String name,
            @Field("email") String email,
            @Field("password") String password);


    @FormUrlEncoded
    @POST("AppData/login.php")
    Call<User> userLogin(
            @Field("email") String email,
            @Field("password") String password
    );

    @GET("AppData/getData.php")
    Call<LocationObjects> getData();

}
