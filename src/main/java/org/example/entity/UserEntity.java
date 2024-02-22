package org.example.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;

@Entity
@Table(name = "user")
@Data
public class UserEntity{
    @Id
    private Long id;

    private String nameAndSurname;
    private String phone;
    private String email;
    private String nameCompany;

    @Override
    public String toString() {
        return "" +
                "ID = " + id +
                ", Имя и фамилия = " + nameAndSurname +
                ", Номер телефона = " + phone +
                ", E-mail = " + email +
                ", Имя компании = " + nameCompany;
    }
}
