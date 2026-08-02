package com.RTGpoll.repository;

import com.RTGpoll.model.Form;
import com.RTGpoll.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Backs FormController: save() on POST /forms (create), findById() on GET /forms/{id}
// (public fetch for anonymous form-filling), and findByCreatorOrderByIdDesc() on
// GET /forms (authenticated list of the caller's own surveys).
public interface FormRepository extends JpaRepository<Form, Long> {

    List<Form> findByCreatorOrderByIdDesc(User creator);
}
