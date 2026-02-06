package com.elice.cinema.domain.member.entity;

import com.elice.cinema.global.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "members",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_member_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_member_nickname", columnNames = "nickname")
        }
)
public class Member extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;

    @Column(name = "age", nullable = false)
    private int age;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;
}