package com.opensurveys.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.opensurveys.model.Form;
import com.opensurveys.model.User;

import java.util.List;

// Backs FormController: save() on POST /forms (create), findById() on GET /forms/{id}
// (public fetch for anonymous form-filling), and findByCreatorOrderByIdDesc() on
// GET /forms (authenticated list of the caller's own surveys).
public interface FormRepository extends JpaRepository<Form, Long> {

    List<Form> findByCreatorOrderByIdDesc(User creator);
}
