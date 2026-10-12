package com.opensurveys.dto;

import com.opensurveys.model.Form;
import com.opensurveys.model.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormResponseTest {

    @Test
    void fromCopiesAccessCode() {
        User creator = new User();
        creator.setUsername("ada");
        Form form = new Form();
        form.setId(4L);
        form.setName("Poll");
        form.setAccessCode("00000004");
        form.setCreator(creator);

        FormResponse response = FormResponse.from(form);

        assertEquals(4L, response.getId());
        assertEquals("00000004", response.getAccessCode());
        assertEquals("ada", response.getCreatorUsername());
    }
}
