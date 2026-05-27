package com.carddemo.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MenuController {

    private static final List<Map<String, Object>> MAIN_MENU_OPTIONS = List.of(
            Map.of("optionNumber", 1, "name", "Account View", "program", "COACTVWC"),
            Map.of("optionNumber", 2, "name", "Account Update", "program", "COACTUPC"),
            Map.of("optionNumber", 3, "name", "Credit Card List", "program", "COCRDLIC"),
            Map.of("optionNumber", 4, "name", "Credit Card View", "program", "COCRDSLC"),
            Map.of("optionNumber", 5, "name", "Credit Card Update", "program", "COCRDUPC"),
            Map.of("optionNumber", 6, "name", "Transaction List", "program", "COTRN00C"),
            Map.of("optionNumber", 7, "name", "Transaction View", "program", "COTRN01C"),
            Map.of("optionNumber", 8, "name", "Transaction Add", "program", "COTRN02C"),
            Map.of("optionNumber", 9, "name", "Transaction Reports", "program", "CORPT00C"),
            Map.of("optionNumber", 10, "name", "Bill Payment", "program", "COBIL00C"),
            Map.of("optionNumber", 11, "name", "Pending Authorization View", "program", "COPAUS0C")
    );

    private static final List<Map<String, Object>> ADMIN_MENU_OPTIONS = List.of(
            Map.of("optionNumber", 1, "name", "User List (Security)", "program", "COUSR00C"),
            Map.of("optionNumber", 2, "name", "User Add (Security)", "program", "COUSR01C"),
            Map.of("optionNumber", 3, "name", "User Update (Security)", "program", "COUSR02C"),
            Map.of("optionNumber", 4, "name", "User Delete (Security)", "program", "COUSR03C"),
            Map.of("optionNumber", 5, "name", "Transaction Type List/Update (Db2)", "program", "COTRTLIC"),
            Map.of("optionNumber", 6, "name", "Transaction Type Maintenance (Db2)", "program", "COTRTUPC")
    );

    @GetMapping("/menu")
    public ResponseEntity<List<Map<String, Object>>> getMainMenu() {
        return ResponseEntity.ok(MAIN_MENU_OPTIONS);
    }

    @GetMapping("/admin/menu")
    public ResponseEntity<List<Map<String, Object>>> getAdminMenu() {
        return ResponseEntity.ok(ADMIN_MENU_OPTIONS);
    }
}
