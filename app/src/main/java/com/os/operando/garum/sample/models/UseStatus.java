package com.os.operando.garum.sample.models;

import com.os.operando.garum.annotations.Pref;
import com.os.operando.garum.annotations.PrefKey;
import com.os.operando.garum.models.PrefModel;

import java.util.Calendar;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@Pref(name = "date_status")
public class UseStatus extends PrefModel {

    @PrefKey("last_used")
    private Date lastUsed;

    @PrefKey
    private Calendar calendar;
}
