package com.ardom.automotive_event_api.user;

import com.ardom.automotive_event_api.user.dto.request.UpdateProfileRequest;
import com.ardom.automotive_event_api.user.exception.UserNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User getCurrentUser(Authentication authentication){
        return (User) authentication.getPrincipal();
    }

    @Transactional
    public void updateProfile(Long id, UpdateProfileRequest request){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        boolean updated = false;

        if(request.name() != null && !request.name().isBlank()){
            user.setName(request.name());
            updated = true;
        }
        if(request.surname() != null && !request.surname().isBlank()){
            user.setSurname(request.surname());
            updated = true;
        }

        if(updated){
            user.setModifiedAt(LocalDateTime.now());
        }
    }
}
