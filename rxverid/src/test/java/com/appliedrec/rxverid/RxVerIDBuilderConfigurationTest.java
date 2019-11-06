package com.appliedrec.rxverid;

import android.content.Context;

import com.appliedrec.verid.core.IFaceDetectionFactory;
import com.appliedrec.verid.core.IFaceRecognitionFactory;
import com.appliedrec.verid.core.IUserManagementFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RxVerIDBuilderConfigurationTest {

    @Test
    public void test_getContext_returnsNull() {
        RxVerID.Builder.Configuration configuration = mock(RxVerID.Builder.Configuration.class);
        when(configuration.getContext()).thenCallRealMethod();

        assertNull(configuration.getContext());
    }

    @Test
    public void test_getFaceDetectionFactory_returnsNull() {
        RxVerID.Builder.Configuration configuration = mock(RxVerID.Builder.Configuration.class);
        when(configuration.getFaceDetectionFactory()).thenCallRealMethod();

        assertNull(configuration.getFaceDetectionFactory());
    }

    @Test
    public void test_getFaceRecognitionFactory_returnsNull() {
        RxVerID.Builder.Configuration configuration = mock(RxVerID.Builder.Configuration.class);
        when(configuration.getFaceRecognitionFactory()).thenCallRealMethod();

        assertNull(configuration.getFaceRecognitionFactory());
    }

    @Test
    public void test_getUserManagementFactory_returnsNull() {
        RxVerID.Builder.Configuration configuration = mock(RxVerID.Builder.Configuration.class);
        when(configuration.getUserManagementFactory()).thenCallRealMethod();

        assertNull(configuration.getUserManagementFactory());
    }

    @Test
    public void test_setFaceDetectionFactory_succeeds() {
        RxVerID.Builder.Configuration configuration = mock(RxVerID.Builder.Configuration.class);
        doCallRealMethod().when(configuration).setFaceDetectionFactory(any());
        doCallRealMethod().when(configuration).getFaceDetectionFactory();
        IFaceDetectionFactory faceDetectionFactory = mock(IFaceDetectionFactory.class);
        configuration.setFaceDetectionFactory(faceDetectionFactory);
        assertEquals(faceDetectionFactory, configuration.getFaceDetectionFactory());
    }

    @Test
    public void test_setFaceRecognitionFactory_succeeds() {
        RxVerID.Builder.Configuration configuration = mock(RxVerID.Builder.Configuration.class);
        doCallRealMethod().when(configuration).setFaceRecognitionFactory(any());
        doCallRealMethod().when(configuration).getFaceRecognitionFactory();
        IFaceRecognitionFactory faceRecognitionFactory = mock(IFaceRecognitionFactory.class);
        configuration.setFaceRecognitionFactory(faceRecognitionFactory);
        assertEquals(faceRecognitionFactory, configuration.getFaceRecognitionFactory());
    }

    @Test
    public void test_setUserManagementFactory_succeeds() {
        RxVerID.Builder.Configuration configuration = mock(RxVerID.Builder.Configuration.class);
        doCallRealMethod().when(configuration).setUserManagementFactory(any());
        doCallRealMethod().when(configuration).getUserManagementFactory();
        IUserManagementFactory userManagementFactory = mock(IUserManagementFactory.class);
        configuration.setUserManagementFactory(userManagementFactory);
        assertEquals(userManagementFactory, configuration.getUserManagementFactory());
    }

    @Test
    public void test_equals_returnsTrue() {
        RxVerID.Builder.Configuration configuration = new RxVerID.Builder.Configuration();
        RxVerID.Builder.Configuration otherConfiguration = mock(RxVerID.Builder.Configuration.class);
        assertTrue(configuration.equals(otherConfiguration));
    }

    @Test
    public void test_equalsToNonConfiguration_returnsFalse() {
        RxVerID.Builder.Configuration configuration = new RxVerID.Builder.Configuration();
        assertFalse(configuration.equals(mock(Object.class)));
    }

    @Test
    public void test_equalsToConfigurationWithOtherContext_returnsFalse() {
        RxVerID.Builder.Configuration configuration = new RxVerID.Builder.Configuration();
        RxVerID.Builder.Configuration otherConfiguration = mock(RxVerID.Builder.Configuration.class);
        when(otherConfiguration.getContext()).thenReturn(mock(Context.class));
        assertFalse(configuration.equals(otherConfiguration));
    }
}
