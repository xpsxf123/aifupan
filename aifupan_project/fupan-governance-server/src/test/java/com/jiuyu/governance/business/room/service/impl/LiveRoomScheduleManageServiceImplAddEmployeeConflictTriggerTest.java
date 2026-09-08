package com.jiuyu.governance.business.room.service.impl;

import com.jiuyu.governance.business.room.pojo.entity.WorkSchedule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

class LiveRoomScheduleManageServiceImplAddEmployeeConflictTriggerTest {

    @Test
    void shouldValidateConflict_WhenScheduleStartAfterNow() throws Exception {
        Method method = LiveRoomScheduleManageServiceImpl.class.getDeclaredMethod(
            "needValidateConflict", WorkSchedule.class, LocalDateTime.class
        );
        method.setAccessible(true);

        LocalDateTime now = LocalDateTime.of(LocalDate.of(2026, 4, 22), LocalTime.of(10, 0));

        WorkSchedule todayFutureStart = new WorkSchedule();
        todayFutureStart.setWorkDay(now.toLocalDate());
        todayFutureStart.setStartWork(1100);
        todayFutureStart.setEndWork(1200);

        boolean todayResult = (boolean) method.invoke(null, todayFutureStart, now);
        Assertions.assertTrue(todayResult);

        WorkSchedule tomorrowMorning = new WorkSchedule();
        tomorrowMorning.setWorkDay(now.toLocalDate().plusDays(1));
        tomorrowMorning.setStartWork(0);
        tomorrowMorning.setEndWork(30);

        boolean tomorrowResult = (boolean) method.invoke(null, tomorrowMorning, now);
        Assertions.assertTrue(tomorrowResult);
    }

    @Test
    void shouldNotValidateConflict_WhenScheduleStartNotAfterNow() throws Exception {
        Method method = LiveRoomScheduleManageServiceImpl.class.getDeclaredMethod(
            "needValidateConflict", WorkSchedule.class, LocalDateTime.class
        );
        method.setAccessible(true);

        LocalDateTime now = LocalDateTime.of(LocalDate.of(2026, 4, 22), LocalTime.of(10, 0));

        WorkSchedule todayOngoing = new WorkSchedule();
        todayOngoing.setWorkDay(now.toLocalDate());
        todayOngoing.setStartWork(900);
        todayOngoing.setEndWork(1100);

        boolean todayOngoingResult = (boolean) method.invoke(null, todayOngoing, now);
        Assertions.assertFalse(todayOngoingResult);

        WorkSchedule todayStartEqualsNow = new WorkSchedule();
        todayStartEqualsNow.setWorkDay(now.toLocalDate());
        todayStartEqualsNow.setStartWork(1000);
        todayStartEqualsNow.setEndWork(1200);

        boolean todayStartEqualsNowResult = (boolean) method.invoke(null, todayStartEqualsNow, now);
        Assertions.assertFalse(todayStartEqualsNowResult);
    }
}
