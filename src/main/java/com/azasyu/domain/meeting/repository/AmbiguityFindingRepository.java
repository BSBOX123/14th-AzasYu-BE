package com.azasyu.domain.meeting.repository;

import com.azasyu.domain.meeting.entity.AmbiguityFinding;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AmbiguityFindingRepository extends JpaRepository<AmbiguityFinding, Long> {

    List<AmbiguityFinding> findAllByAnalysisIdOrderByFindingOrderAsc(Long analysisId);

    /**
     * 기존 탐지 결과를 즉시 삭제함.
     *
     * <p>파생 삭제({@code deleteAllByAnalysisId})는 DELETE를 flush 시점까지 미루는데,
     * Hibernate는 flush 시 INSERT를 DELETE보다 먼저 실행한다. 그 결과 재생성 시
     * 같은 {@code (analysis_id, finding_order)}로 INSERT가 먼저 나가
     * {@code uk_ambiguity_findings_order} 제약을 위반함.
     * 벌크 삭제로 바꿔 DELETE가 먼저 실행되게 함.
     */
    @Modifying(flushAutomatically = true)
    @Query("delete from AmbiguityFinding f where f.analysis.id = :analysisId")
    void deleteAllByAnalysisId(@Param("analysisId") Long analysisId);
}
