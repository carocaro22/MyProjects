package com.example.ledcontrol

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.databinding.DataBindingUtil
import com.example.ledcontrol.databinding.ActivityMainBinding
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

//Put your Write API Key here
val apikey = "YI1OLHC1NE6RFW41"

//Create the binding object
private lateinit var binding: ActivityMainBinding

//create the moshi object to parse JSON
private val moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

//create the retrofit object
private val retrofit = Retrofit.Builder()
    .baseUrl("https://api.thingspeak.com/")
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .build()

//create the interface to send the Data
interface MyApi {
    @GET("update")

    fun updateLed(
        @Query("api_key") apikey: String,
        @Query("field1") ledStatus: String)

        : Call<ResponseBody>
}


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.led1.setOnClickListener{evaluateSwitch()}
    }
}

fun evaluateSwitch () {
        if (binding.led1.isChecked){
            updateLedStatus("0")
            binding.led1.isEnabled = false;
            Handler().postDelayed(
                {
                    binding.led1.isEnabled = true;
                },
                15000 // value in milliseconds
            )
        }
        else{
            updateLedStatus("1")
            binding.led1.isEnabled = false;
            Handler().postDelayed(
                {
                    binding.led1.isEnabled = true;
                },
                15000 // value in milliseconds
            )
        }
}

fun updateLedStatus(ledStatus: String) {
    val service = retrofit.create(MyApi::class.java)

    service.updateLed(apikey, ledStatus).enqueue(
        object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                Log.d("Connection: ", "Success")
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.d("Connection: ", "Failure " + t.message)
            }

        }

    )
}