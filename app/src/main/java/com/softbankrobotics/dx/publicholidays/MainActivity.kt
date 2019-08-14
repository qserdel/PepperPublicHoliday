package com.softbankrobotics.dx.publicholidays

import android.os.Bundle
import android.util.Log
import com.aldebaran.qi.sdk.QiContext
import com.aldebaran.qi.sdk.QiSDK
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks
import com.aldebaran.qi.sdk.design.activity.RobotActivity
import com.aldebaran.qi.sdk.`object`.conversation.*
import com.aldebaran.qi.sdk.builder.ChatBuilder
import com.aldebaran.qi.sdk.builder.QiChatbotBuilder
import com.aldebaran.qi.sdk.builder.TopicBuilder
import kotlinx.android.synthetic.main.activity_main.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

const val TAG = "MainPublicHolidays"

class MainActivity : RobotActivity(), RobotLifecycleCallbacks {

    lateinit var topic: Topic
    lateinit var qiChatbot: QiChatbot
    lateinit var chat: Chat
    lateinit var askedCountry:String

    override fun onRobotFocusGained(qiContext: QiContext) {
        Log.i(TAG, "onRobotFocusGained called")

        topic = TopicBuilder.with(qiContext).withResource(R.raw.public_holidays_chatbot).build()
        qiChatbot = QiChatbotBuilder.with(qiContext).withTopic(topic).build()
        chat = ChatBuilder.with(qiContext).withChatbot(qiChatbot).build()
        askedCountry= qiChatbot.variable("country").value

        val executors: Map<String, QiChatExecutor> = hashMapOf(
            "findNextHoliday" to FindNextHolidayExecutor(qiContext),
            "todayHoliday" to TodayHolidayExecutor(qiContext),
            "rawDateToDate" to RawDateToDateExecutor(qiContext)
        )
        qiChatbot.executors = executors
        chat.addOnStartedListener { goToBookmark("BEGIN") }

        qiChatbot.variable("show").addOnValueChangedListener {
            runOnUiThread {
                textView.text = it
            }
        }
        // Start the dialogue
        chat.run()
    }

    override fun onRobotFocusRefused(reason: String?) {
    }

    override fun onRobotFocusLost() {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        QiSDK.register(this, this)
        Log.i(TAG, "onCreate called")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onDestroy() {
        QiSDK.unregister(this, this)
        Log.i(TAG, "onDestroy called")
        super.onDestroy()
    }

    /* qiChat executors */

    private inner class FindNextHolidayExecutor(qiContext: QiContext)
        :BaseQiChatExecutor(qiContext){
        override fun runWith(params: List<String>) {
            Log.i(TAG, "Running with: $params")
            askedCountry = params[0]
            val countryCode = translateCountryToCode(askedCountry)
            if (countryCode == "COUNTRY NOT FOUND") {
                qiChatbot.variable("date").value = "DATE NOT FOUND"
            } else {
                val nextPublicHoliday = NextPublicHoliday()
                val nextHoliday = nextPublicHoliday.service.getNextHolidayList(countryCode).execute()
                qiChatbot.variable("name").value = nextHoliday.body()?.get(0)?.name
                qiChatbot.variable("date").value = nextHoliday.body()?.get(0)?.date
            }
        }
        override fun stop() {}
    }

    private inner class TodayHolidayExecutor(qiContext: QiContext)
        :BaseQiChatExecutor(qiContext) {
        override fun runWith(params: List<String>) {
            Log.i(TAG, "Running with: $params")
            askedCountry = params[0]
            val countryCode = translateCountryToCode(askedCountry)
            if (countryCode == "COUNTRY NOT FOUND") {
                qiChatbot.variable("date").value = "DATE NOT FOUND"
            } else {
                val nextPublicHoliday = NextPublicHoliday()
                val nextHoliday = nextPublicHoliday.service.getNextHolidayList(countryCode).execute()
                val nextHolidayDate = nextHoliday.body()?.get(0)?.date.toString()
                Log.i(TAG, "nextHolidayDate: [$nextHolidayDate]")
                val nextHolidayName = nextHoliday.body()?.get(0)?.name
                val today = qiChatbot.variable("today").value.toString()
                Log.i(TAG, "today: [$today]")
                if(nextHolidayDate==today){
                    qiChatbot.variable("todayHoliday").value = "Today is the $nextHolidayName in $askedCountry"
                } else {
                    qiChatbot.variable("todayHoliday").value = "Today is not a holiday in $askedCountry"
                }
            }
        }
        override fun stop() {}
    }

    private inner class RawDateToDateExecutor(qiContext: QiContext)
        :BaseQiChatExecutor(qiContext) {
        override fun runWith(params: List<String>) {
            Log.i(TAG, "Running with: $params")
            qiChatbot.variable("year").value = qiChatbot.variable("date").value.toString().substring(0,4)
            qiChatbot.variable("month").value = qiChatbot.variable("date").value.toString().substring(5,7)
            qiChatbot.variable("day").value = qiChatbot.variable("date").value.toString().substring(8,10)
        }
        override fun stop() {}
    }


    /* only working on the 26 API version */
    /*
    fun changeDateFormat(rawDate:String):String {
        val date = LocalDate.parse(rawDate)
        val formatter = DateTimeFormatter.ofPattern("dd, MMMM, yyyy")
        return date.format(formatter)
    }*/


    private fun translateCountryToCode(country:String): String {
        for(countryAndCode in countryList){
            if(countryAndCode.country.toLowerCase() == country.toLowerCase()){
                return countryAndCode.code
            }
        }
        return "COUNTRY NOT FOUND"
    }

    private fun goToBookmark(bookmarkName: String) {
        qiChatbot.goToBookmark(
            topic.bookmarks[bookmarkName],
            AutonomousReactionImportance.HIGH,
            AutonomousReactionValidity.IMMEDIATE
        )
    }


    /* Country data base */

    data class CountryAndCode(
        val country: String,
        val code: String
    )
    private val countryList: List<CountryAndCode> = listOf(
    CountryAndCode("Andorra","AD"),
    CountryAndCode("Albania","AL"),
    CountryAndCode("Argentina","AR"),
    CountryAndCode("Austria","AT"),
    CountryAndCode("Australia","AU"),
    CountryAndCode("Åland Islands","AX"),
    CountryAndCode("Barbados","BB"),
    CountryAndCode("Belgium","BE"),
    CountryAndCode("Bulgaria","BG"),
    CountryAndCode("Benin","BJ"),
    CountryAndCode("Bolivia","BO"),
    CountryAndCode("Brazil","BR"),
    CountryAndCode("Bahamas","BS"),
    CountryAndCode("Botswana","BW"),
    CountryAndCode("Belarus","BY"),
    CountryAndCode("Belize","BZ"),
    CountryAndCode("Canada","CA"),
    CountryAndCode("Switzerland","CH"),
    CountryAndCode("Chile","CL"),
    CountryAndCode("China","CN"),
    CountryAndCode("Colombia","CO"),
    CountryAndCode("Costa Rica","CR"),
    CountryAndCode("Cuba","CU"),
    CountryAndCode("Cyprus","CY"),
    CountryAndCode("Czechia","CZ"),
    CountryAndCode("Germany","DE"),
    CountryAndCode("Denmark","DK"),
    CountryAndCode("Dominican Republic","DO"),
    CountryAndCode("Ecuador","EC"),
    CountryAndCode("Estonia","EE"),
    CountryAndCode("Egypt","EG"),
    CountryAndCode("Spain","ES"),
    CountryAndCode("Finland","FI"),
    CountryAndCode("Faroe Islands","FO"),
    CountryAndCode("France","FR"),
    CountryAndCode("Gabon","GA"),
    CountryAndCode("United Kingdom","GB"),
    CountryAndCode("Grenada","GD"),
    CountryAndCode("Greenland","GL"),
    CountryAndCode("Gambia","GM"),
    CountryAndCode("Greece","GR"),
    CountryAndCode("Guatemala","GT"),
    CountryAndCode("Guyana","GY"),
    CountryAndCode("Honduras","HN"),
    CountryAndCode("Croatia","HR"),
    CountryAndCode("Haiti","HT"),
    CountryAndCode("Hungary","HU"),
    CountryAndCode("Indonesia","ID"),
    CountryAndCode("Ireland","IE"),
    CountryAndCode("Isle of Man","IM"),
    CountryAndCode("Iceland","IS"),
    CountryAndCode("Italy","IT"),
    CountryAndCode("Jersey","JE"),
    CountryAndCode("Jamaica","JM"),
    CountryAndCode("Japan","JP"),
    CountryAndCode("Liechtenstein","LI"),
    CountryAndCode("Lesotho","LS"),
    CountryAndCode("Lithuania","LT"),
    CountryAndCode("Luxembourg","LU"),
    CountryAndCode("Latvia","LV"),
    CountryAndCode("Morocco","MA"),
    CountryAndCode("Monaco","MC"),
    CountryAndCode("Moldova","MD"),
    CountryAndCode("Madagascar","MG"),
    CountryAndCode("Macedonia","MK"),
    CountryAndCode("Mongolia","MN"),
    CountryAndCode("Malta","MT"),
    CountryAndCode("Mexico","MX"),
    CountryAndCode("Mozambique","MZ"),
    CountryAndCode("Namibia","NA"),
    CountryAndCode("Niger","NE"),
    CountryAndCode("Nicaragua","NI"),
    CountryAndCode("Netherlands","NL"),
    CountryAndCode("Norway","NO"),
    CountryAndCode("New Zealand","NZ"),
    CountryAndCode("Panama","PA"),
    CountryAndCode("Peru","PE"),
    CountryAndCode("Poland","PL"),
    CountryAndCode("Puerto Rico","PR"),
    CountryAndCode("Portugal","PT"),
    CountryAndCode("Paraguay","PY"),
    CountryAndCode("Romania","RO"),
    CountryAndCode("Serbia","RS"),
    CountryAndCode("Russia","RU"),
    CountryAndCode("Sweden","SE"),
    CountryAndCode("Slovenia","SI"),
    CountryAndCode("Svalbard and Jan Mayen","SJ"),
    CountryAndCode("Slovakia","SK"),
    CountryAndCode("San Marino","SM"),
    CountryAndCode("Suriname","SR"),
    CountryAndCode("El Salvador","SV"),
    CountryAndCode("Tunisia","TN"),
    CountryAndCode("Turkey","TR"),
    CountryAndCode("Ukraine","UA"),
    CountryAndCode("United States","US"),
    CountryAndCode("Uruguay","UY"),
    CountryAndCode("Vatican City","VA"),
    CountryAndCode("Venezuela","VE"),
    CountryAndCode("Vietnam","VN"),
    CountryAndCode("South Africa","ZA"))

}
