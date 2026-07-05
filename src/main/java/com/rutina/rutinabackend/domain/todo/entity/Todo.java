package com.rutina.rutinabackend.domain.todo.entity;

import com.rutina.rutinabackend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "todos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Todo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "todo_date", nullable = false)
    private LocalDate todoDate;

    @Column(name = "todo_time")
    private LocalTime todoTime;

    @Column(nullable = false, length = 30)
    private String content;

    @Column(nullable = false)
    private Boolean completed;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private OffsetDateTime updatedAt;

    public static Todo create(User user, LocalDate todoDate, LocalTime todoTime, String content) {
        Todo todo = new Todo();
        todo.user = user;
        todo.todoDate = todoDate;
        todo.todoTime = todoTime;
        todo.content = content;
        todo.completed = false;
        todo.createdAt = OffsetDateTime.now();
        todo.updatedAt = OffsetDateTime.now();
        return todo;
    }

    public void update(LocalDate todoDate, LocalTime todoTime, String content) {
        this.todoDate = todoDate;
        this.todoTime = todoTime;
        this.content = content;
        this.updatedAt = OffsetDateTime.now();
    }

    public void updateCompleted(Boolean completed) {
        this.completed = completed;
        this.updatedAt = OffsetDateTime.now();
    }
}