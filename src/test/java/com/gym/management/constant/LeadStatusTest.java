package com.gym.management.constant;

import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static com.gym.management.constant.LeadStatus.*;
import static org.junit.jupiter.api.Assertions.*;

class LeadStatusTest {

    @Test
    void validTransitions() {
        assertTrue(NEW.canTransitionTo(CONTACTED));
        assertTrue(NEW.canTransitionTo(LOST));
        assertTrue(NEW.canTransitionTo(CONVERTED));
        assertTrue(CONTACTED.canTransitionTo(LOST));
        assertTrue(CONTACTED.canTransitionTo(CONVERTED));
        assertTrue(LOST.canTransitionTo(CONTACTED));
    }

    @Test
    void invalidTransitions() {
        assertFalse(CONTACTED.canTransitionTo(NEW));
        assertFalse(NEW.canTransitionTo(NEW));
        assertFalse(LOST.canTransitionTo(CONVERTED));
        for (LeadStatus target : LeadStatus.values()) {
            assertFalse(CONVERTED.canTransitionTo(target), "CONVERTED không được chuyển ngược sang " + target);
        }
    }

    @Test
    void parseIsCaseInsensitiveAndRejectsGarbage() {
        assertEquals(CONTACTED, LeadStatus.parse(" contacted "));
        BusinessException ex = assertThrows(BusinessException.class, () -> LeadStatus.parse("ABC"));
        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
        assertThrows(BusinessException.class, () -> LeadStatus.parse(" "));
    }
}
