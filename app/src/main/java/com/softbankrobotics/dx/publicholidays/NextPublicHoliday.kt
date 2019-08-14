package com.softbankrobotics.dx.publicholidays

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Path

class NextPublicHoliday{

    private val uRLBase = "https://date.nager.at/Api/v2/"

    interface NextPublicHolidayInfo {

        @GET("NextPublicHolidays/{country}")
        fun getNextHolidayList(@Path("country") country: String): Call<List<Holiday>>
    }

    private val retrofit = Retrofit.Builder()
        .baseUrl(uRLBase)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service = retrofit.create(NextPublicHolidayInfo::class.java)!!

    data class Holiday(
        val date: String,
        val localName: String,
        val name: String,
        val countryCode: String,
        val fixed: Boolean,
        val global: Boolean,
        val counties: List<String>,
        val launchYear: Int,
        val type: String
    )


}