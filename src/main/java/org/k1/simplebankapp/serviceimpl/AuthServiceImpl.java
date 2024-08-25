package org.k1.simplebankapp.serviceimpl;

import lombok.extern.slf4j.Slf4j;
import org.k1.simplebankapp.dto.LoginRequest;
import org.k1.simplebankapp.dto.LoginResponse;
import org.k1.simplebankapp.entity.User;
import org.k1.simplebankapp.mapper.AuthMapper;
import org.k1.simplebankapp.repository.UserRepository;
import org.k1.simplebankapp.service.AuthService;
import org.k1.simplebankapp.service.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.Map;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ValidationService validationService;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    @Autowired
    private AuthMapper authMapper;

    @Value("${BASEURL}")
    private String baseUrl;

    @Override
    public LoginResponse login(LoginRequest request) {
        validationService.validate(request);
        User checkUser = userRepository.findByUsername(request.getUsername());

        // Check if the user exists
        if (checkUser == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Username or password invalid");
        }

        // Check if the account is locked
        if (!checkUser.isAccountNonLocked()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your account is locked due to too many failed login attempts. Please try again later.");
        }

        // Check if the password matches
        if (!encoder.matches(request.getPassword(), checkUser.getPassword())) {
            // Increment failed attempts
            int newFailAttempts = checkUser.getLoginAttempts() + 1;
            checkUser.setLoginAttempts(newFailAttempts);

            if (newFailAttempts >= 3) {
                // Lock the account if failed attempts >= 3
                checkUser.setAccountNonLocked(false);
                userRepository.save(checkUser);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your account is locked, please change password!");
            }

            userRepository.save(checkUser);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your invalid username or password has been attempted " + checkUser.getLoginAttempts() + " times");
        }
        String url = baseUrl + "/oauth/token?username=" + checkUser.getUsername() +
                "&password=" + request.getPassword() +
                "&grant_type=password" +
                "&client_id=my-client-web" +
                "&client_secret=password";
        ResponseEntity<Map<Object, Object>> response = restTemplateBuilder.build().exchange(url, HttpMethod.POST, null, new
                ParameterizedTypeReference<Map<Object, Object>>() {
                }
        );
        if (response.getStatusCode() == HttpStatus.OK) {
            return authMapper.toLoginResponse(response);
        } else {
            throw new ResponseStatusException(response.getStatusCode(), response.getStatusCode().getReasonPhrase());
        }
    }

    @Override
    public Object forgotPassword() {
        return null;
    }
}
