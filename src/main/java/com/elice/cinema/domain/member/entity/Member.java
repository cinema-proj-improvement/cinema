package com.elice.cinema.domain.member.entity;

import com.elice.cinema.global.entity.BaseEntity;
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
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(nullable = false)
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;
}