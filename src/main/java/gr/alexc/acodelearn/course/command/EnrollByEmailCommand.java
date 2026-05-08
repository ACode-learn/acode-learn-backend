package gr.alexc.acodelearn.course.command;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EnrollByEmailCommand(@NotBlank @Email String email) {}
