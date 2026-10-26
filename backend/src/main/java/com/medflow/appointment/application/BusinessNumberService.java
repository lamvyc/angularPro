package com.medflow.appointment.application;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.medflow.appointment.domain.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BusinessNumberService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;

    private final AppointmentRepository repository;
    private final Clock clock;

    /**
     * 必须在业务事务内调用：序列自增与业务写入同事务，失败一起回滚。
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public String next(String prefix) {
        String day = LocalDate.now(clock).format(DAY);
        long value = repository.nextSequence(prefix + "-" + day);
        return prefix + day + String.format("%06d", value);
    }
}
