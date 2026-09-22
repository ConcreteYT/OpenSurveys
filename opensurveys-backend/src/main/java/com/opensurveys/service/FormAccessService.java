package com.opensurveys.service;

import com.opensurveys.model.Form;
import com.opensurveys.model.User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class FormAccessService {

    public boolean isFormOwner(Form form, User user) {
        return form != null
                && user != null
                && form.getCreator() != null
                && form.getCreator().getId().equals(user.getId());
    }

    public boolean isAdminUser(User user) {
        return user != null && User.ROLE_ADMIN.equals(user.getRole());
    }

    /**
     * Admins may not view another user's responses/media/export, even when responses are public.
     */
    public boolean adminBlockedFromResponses(Form form, Optional<User> userOptional) {
        if (userOptional.isEmpty()) {
            return false;
        }
        User user = userOptional.get();
        return isAdminUser(user) && !isFormOwner(form, user);
    }

    public boolean canViewResponses(Form form, Optional<User> userOptional) {
        if (adminBlockedFromResponses(form, userOptional)) {
            return false;
        }
        if (form.isResponsesPublic()) {
            return true;
        }
        return userOptional.isPresent() && isFormOwner(form, userOptional.get());
    }

    public boolean canExportResponses(Form form, User user) {
        return isFormOwner(form, user);
    }

    public boolean canManageForm(Form form, User user) {
        return isFormOwner(form, user);
    }
}
