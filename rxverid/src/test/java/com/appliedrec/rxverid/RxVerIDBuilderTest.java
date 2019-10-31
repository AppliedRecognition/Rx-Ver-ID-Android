package com.appliedrec.rxverid;

import android.content.Context;

import com.appliedrec.verid.core.IFaceDetectionFactory;
import com.appliedrec.verid.core.IFaceRecognitionFactory;
import com.appliedrec.verid.core.IUserManagementFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RxVerIDBuilderTest {

    @Test
    public void test_getConfiguration() {
        RxVerID.Builder builder = mock(RxVerID.Builder.class);
        when(builder.getConfiguration()).thenCallRealMethod();

        RxVerID.Builder.Configuration configuration = builder.getConfiguration();

        assertNull(configuration);
    }

    @Test
    public void test_setFaceDetectionFactory_succeeds() {
        RxVerID.Builder.Configuration mockConfiguration = mock(RxVerID.Builder.Configuration.class);
        RxVerID.Builder builder = mock(RxVerID.Builder.class);
        when(builder.setFaceDetectionFactory(any())).thenCallRealMethod();
        when(builder.getConfiguration()).thenReturn(mockConfiguration);

        builder.setFaceDetectionFactory(mock(IFaceDetectionFactory.class));

        verify(builder).getConfiguration();
        verify(mockConfiguration).setFaceDetectionFactory(any());
    }

    @Test
    public void test_setFaceRecognitionFactory_succeeds() {
        RxVerID.Builder.Configuration mockConfiguration = mock(RxVerID.Builder.Configuration.class);
        RxVerID.Builder builder = mock(RxVerID.Builder.class);
        when(builder.setFaceRecognitionFactory(any())).thenCallRealMethod();
        when(builder.getConfiguration()).thenReturn(mockConfiguration);

        builder.setFaceRecognitionFactory(mock(IFaceRecognitionFactory.class));

        verify(builder).getConfiguration();
        verify(mockConfiguration).setFaceRecognitionFactory(any());
    }

    @Test
    public void test_setUserManagementFactory_succeeds() {
        RxVerID.Builder.Configuration mockConfiguration = mock(RxVerID.Builder.Configuration.class);
        RxVerID.Builder builder = mock(RxVerID.Builder.class);
        when(builder.setUserManagementFactory(any())).thenCallRealMethod();
        when(builder.getConfiguration()).thenReturn(mockConfiguration);

        builder.setUserManagementFactory(mock(IUserManagementFactory.class));

        verify(builder).getConfiguration();
        verify(mockConfiguration).setUserManagementFactory(any());
    }

    @Test
    public void test_build_returnExistingInstance() {
        RxVerID rxVerid = mock(RxVerID.class);
        RxVerID.Builder builder = mock(RxVerID.Builder.class);
        when(builder.build()).thenCallRealMethod();
        when(builder.getInstanceForConfiguration(any())).thenReturn(rxVerid);

        RxVerID builtVerid = builder.build();

        assertEquals(rxVerid, builtVerid);
    }

    @Test
    public void test_build_returnNewInstance() {
        RxVerID.Builder.Configuration mockConfiguration = mock(RxVerID.Builder.Configuration.class);
        when(mockConfiguration.getContext()).thenReturn(mock(Context.class));
        RxVerID.Builder builder = mock(RxVerID.Builder.class);
        when(builder.getConfiguration()).thenReturn(mockConfiguration);
        when(builder.build()).thenCallRealMethod();
        when(builder.getInstanceForConfiguration(any())).thenReturn(null);

        builder.build();
    }

    @Test
    public void test_getInstanceForConfiguration() {
        RxVerID.Builder.Configuration mockConfiguration = mock(RxVerID.Builder.Configuration.class);
        RxVerID.Builder builder = mock(RxVerID.Builder.class);
        when(builder.getInstanceForConfiguration(any())).thenCallRealMethod();

        assertNull(builder.getInstanceForConfiguration(mockConfiguration));
    }
}
