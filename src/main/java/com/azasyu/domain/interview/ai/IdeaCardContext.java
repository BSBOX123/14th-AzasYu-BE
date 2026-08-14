package com.azasyu.domain.interview.ai;

import com.azasyu.domain.interview.entity.IdeaCard;

/**
 * 전체 의견 요약의 입력이 되는 익명 아이디어 카드 한 장.
 */
public record IdeaCardContext(String coreOpinion, String rationale, String concern, String alternative) {

    public static IdeaCardContext from(IdeaCard card) {
        return new IdeaCardContext(
            card.getCoreOpinion(), card.getRationale(), card.getConcern(), card.getAlternative()
        );
    }
}
