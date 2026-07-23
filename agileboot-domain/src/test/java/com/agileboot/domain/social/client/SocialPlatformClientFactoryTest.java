package com.agileboot.domain.social.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.agileboot.common.exception.ApiException;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

/**
 * @author SocialMedia-Hub
 */
class SocialPlatformClientFactoryTest {

    @Test
    void should_dispatch_by_platform() {
        SocialPlatformClient xhs = mock(SocialPlatformClient.class);
        when(xhs.platform()).thenReturn("xhs");
        SocialPlatformClient bili = mock(SocialPlatformClient.class);
        when(bili.platform()).thenReturn("bili");

        SocialPlatformClientFactory factory = new SocialPlatformClientFactory(Arrays.asList(xhs, bili));
        factory.init();

        assertEquals(xhs, factory.get("xhs"));
        assertEquals(bili, factory.get("bili"));
    }

    @Test
    void should_throw_for_unknown_platform() {
        SocialPlatformClient xhs = mock(SocialPlatformClient.class);
        when(xhs.platform()).thenReturn("xhs");

        SocialPlatformClientFactory factory = new SocialPlatformClientFactory(Collections.singletonList(xhs));
        factory.init();

        assertThrows(ApiException.class, () -> factory.get("douyin"));
    }

}
