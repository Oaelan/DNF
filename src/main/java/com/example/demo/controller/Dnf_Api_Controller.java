package com.example.demo.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.demo.dto.CharacterInfoDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@Slf4j
@RequestMapping("/dnf/api/rest")
public class Dnf_Api_Controller {

    @Autowired
    private WebClient webClient;  // WebClient 인스턴스

    @Autowired
    private ObjectMapper objectMapper;  // Jackson ObjectMapper
    
    @GetMapping("/getAllServer")
    public Mono<ResponseEntity<String>> getAllServer() {
        String apiUrl = "/df/servers?apikey=mzhcSOC1Kt5N0X69U7YLfNF0BcFTY4NF"; // Neople API URL

        return webClient.get()
                .uri(apiUrl)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> ResponseEntity.ok(response));  // 서버에서 받은 JSON 데이터를 그대로 반환
    }
    
    // 캐릭터 검색 엔드포인트
    @PostMapping("/search")
    public Mono<ResponseEntity<Map<String, Object>>> searchChar(
            @RequestParam String server,
            @RequestParam String characterName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int size){

        String apiUrl = "/df/servers/" + server + "/characters?characterName=" + characterName + 
                        "&wordType=full&limit=100&apikey=mzhcSOC1Kt5N0X69U7YLfNF0BcFTY4NF";

        return webClient.get() // GET 요청
                .uri(apiUrl) // 호출할 URL
                .retrieve() // 응답 가져오기
                .bodyToMono(String.class)  // 응답을 String으로 변환
                .flatMap(response -> {
                    // 응답을 처리하여 List<CharacterInfoDTO>로 변환
                    List<CharacterInfoDTO> allCharacters = parseCharacterData(response);

                    // List<CharacterInfoDTO>를 Flux로 변환하여 비동기 처리
                    return Flux.fromIterable(allCharacters)
                            .filter(character -> character.getLevel() == 110)  // 레벨이 110인 캐릭터만 필터링
                            
                            .flatMap(character -> getAdventureNameAsync(character.getServerId(), character.getCharacterId())
                                .map(adventureName -> {                                 
                                    if (character.getFame() == null) {
                                        character.setFame(0);
                                    } character.setAdventureName(adventureName);
                                    return character;
                                })
                            )
                            .collectList();  // 다시 List로 변환하여 Mono로 반환 
                    		//.collectList()는 Flux로 처리된 여러 character 객체들을 **하나의 List**로 모아서 변환하는 작업을 합니다.
                })
                .flatMap(filteredCharacters -> {
                	filteredCharacters.sort(Comparator.comparing((CharacterInfoDTO character) -> {
                        // 검색어로 시작하는지 여부를 판단하여 우선순위 지정
                        if (character.getCharacterName().startsWith(characterName)) {
                            return 0;  // 검색어로 시작하는 캐릭터는 우선
                        } else {
                            return 1;  // 그 외의 캐릭터는 이후에 정렬
                        }
                    }).thenComparing(CharacterInfoDTO::getCharacterName));  // 이름순으로 정렬
                    // 페이징 처리
                    int totalCharacters = filteredCharacters.size();
                    int totalPages = (int) Math.ceil((double) totalCharacters / size);
                    List<CharacterInfoDTO> paginatedCharacters = paginateCharacters(filteredCharacters, page, size);

                    // 응답 데이터 구성
                    Map<String, Object> responseBody = new HashMap<>();
                    responseBody.put("characters", paginatedCharacters);
                    responseBody.put("totalPages", totalPages);
                    responseBody.put("currentPage", page);
                    return Mono.just(ResponseEntity.ok(responseBody));  // 비동기 응답 반환
                });
    }

    // 비동기적으로 모험단명을 가져오는 메서드
    private Mono<String> getAdventureNameAsync(String serverId, String characterId) {
        String apiUrl = "/df/servers/" + serverId + "/characters/" + characterId + "?apikey=mzhcSOC1Kt5N0X69U7YLfNF0BcFTY4NF";

        return webClient.get()
                .uri(apiUrl)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    ObjectMapper objectMapper = new ObjectMapper();
                    try {
                        JsonNode rootNode = objectMapper.readTree(response);
                        JsonNode adventureNameNode = rootNode.path("adventureName"); //.path("adventureName")를 통해 응답에서 adventureName 필드를 찾습니다
                        //만약 adventureName이 존재하면 해당 값을 반환하고, 존재하지 않으면 "모험단명 없음"을 반환합니다.
                        return !adventureNameNode.isMissingNode() ? adventureNameNode.asText() : "모험단명 없음";
                    } catch (Exception e) {
                    	//예외 처리: 만약 JSON 파싱 중 문제가 발생하면 예외를 출력하고 "Unknown" 값을 반환합니다.
                        e.printStackTrace();
                        return "Unknown";
                    }
                });
    }

    // JSON 데이터를 List<CharacterInfoDTO>로 변환하는 메서드
    public List<CharacterInfoDTO> parseCharacterData(String jsonData) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonData);
            JsonNode rowsNode = rootNode.get("rows");

            // "rows" 필드가 배열인 경우 처리
            if (rowsNode != null && rowsNode.isArray()) {
                return objectMapper.convertValue(
                    rowsNode, new TypeReference<List<CharacterInfoDTO>>() {}
                );
            } else {
                return List.of(); // 데이터가 없을 경우 빈 리스트 반환
            }
        } catch (Exception e) {
            e.printStackTrace();
            return List.of(); // 파싱 실패 시 빈 리스트 반환
        }
    }

    // 페이징 처리 메서드
    private List<CharacterInfoDTO> paginateCharacters(List<CharacterInfoDTO> characters, int page, int size) {
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, characters.size());
        return characters.subList(fromIndex, toIndex);
    }
}
