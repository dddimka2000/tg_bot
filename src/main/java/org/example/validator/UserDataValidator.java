package org.example.validator;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class UserDataValidator {
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+380\\d{9}$");
    private static final Pattern NAME_SURNAME_PATTERN = Pattern.compile("^[a-zA-Zа-яА-Я]+\\s+[a-zA-Zа-яА-Я]+$");

    public  boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        Pattern pattern = Pattern.compile(emailRegex);
        return pattern.matcher(email).matches();
    }


    public  boolean isValidPhoneNumber(String phoneNumber) {
        return PHONE_PATTERN.matcher(phoneNumber).matches();
    }

    public  boolean isValidNameAndSurname(String nameAndSurname) {
        return NAME_SURNAME_PATTERN.matcher(nameAndSurname).matches();
    }
}
