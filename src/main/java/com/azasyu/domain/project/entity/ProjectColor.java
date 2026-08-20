package com.azasyu.domain.project.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프로젝트 카드 색상", allowableValues = {"RED", "ORANGE", "GREEN", "BLUE", "BLACK"})
public enum ProjectColor {
    RED,
    ORANGE,
    GREEN,
    BLUE,
    BLACK
}
