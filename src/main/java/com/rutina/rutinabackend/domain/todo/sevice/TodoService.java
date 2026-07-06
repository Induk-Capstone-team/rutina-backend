package com.rutina.rutinabackend.domain.todo.service;

import com.rutina.rutinabackend.domain.todo.dto.*;
import com.rutina.rutinabackend.domain.todo.entity.Todo;
import com.rutina.rutinabackend.domain.todo.repository.TodoRepository;
import com.rutina.rutinabackend.domain.user.entity.User;
import com.rutina.rutinabackend.domain.user.repository.UserRepository;
import com.rutina.rutinabackend.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoService {

    private final TodoRepository todoRepository;
    private final UserRepository userRepository;

    @Transactional
    public TodoResponse createTodo(Long userId, TodoCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "USER_404", "사용자를 찾을 수 없습니다."));

        Todo todo = Todo.create(
                user,
                request.getTodoDate(),
                request.getTodoTime(),
                request.getContent().trim()
        );

        return TodoResponse.from(todoRepository.save(todo));
    }

    public List<TodoResponse> getTodosByDate(Long userId, LocalDate date) {
        return sortTodos(todoRepository.findAllByUser_IdAndTodoDateOrderByTodoTimeAscIdAsc(userId, date));
    }

    public List<TodoResponse> getTodayTodos(Long userId) {
        return getTodosByDate(userId, LocalDate.now());
    }

    public List<TodoResponse> getTodosByMonth(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);

        List<Todo> todos = todoRepository.findAllByUser_IdAndTodoDateBetweenOrderByTodoDateAscTodoTimeAscIdAsc(
                userId,
                yearMonth.atDay(1),
                yearMonth.atEndOfMonth()
        );

        return sortTodos(todos);
    }

    @Transactional
    public TodoResponse updateTodo(Long userId, Long todoId, TodoUpdateRequest request) {
        Todo todo = findTodo(userId, todoId);

        todo.update(
                request.getTodoDate(),
                request.getTodoTime(),
                request.getContent().trim()
        );

        return TodoResponse.from(todo);
    }

    @Transactional
    public TodoResponse updateCompleted(Long userId, Long todoId, TodoCompletedRequest request) {
        Todo todo = findTodo(userId, todoId);
        todo.updateCompleted(request.getCompleted());
        return TodoResponse.from(todo);
    }

    @Transactional
    public void deleteTodo(Long userId, Long todoId) {
        Todo todo = findTodo(userId, todoId);
        todoRepository.delete(todo);
    }

    private Todo findTodo(Long userId, Long todoId) {
        return todoRepository.findByIdAndUser_Id(todoId, userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "TODO_404", "Todo를 찾을 수 없습니다."));
    }

    private List<TodoResponse> sortTodos(List<Todo> todos) {
        return todos.stream()
                .sorted(
                        Comparator
                                .comparing((Todo todo) -> todo.getTodoTime() == null)
                                .thenComparing(Todo::getTodoDate)
                                .thenComparing(
                                        Todo::getTodoTime,
                                        Comparator.nullsLast(Comparator.naturalOrder())
                                )
                                .thenComparing(Todo::getId)
                )
                .map(TodoResponse::from)
                .toList();
    }
}