package vn.dentalcare.security;

import io.jsonwebtoken.JwtException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class ChatWebSocketAuthenticationInterceptor implements ChannelInterceptor {
    private static final String MESSAGE_DESTINATION = "/user/queue/messages";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public ChatWebSocketAuthenticationInterceptor(JwtService jwtService,
                                                   UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            throw new MessageDeliveryException("Invalid WebSocket message");
        }
        if (accessor.getCommand() == null) {
            throw new MessageDeliveryException("Invalid WebSocket message");
        }

        if (accessor.getCommand() == StompCommand.CONNECT) {
            authenticate(accessor);
        } else if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
            if (accessor.getUser() == null || !MESSAGE_DESTINATION.equals(accessor.getDestination())) {
                throw new MessageDeliveryException("WebSocket subscription is not authorized");
            }
        } else if (accessor.getCommand() == StompCommand.SEND) {
            throw new MessageDeliveryException("Client message publishing is not allowed");
        }

        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new MessageDeliveryException("WebSocket authentication is required");
        }

        String token = authorization.substring(7);
        try {
            String email = jwtService.subject(token);
            UserDetails user = userDetailsService.loadUserByUsername(email);
            if (!user.isEnabled() || !jwtService.valid(token, user.getUsername())) {
                throw new MessageDeliveryException("WebSocket authentication failed");
            }
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities());
            accessor.setUser(authentication);
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException exception) {
            throw new MessageDeliveryException("WebSocket authentication failed");
        }
    }
}
