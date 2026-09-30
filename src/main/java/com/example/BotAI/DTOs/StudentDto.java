package com.example.BotAI.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentDto {

    String name;
    String school;
    String email;
    String phone;
    String description;

}
