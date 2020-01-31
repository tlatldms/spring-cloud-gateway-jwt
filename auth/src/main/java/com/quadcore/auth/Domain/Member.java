package com.quadcore.auth.Domain;

import lombok.Data;
import lombok.experimental.Delegate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.validator.constraints.Length;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.Date;

@Data
@Entity
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable=false, unique=true, length=20)
    private String username;

    @Column(nullable=false, unique=true, length=50)
    private String email;

    @Length(min=8, max=200)
    private String password;

    @CreationTimestamp
    private Date reg_dt;

    @UpdateTimestamp
    private Date updated_dt;

    private Date access_dt;

    private int grade;
}
