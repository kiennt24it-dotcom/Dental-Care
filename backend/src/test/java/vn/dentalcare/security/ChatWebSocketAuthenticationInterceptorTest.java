package vn.dentalcare.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.security.Principal;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketAuthenticationInterceptorTest {
    @Mock JwtService jwtService;
    @Mock UserDetailsService userDetailsService;

    private ChatWebSocketAuthenticationInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new ChatWebSocketAuthenticationInterceptor(jwtService, userDetailsService);
    }

    @Test
    void connectAuthenticatesTheUserAndPreservesItForTheWebSocketSession() {
        UserDetails user = User.withUsername("patient@example.test")
            .password("unused").roles("PATIENT").build();
        when(jwtService.subject("signed-token")).thenReturn(user.getUsername());
        when(jwtService.valid("signed-token", user.getUsername())).thenReturn(true);
        when(userDetailsService.loadUserByUsername(user.getUsername())).thenReturn(user);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        AtomicReference<Principal> sessionPrincipal = new AtomicReference<>();
        accessor.setUserChangeCallback(sessionPrincipal::set);
        accessor.setLeaveMutable(true);
        accessor.setNativeHeader("Authorization", "Bearer signed-token");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> authenticated = interceptor.preSend(message, mock(org.springframework.messaging.MessageChannel.class));
        assertSame(message, authenticated);
        assertEquals(user.getUsername(), sessionPrincipal.get().getName());

        StompHeaderAccessor subscribe = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subscribe.setUser(sessionPrincipal.get());
        subscribe.setDestination("/user/queue/messages");
        Message<byte[]> subscribeMessage = MessageBuilder.createMessage(new byte[0], subscribe.getMessageHeaders());
        assertSame(subscribeMessage,
            interceptor.preSend(subscribeMessage, mock(org.springframework.messaging.MessageChannel.class)));
    }

    @Test
    void connectRejectsAnInvalidToken() {
        when(jwtService.subject("invalid-token")).thenThrow(new JwtException("invalid"));
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer invalid-token");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(MessageDeliveryException.class,
            () -> interceptor.preSend(message, mock(org.springframework.messaging.MessageChannel.class)));
    }

    @Test
    void clientCannotPublishMessagesOrSubscribeToAnotherDestination() {
        UserDetails user = User.withUsername("patient@example.test")
            .password("unused").roles("PATIENT").build();
        var authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            user, null, user.getAuthorities());

        StompHeaderAccessor publish = StompHeaderAccessor.create(StompCommand.SEND);
        publish.setUser(authentication);
        publish.setDestination("/app/chat.send");
        Message<byte[]> publishMessage = MessageBuilder.createMessage(new byte[0], publish.getMessageHeaders());

        StompHeaderAccessor subscribe = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subscribe.setUser(authentication);
        subscribe.setDestination("/queue/messages");
        Message<byte[]> subscribeMessage = MessageBuilder.createMessage(new byte[0], subscribe.getMessageHeaders());
        var channel = mock(org.springframework.messaging.MessageChannel.class);

        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(publishMessage, channel));
        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(subscribeMessage, channel));
    }
}
