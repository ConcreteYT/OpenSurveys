package com.opensurveys.service;

import com.opensurveys.model.Form;
import com.opensurveys.model.User;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormAccessServiceTest {

    private final FormAccessService service = new FormAccessService();

    private static User user(long id, String role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return user;
    }

    private static Form form(User creator, boolean responsesPublic) {
        Form form = new Form();
        form.setCreator(creator);
        form.setResponsesPublic(responsesPublic);
        return form;
    }

    @Test
    void ownerCanManageAndExport() {
        User owner = user(1, User.ROLE_USER);
        Form form = form(owner, false);
        assertTrue(service.canManageForm(form, owner));
        assertTrue(service.canExportResponses(form, owner));
        assertFalse(service.canManageForm(form, user(2, User.ROLE_USER)));
        assertFalse(service.canExportResponses(form, null));
        assertFalse(service.canManageForm(form(null, true), owner));
    }

    @Test
    void publicResponsesVisibleToAnyoneButOtherAdmins() {
        User owner = user(1, User.ROLE_USER);
        Form form = form(owner, true);
        assertTrue(service.canViewResponses(form, Optional.empty()));
        assertTrue(service.canViewResponses(form, Optional.of(user(2, User.ROLE_USER))));
        assertFalse(service.canViewResponses(form, Optional.of(user(3, User.ROLE_ADMIN))));
        assertTrue(service.adminBlockedFromResponses(form, Optional.of(user(3, User.ROLE_ADMIN))));
    }

    @Test
    void privateResponsesOnlyForOwner() {
        User owner = user(1, User.ROLE_USER);
        Form form = form(owner, false);
        assertTrue(service.canViewResponses(form, Optional.of(owner)));
        assertFalse(service.canViewResponses(form, Optional.empty()));
        assertFalse(service.canViewResponses(form, Optional.of(user(2, User.ROLE_USER))));
        assertFalse(service.adminBlockedFromResponses(form, Optional.empty()));
    }

    @Test
    void adminOwnerIsNotBlocked() {
        User adminOwner = user(1, User.ROLE_ADMIN);
        Form form = form(adminOwner, false);
        assertFalse(service.adminBlockedFromResponses(form, Optional.of(adminOwner)));
        assertTrue(service.canViewResponses(form, Optional.of(adminOwner)));
    }
}
