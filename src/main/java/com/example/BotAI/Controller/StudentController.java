package com.example.BotAI.Controller;

import com.example.BotAI.DTOs.StudentDto;
import com.example.BotAI.Entity.StudentEntity;
import com.example.BotAI.Services.StudentServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class StudentController {

        @Autowired
        StudentServices studentService;

        @GetMapping("/students")
        public Object getAllStudents() {
            return studentService.getAllStudents();
        }

        @PostMapping("/students")
        public Object createStudent(@RequestBody StudentDto student) {
            return studentService.createStudent(student);
        }

        @PutMapping("/students/{id}")
        public Object updateStudent(@PathVariable long id, @RequestBody StudentDto student) {
            return studentService.updateStudent(id, student);
        }

        @DeleteMapping("/students/{id}")
        public Object deleteStudent(@PathVariable long id) {
            return studentService.deleteStudent(id);
        }

        @GetMapping("/askai")
       public Object askAI(@RequestParam String question,
                          @RequestParam(required = false) Boolean confirm,
                          @RequestParam(required = false) String actionId) {
           return studentService.askAI(question, actionId, Boolean.TRUE.equals(confirm));
       }

       @PostMapping("/askai/confirm")
       public Object confirmAction(@RequestParam String actionId,
                                  @RequestParam Boolean confirm) {
           return studentService.confirmAction(actionId, Boolean.TRUE.equals(confirm));
       }

}
