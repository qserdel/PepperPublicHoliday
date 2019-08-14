package com.softbankrobotics.dx.publicholidays

import org.junit.Assert
import org.junit.Test

class HolidayTest{
    @Test

    fun holidayMatchTest() {
        val nextPublicHoliday = NextPublicHoliday()
        val nextHolidayFR = nextPublicHoliday.service.getNextHolidayList("FR").execute()
        val nextHolidayFRName = nextHolidayFR.body()?.get(0)?.name?.toLowerCase()

        Assert.assertEquals("bastille day", nextHolidayFRName)

    }

}

