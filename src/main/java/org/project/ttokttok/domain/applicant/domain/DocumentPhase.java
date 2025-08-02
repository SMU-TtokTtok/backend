package org.project.ttokttok.domain.applicant.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.project.ttokttok.domain.applicant.domain.enums.PhaseStatus;
import org.project.ttokttok.domain.applicant.domain.json.Answer;
import org.project.ttokttok.domain.applicant.domain.json.AnswerListConverter;
import org.project.ttokttok.domain.memo.domain.Memo;
import org.project.ttokttok.global.entity.BaseTimeEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "document_phases")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentPhase extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, updatable = false, unique = true)
    private String id;

    @OneToOne
    @JoinColumn(name = "applicant_id")
    private Applicant applicant;

    @Convert(converter = AnswerListConverter.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<Answer> answers;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PhaseStatus status;  // EVALUATING, PASS, FAIL

    @OneToMany(mappedBy = "documentPhase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Memo> memos = new ArrayList<>();

    public static DocumentPhase create(Applicant applicant, List<Answer> answers) {
        DocumentPhase phase = new DocumentPhase();
        phase.applicant = applicant;
        phase.answers = answers;
        phase.status = PhaseStatus.EVALUATING;
        return phase;
    }

    public void updateStatus(PhaseStatus status) {
        this.status = status;
    }

    // ----- 메모 관련 비즈니스 메서드 ----- //
    public String addMemo(String content) {
        Memo memo = Memo.create(this, content);
        this.memos.add(memo);
        return memo.getId();
    }

    public void updateMemo(String memoId, String content) {
        this.memos.stream()
                .filter(memo -> memo.getId().equals(memoId))
                .findFirst()
                .ifPresent(memo -> memo.updateContent(content));
    }

    public void deleteMemo(String memoId) {
        this.memos.removeIf(memo -> memo.getId().equals(memoId));
    }

    public List<Memo> getMemos() {
        return new ArrayList<>(memos); // 방어적 복사
    }
}

