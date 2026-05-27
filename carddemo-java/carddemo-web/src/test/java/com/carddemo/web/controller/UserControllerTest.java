package com.carddemo.web.controller;

import com.carddemo.domain.entity.UserSecurity;
import com.carddemo.service.JwtTokenProvider;
import com.carddemo.service.UserService;
import com.carddemo.web.config.GlobalExceptionHandler;
import com.carddemo.web.config.JwtAuthenticationFilter;
import com.carddemo.web.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = {UserController.class, SecurityConfig.class,
        JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private void mockAdminAuth(String token) {
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUserId(token)).thenReturn("ADMIN01");
        when(jwtTokenProvider.getUserType(token)).thenReturn("A");
    }

    private void mockRegularAuth(String token) {
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUserId(token)).thenReturn("USER01");
        when(jwtTokenProvider.getUserType(token)).thenReturn("U");
    }

    @Test
    void listUsersRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listUsersReturnsPage() throws Exception {
        String token = "t";
        mockAdminAuth(token);

        UserSecurity user = UserSecurity.builder()
                .usrId("USER01").usrFname("First").usrLname("Last").usrPwd("hash").usrType("U").build();
        when(userService.listUsers(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").value("USER01"))
                .andExpect(jsonPath("$.content[0].firstName").value("First"));
    }

    @Test
    void getUserById() throws Exception {
        String token = "t";
        mockAdminAuth(token);

        UserSecurity user = UserSecurity.builder()
                .usrId("USER01").usrFname("John").usrLname("Doe").usrPwd("hash").usrType("U").build();
        when(userService.getUser("USER01")).thenReturn(user);

        mockMvc.perform(get("/api/users/USER01")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("USER01"))
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    void createUserReturns201() throws Exception {
        String token = "t";
        mockAdminAuth(token);

        UserSecurity created = UserSecurity.builder()
                .usrId("NEW01").usrFname("New").usrLname("User").usrPwd("hash").usrType("U").build();
        when(userService.createUser("NEW01", "New", "User", "password1", "U")).thenReturn(created);

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UserController.CreateUserRequest("NEW01", "New", "User", "password1", "U"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value("NEW01"));
    }

    @Test
    void createUserValidatesShortUserId() throws Exception {
        String token = "t";
        mockAdminAuth(token);

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"\",\"password\":\"password1\",\"userType\":\"U\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUserValidatesPasswordTooShort() throws Exception {
        String token = "t";
        mockAdminAuth(token);

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"NEW01\",\"password\":\"short\",\"userType\":\"U\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUserValidatesInvalidUserType() throws Exception {
        String token = "t";
        mockAdminAuth(token);

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"NEW01\",\"password\":\"password1\",\"userType\":\"X\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDuplicateUserReturns409() throws Exception {
        String token = "t";
        mockAdminAuth(token);

        when(userService.createUser("DUPE01", "A", "B", "password1", "U"))
                .thenThrow(new UserService.DuplicateUserException("DUPE01"));

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UserController.CreateUserRequest("DUPE01", "A", "B", "password1", "U"))))
                .andExpect(status().isConflict());
    }

    @Test
    void updateUserReturnsUpdatedUser() throws Exception {
        String token = "t";
        mockAdminAuth(token);

        UserSecurity updated = UserSecurity.builder()
                .usrId("USER01").usrFname("Updated").usrLname("Name").usrPwd("hash").usrType("A").build();
        when(userService.updateUser("USER01", "Updated", "Name", "newpass12", "A")).thenReturn(updated);

        mockMvc.perform(put("/api/users/USER01")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UserController.UpdateUserRequest("Updated", "Name", "newpass12", "A"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"));
    }

    @Test
    void deleteUserByAdminReturnsOk() throws Exception {
        String token = "t";
        mockAdminAuth(token);

        doNothing().when(userService).deleteUser("USER01");

        mockMvc.perform(delete("/api/users/USER01")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User deleted successfully"));
    }

    @Test
    void deleteUserByRegularUserReturnsForbidden() throws Exception {
        String token = "t";
        mockRegularAuth(token);

        mockMvc.perform(delete("/api/users/USER01")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserNotFoundReturns404() throws Exception {
        String token = "t";
        mockAdminAuth(token);

        when(userService.getUser("NOUSER")).thenThrow(new UserService.UserNotFoundException("NOUSER"));

        mockMvc.perform(get("/api/users/NOUSER")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
