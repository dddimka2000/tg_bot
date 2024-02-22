package org.example.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "client")
@Data
public class ClientEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nameAndSurname;
    private String phone;
    private String email;
    private String nameCompany;
    private LocalDate localDate;

    @Override
    public String toString() {
        return "Имя и Фамилия = " + nameAndSurname +
                ", Номер телефона =" + phone +
                ", email = " + email +
                ", Имя компании = " + nameCompany +
                ", Дата =" + localDate;
    }
}
