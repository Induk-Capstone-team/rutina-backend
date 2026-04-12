package com.rutina.rutinabackend.domain.ailog.service;

import com.rutina.rutinabackend.domain.ailog.dto.AiRequest;
import com.rutina.rutinabackend.domain.ailog.dto.AiResponse;
//import com.rutina.rutinabackend.domain.ailog.entity.AiLog;
//import com.rutina.rutinabackend.domain.ailog.repository.AiLogRepository;
import com.rutina.rutinabackend.domain.user.entity.User;
import com.rutina.rutinabackend.domain.user.repository.UserRepository;
import com.rutina.rutinabackend.global.exception.BusinessException;
//import com.rutina.rutinabackend.global.exception.ErrorCode;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;

@Service
public class AiService {

    private final ChatClient chatClient;
    private final UserRepository userRepository;
//    private final AiLogRepository aiLogRepository;

    public AiService(
            ChatClient.Builder chatClientBuilder,
            UserRepository userRepository
//            AiLogRepository aiLogRepository
    ) {
        this.chatClient = chatClientBuilder.build();
        this.userRepository = userRepository;
//        this.aiLogRepository = aiLogRepository;
    }

    //간단하게 임시 프롬포트 작성해 둠
    public AiResponse generate(AiRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND",
                        "사용자를 찾을 수 없습니다."
                ));

        validateUserInfo(user);

        String prompt = """
                사용자 정보입니다.
                
                닉네임: %s
                직업: %s
                연령: %s
                성별: %s
                """.formatted(
                user.getNickname(),
                user.getRole(),
                user.getAge(),
                convertGender(user.getGender())
        );

        String result = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

//        AiLog aiLog = AiLog.builder()
//                .userId(user.getId())
//                .routineId(request.routineId())
//                .requestType("ROUTINE_RECOMMEND")
//                .prompt(prompt)
//                .response(result)
//                .build();
//
//        aiLogRepository.save(aiLog);

        return new AiResponse(result);
    }

    private void validateUserInfo(User user) {
        boolean missingRole = user.getRole() == null || user.getRole().isBlank();
        boolean missingAge = user.getAge() == null;
        boolean missingGender = user.getGender() == null;

        throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "AI_PROFILE_INFO_REQUIRED",
                "직업, 연령, 성별 정보를 입력해야 이용 가능한 서비스입니다."
        );
    }

    private String convertGender(Integer gender) {
        return switch (gender) {
            case 0 -> "남성";
            case 1 -> "여성";
            case 2 -> "기타";
            default -> "미입력";
        };
    }
}