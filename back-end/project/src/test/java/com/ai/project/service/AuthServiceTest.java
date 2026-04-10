package com.ai.project.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ai.project.dto.RegisterRequest;
import com.ai.project.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
	@Mock
	UserRepository userRepository;
	@InjectMocks
	AuthService authService;
	@Test
	public void register() {
		RegisterRequest registerRequest=new RegisterRequest();
		authService.register(registerRequest);
	}
}
