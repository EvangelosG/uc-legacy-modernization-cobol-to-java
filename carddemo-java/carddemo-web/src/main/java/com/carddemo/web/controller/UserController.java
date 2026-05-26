package com.carddemo.web.controller;

import com.carddemo.domain.entity.UserSecurity;
import com.carddemo.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<Page<UserResponse>> listUsers(Pageable pageable) {
        Page<UserResponse> page = userService.listUsers(pageable).map(UserResponse::from);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable("id") String id) {
        UserSecurity user = userService.getUser(id);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserSecurity user = userService.createUser(
                request.userId(), request.firstName(), request.lastName(),
                request.password(), request.userType());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable("id") String id,
                                                   @Valid @RequestBody UpdateUserRequest request) {
        UserSecurity user = userService.updateUser(
                id, request.firstName(), request.lastName(),
                request.password(), request.userType());
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable("id") String id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }

    public record CreateUserRequest(
            @NotBlank @Size(min = 1, max = 8) String userId,
            String firstName,
            String lastName,
            @NotBlank @Size(min = 8) String password,
            @NotBlank @Pattern(regexp = "[AU]") String userType
    ) {}

    public record UpdateUserRequest(
            String firstName,
            String lastName,
            @Size(min = 8) String password,
            @Pattern(regexp = "[AU]") String userType
    ) {}

    public record UserResponse(String userId, String firstName, String lastName, String userType) {
        static UserResponse from(UserSecurity u) {
            return new UserResponse(u.getUsrId(), u.getUsrFname(), u.getUsrLname(), u.getUsrType());
        }
    }
}
