package com.smarthome.security;

import com.smarthome.config.InternalApiProperties;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternalApiFilterTest {

    @Mock InternalApiProperties internalApiProperties;
    @Mock FilterChain filterChain;

    @InjectMocks InternalApiFilter filter;

    @BeforeEach
    void setUp() {
        when(internalApiProperties.getToken()).thenReturn("test-internal-token");
    }

    @Test
    void blocksInternalRequestWithoutToken() throws Exception {
        MockHttpServletRequest req = internalRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, filterChain);

        assertEquals(401, res.getStatus());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void allowsInternalRequestWithValidToken() throws Exception {
        MockHttpServletRequest req = internalRequest();
        req.addHeader("X-Internal-Token", "test-internal-token");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, filterChain);

        verify(filterChain).doFilter(req, res);
    }

    private static MockHttpServletRequest internalRequest() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/internal/products/search");
        req.setServletPath("/internal/products/search");
        return req;
    }
}
