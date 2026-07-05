package com.rutina.rutinabackend.domain.todo.repository;

import com.rutina.rutinabackend.domain.todo.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    Optional<Todo> findByIdAndUser_Id(Long todoId, Long userId);

    List<Todo> findAllByUser_IdAndTodoDateOrderByTodoTimeAscIdAsc(Long userId, LocalDate todoDate);

    List<Todo> findAllByUser_IdAndTodoDateBetweenOrderByTodoDateAscTodoTimeAscIdAsc(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );
}