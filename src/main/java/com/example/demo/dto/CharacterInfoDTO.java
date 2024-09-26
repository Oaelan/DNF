package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CharacterInfoDTO {
	// 서버 ID
    private String serverId;

    // 캐릭터 ID
    private String characterId;

    // 캐릭터 이름
    private String characterName;

    // 캐릭터 레벨
    private int level;

    // 캐릭터 직업 ID
    private String jobId;

    // 직업 성장 ID (전직 후 ID)
    private String jobGrowId;

    // 기본 직업 이름
    private String jobName;

    // 직업 성장 이름 (전직 후 직업 이름)
    private String jobGrowName;

    // 캐릭터 명성
    private Integer fame;
    
    // 모험단명
    private String adventureName;
}
