package com.pollhub.repository;

import com.pollhub.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {
    long countByOptionId(Long optionId);
    boolean existsByQuestionIdAndVoterUsername(Long questionId, String voterUsername);
    void deleteByQuestionPollId(Long pollId);
}