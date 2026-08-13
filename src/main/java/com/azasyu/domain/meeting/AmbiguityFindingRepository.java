package com.azasyu.domain.meeting;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AmbiguityFindingRepository extends JpaRepository<AmbiguityFinding, Long> {

    List<AmbiguityFinding> findAllByAnalysisIdOrderByFindingOrderAsc(Long analysisId);

    void deleteAllByAnalysisId(Long analysisId);
}
