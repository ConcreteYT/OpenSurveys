package com.opensurveys.service;

import com.opensurveys.model.Form;
import com.opensurveys.model.User;
import com.opensurveys.repository.FormRepository;
import com.opensurveys.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.util.List;

/**
 * Removes an account together with every survey it owns. Forms reference USER via FK, so the
 * owned surveys (and their cascaded questions/answers) go first. Uploaded files are removed only
 * once the database changes have committed.
 */
@Service
public class UserDeletionService {

    private static final Logger log = LoggerFactory.getLogger(UserDeletionService.class);

    @Autowired
    private FormRepository formRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UploadStorageService uploadStorageService;

    @Transactional
    public void deleteUserAndForms(User user) {
        List<Form> forms = formRepository.findByCreatorOrderByIdDesc(user);
        List<Long> formIds = forms.stream().map(Form::getId).toList();
        formRepository.deleteAll(forms);
        userRepository.delete(user);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (Long formId : formIds) {
                    try {
                        uploadStorageService.deleteFormUploads(formId);
                    } catch (IOException e) {
                        log.warn("Failed to delete uploads for form {}: {}", formId, e.getMessage());
                    }
                }
            }
        });
    }
}
