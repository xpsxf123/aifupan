package com.jiuyu.replay.words.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class SyncContrastInfoVoUpper extends SyncContrastVo implements Serializable {

    private static final long serialVersionUID = 1L;
}
