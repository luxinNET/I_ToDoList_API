package com.example.itodo.reminder;

import com.example.itodo.reminder.dto.ReminderResponse;
import com.example.itodo.reminder.entity.Reminder;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReminderDtoMapper {

    ReminderResponse toReminderResponse(Reminder reminder);

    List<ReminderResponse> toReminderResponses(List<Reminder> reminders);
}
