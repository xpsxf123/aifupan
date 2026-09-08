package com.jiuyu.governance.business.room.pojo.bo;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 批量排班查询 DTO
 *
 * @author HeHui
 */
@Getter
@Setter
@EqualsAndHashCode
public class BatchQueryScheduleDto {
    private Long roomId;
    private LocalDate startDay;
    private LocalDate endDay;

    public BatchQueryScheduleDto() {
    }

    public BatchQueryScheduleDto(Long roomId, LocalDate startDay, LocalDate endDay) {
        this.roomId = roomId;
        this.startDay = startDay;
        this.endDay = endDay;
    }
}
