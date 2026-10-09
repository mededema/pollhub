package com.pollhub.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// La contrainte unique est la vraie protection contre le double vote : le controle
// applicatif (existsBy...) n'est qu'un raccourci pour un message d'erreur agreable,
// il ne suffit pas seul en cas de requetes concurrentes.
@Entity
@Table(
    name = "votes",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_votes_question_voter",
        columnNames = {"question_id", "voter_username"}
    )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "question_id")
    private Question question;

    @ManyToOne
    @JoinColumn(name = "option_id")
    private Option option;

    @Column(name = "voter_username")
    private String voterUsername;

    @Column(name = "voted_at")
    private LocalDateTime votedAt;

    @PrePersist
    protected void onCreate() {
        votedAt = LocalDateTime.now();
    }
}