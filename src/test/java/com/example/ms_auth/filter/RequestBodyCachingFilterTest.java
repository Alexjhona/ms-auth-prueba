package com.example.ms_auth.filter;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RequestBodyCachingFilterTest {

    private final RequestBodyCachingFilter filter = new RequestBodyCachingFilter();

    @Test
    void preservesAlreadyCachedRequest() throws Exception {
        ContentCachingRequestWrapper request =
                new ContentCachingRequestWrapper(new MockHttpServletRequest());
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void wrapsPlainRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        ArgumentCaptor<ServletRequest> captor = ArgumentCaptor.forClass(ServletRequest.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(captor.capture(), org.mockito.ArgumentMatchers.same(response));
        assertThat(captor.getValue()).isInstanceOf(ContentCachingRequestWrapper.class);
    }
}
