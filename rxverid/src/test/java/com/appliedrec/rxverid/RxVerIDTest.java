package com.appliedrec.rxverid;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;

import androidx.exifinterface.media.ExifInterface;

import com.appliedrec.verid.core.Bearing;
import com.appliedrec.verid.core.DetectedFace;
import com.appliedrec.verid.core.Face;
import com.appliedrec.verid.core.FaceDetectionRecognitionFactory;
import com.appliedrec.verid.core.IFaceDetection;
import com.appliedrec.verid.core.IFaceDetectionFactory;
import com.appliedrec.verid.core.IFaceRecognition;
import com.appliedrec.verid.core.IFaceRecognitionFactory;
import com.appliedrec.verid.core.IRecognizable;
import com.appliedrec.verid.core.IUserManagement;
import com.appliedrec.verid.core.IUserManagementFactory;
import com.appliedrec.verid.core.RecognizableFace;
import com.appliedrec.verid.core.UserIdentification;
import com.appliedrec.verid.core.VerID;
import com.appliedrec.verid.core.VerIDFactory;
import com.appliedrec.verid.core.VerIDImage;
import com.appliedrec.verid.core.VerIDSessionResult;

import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.FileNotFoundException;
import java.io.InputStream;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RxVerIDTest {

    @Ignore
    @Test
    public void test_getVerID_succeeds() {
        try {
            FaceDetectionRecognitionFactory faceDetectionRecognitionFactory = mock(FaceDetectionRecognitionFactory.class);
            VerID mockVerID = mock(VerID.class);
            RxVerID mockRxVerID = mock(RxVerID.class);
            VerIDFactory mockVerIDFactory = mock(VerIDFactory.class);
            doCallRealMethod().when(mockVerIDFactory).setFaceDetectionFactory(any());
            doCallRealMethod().when(mockVerIDFactory).setFaceRecognitionFactory(any());
            doCallRealMethod().when(mockVerIDFactory).setUserManagementFactory(any());
            when(mockRxVerID.createVerIDFactory()).thenReturn(mockVerIDFactory);
            when(mockRxVerID.getFaceDetectionFactory()).thenReturn(faceDetectionRecognitionFactory);
            when(mockRxVerID.getFaceRecognitionFactory()).thenReturn(faceDetectionRecognitionFactory);
            when(mockVerIDFactory.createVerIDSync()).thenReturn(mockVerID);
            doCallRealMethod().when(mockRxVerID).getVerID();

            TestObserver<VerID> testObserver = mockRxVerID.getVerID().test();

            testObserver
                    .assertSubscribed()
                    .assertNoErrors()
                    .assertValues(mockVerID)
                    .assertComplete();

            verify(mockRxVerID).createVerIDFactory();
            verify(mockVerIDFactory).createVerIDSync();
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Ignore
    @Test
    public void test_getVerID_fails() {
        try {
            String exceptionMessage = "Test exception";
            RxVerID mockRxVerID = mock(RxVerID.class);
            VerIDFactory mockVerIDFactory = mock(VerIDFactory.class);
            doThrow(new Exception(exceptionMessage)).when(mockVerIDFactory).createVerIDSync();
            doReturn(mockVerIDFactory).when(mockRxVerID).createVerIDFactory();
            doCallRealMethod().when(mockRxVerID).getVerID();
            TestObserver<VerID> testObserver = mockRxVerID.getVerID().test();

            testObserver
                    .assertSubscribed()
                    .assertErrorMessage(exceptionMessage)
                    .assertTerminated();

            verify(mockRxVerID).createVerIDFactory();
            verify(mockVerIDFactory).createVerIDSync();
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_getContext_returnsContext() {
        Context context = mock(Context.class);
        RxVerID rxVerID = new RxVerID.Builder(context).build();
        assertEquals(context, rxVerID.getContext());
    }

    @Test
    public void test_detectFacesInImage_succeeds() {
        try {
            Face mockFace = mock(Face.class);
            IFaceDetection mockFaceDetection = mock(IFaceDetection.class);
            when(mockFaceDetection.detectFacesInImage(any(), anyInt(), anyInt())).thenReturn(new Face[]{mockFace});
            VerID mockVerID = mock(VerID.class);
            when(mockVerID.getFaceDetection()).thenReturn(mockFaceDetection);
            VerIDImage mockImage = mock(VerIDImage.class);
            RxVerID rxVerID = mock(RxVerID.class);
            when(rxVerID.getVerID()).thenReturn(Single.just(mockVerID));
            when(rxVerID.detectFacesInImage(any(VerIDImage.class), anyInt())).thenCallRealMethod();
            when(rxVerID.detectFacesInImage(any(VerID.class), any(VerIDImage.class), anyInt())).thenCallRealMethod();

            TestObserver<Face> faceTestObserver = rxVerID.detectFacesInImage(mockImage, 1).test();
            faceTestObserver.assertSubscribed().assertNoErrors().assertValue(mockFace).assertComplete();
            verify(mockVerID).getFaceDetection();
            verify(mockFaceDetection).detectFacesInImage(any(), anyInt(), anyInt());
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_detectFacesInImage_noFaces() {
        try {
            IFaceDetection mockFaceDetection = mock(IFaceDetection.class);
            when(mockFaceDetection.detectFacesInImage(any(), anyInt(), anyInt())).thenReturn(new Face[0]);
            VerID mockVerID = mock(VerID.class);
            when(mockVerID.getFaceDetection()).thenReturn(mockFaceDetection);
            VerIDImage mockImage = mock(VerIDImage.class);
            RxVerID rxVerID = mock(RxVerID.class);
            when(rxVerID.getVerID()).thenReturn(Single.just(mockVerID));
            when(rxVerID.detectFacesInImage(any(VerIDImage.class), anyInt())).thenCallRealMethod();
            when(rxVerID.detectFacesInImage(any(VerID.class), any(VerIDImage.class), anyInt())).thenCallRealMethod();

            TestObserver<Face> faceTestObserver = rxVerID.detectFacesInImage(mockImage, 1).test();

            faceTestObserver
                    .assertSubscribed()
                    .assertNoErrors()
                    .assertNoValues()
                    .assertComplete();

            verify(mockVerID).getFaceDetection();
            verify(mockFaceDetection).detectFacesInImage(any(), anyInt(), anyInt());
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_detectFacesInImage_fails() {
        try {
            IFaceDetection mockFaceDetection = mock(IFaceDetection.class);
            String exceptionMessage = "Test message";
            when(mockFaceDetection.detectFacesInImage(any(), anyInt(), anyInt())).thenThrow(new Exception(exceptionMessage));
            VerID mockVerID = mock(VerID.class);
            when(mockVerID.getFaceDetection()).thenReturn(mockFaceDetection);
            VerIDImage mockImage = mock(VerIDImage.class);
            RxVerID rxVerID = mock(RxVerID.class);
            when(rxVerID.getVerID()).thenReturn(Single.just(mockVerID));
            when(rxVerID.detectFacesInImage(any(VerIDImage.class), anyInt())).thenCallRealMethod();
            when(rxVerID.detectFacesInImage(any(VerID.class), any(VerIDImage.class), anyInt())).thenCallRealMethod();
            TestObserver<Face> faceTestObserver = rxVerID.detectFacesInImage(mockImage, 1).test();

            faceTestObserver
                    .assertSubscribed()
                    .assertErrorMessage(exceptionMessage)
                    .assertTerminated();

            verify(mockVerID).getFaceDetection();
            verify(mockFaceDetection).detectFacesInImage(any(), anyInt(), anyInt());
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_detectFacesInImageWithContext_succeeds() {
        try {
            Face mockFace = mock(Face.class);
            IFaceDetection mockFaceDetection = mock(IFaceDetection.class);
            when(mockFaceDetection.detectFacesInImage(any(), anyInt(), anyInt())).thenReturn(new Face[]{mockFace});
            VerID mockVerID = mock(VerID.class);
            when(mockVerID.getFaceDetection()).thenReturn(mockFaceDetection);
            VerIDImage mockImage = mock(VerIDImage.class);
            RxVerID rxVerID = mock(RxVerID.class);
            doReturn(Single.just(mockVerID)).when(rxVerID).getVerID();
            doReturn(Single.just(mockImage)).when(rxVerID).convertUriToVerIDImage(any());
            doCallRealMethod().when(rxVerID).detectFacesInImage(any(Uri.class), anyInt());
            doCallRealMethod().when(rxVerID).detectFacesInImage(any(VerIDImage.class), anyInt());
            doCallRealMethod().when(rxVerID).detectFacesInImage(any(VerID.class), any(VerIDImage.class), anyInt());
            TestObserver<Face> faceTestObserver = rxVerID.detectFacesInImage(mock(Uri.class), 1).test();
            faceTestObserver.assertSubscribed().assertNoErrors().assertValue(mockFace).assertComplete();
            verify(mockVerID).getFaceDetection();
            verify(mockFaceDetection).detectFacesInImage(any(), anyInt(), anyInt());
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_detectRecognizableFacesInImageWithContext_succeeds() {
        try {
            RecognizableFace mockRecognizableFace = mock(RecognizableFace.class);
            Face mockFace = mock(Face.class);
            IFaceDetection mockFaceDetection = mock(IFaceDetection.class);
            when(mockFaceDetection.detectFacesInImage(any(), anyInt(), anyInt())).thenReturn(new Face[]{mockFace});
            IFaceRecognition mockFaceRecognition = mock(IFaceRecognition.class);
            when(mockFaceRecognition.createRecognizableFacesFromFaces(any(), any())).thenReturn(new RecognizableFace[]{mockRecognizableFace});
            VerID mockVerID = mock(VerID.class);
            when(mockVerID.getFaceDetection()).thenReturn(mockFaceDetection);
            when(mockVerID.getFaceRecognition()).thenReturn(mockFaceRecognition);
            VerIDImage mockImage = mock(VerIDImage.class);
            RxVerID rxVerID = mock(RxVerID.class);
            doReturn(Single.just(mockVerID)).when(rxVerID).getVerID();
            doReturn(Single.just(mockImage)).when(rxVerID).convertUriToVerIDImage(any());
            doCallRealMethod().when(rxVerID).detectFacesInImage(any(VerIDImage.class), anyInt());
            doCallRealMethod().when(rxVerID).detectFacesInImage(any(VerID.class), any(VerIDImage.class), anyInt());
            doCallRealMethod().when(rxVerID).detectRecognizableFacesInImage(any(Uri.class), anyInt());
            doCallRealMethod().when(rxVerID).detectRecognizableFacesInImage(any(VerIDImage.class), anyInt());
            doCallRealMethod().when(rxVerID).convertFaceToRecognizableFace(any(), any());
            doCallRealMethod().when(rxVerID).convertFaceToRecognizableFace(any(VerID.class), any(), any());

            TestObserver<RecognizableFace> faceTestObserver = rxVerID.detectRecognizableFacesInImage(mock(Uri.class), 1).test();

            faceTestObserver
                    .assertSubscribed()
                    .assertNoErrors()
                    .assertValue(mockRecognizableFace)
                    .assertComplete();

            verify(mockVerID).getFaceDetection();
            verify(mockVerID).getFaceRecognition();
            verify(mockFaceDetection).detectFacesInImage(any(), anyInt(), anyInt());
            verify(mockFaceRecognition).createRecognizableFacesFromFaces(any(), any());
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_detectRecognizableFacesInImage_succeeds() {
        try {
            RecognizableFace mockRecognizableFace = mock(RecognizableFace.class);
            Face mockFace = mock(Face.class);
            IFaceDetection mockFaceDetection = mock(IFaceDetection.class);
            when(mockFaceDetection.detectFacesInImage(any(), anyInt(), anyInt())).thenReturn(new Face[]{mockFace});
            IFaceRecognition mockFaceRecognition = mock(IFaceRecognition.class);
            when(mockFaceRecognition.createRecognizableFacesFromFaces(any(), any())).thenReturn(new RecognizableFace[]{mockRecognizableFace});
            VerID mockVerID = mock(VerID.class);
            when(mockVerID.getFaceDetection()).thenReturn(mockFaceDetection);
            when(mockVerID.getFaceRecognition()).thenReturn(mockFaceRecognition);
            VerIDImage mockImage = mock(VerIDImage.class);
            RxVerID rxVerID = mock(RxVerID.class);
            when(rxVerID.getVerID()).thenReturn(Single.just(mockVerID));
            when(rxVerID.detectRecognizableFacesInImage(any(VerIDImage.class), anyInt())).thenCallRealMethod();
            when(rxVerID.detectFacesInImage(any(VerIDImage.class), anyInt())).thenCallRealMethod();
            when(rxVerID.detectFacesInImage(any(VerID.class), any(VerIDImage.class), anyInt())).thenCallRealMethod();
            when(rxVerID.convertFaceToRecognizableFace(any(), any())).thenCallRealMethod();
            when(rxVerID.convertFaceToRecognizableFace(any(VerID.class), any(), any())).thenCallRealMethod();

            TestObserver<RecognizableFace> faceTestObserver = rxVerID.detectRecognizableFacesInImage(mockImage, 1).test();

            faceTestObserver
                    .assertSubscribed()
                    .assertNoErrors()
                    .assertValue(mockRecognizableFace)
                    .assertComplete();

            verify(mockVerID).getFaceDetection();
            verify(mockVerID).getFaceRecognition();
            verify(mockFaceDetection).detectFacesInImage(any(), anyInt(), anyInt());
            verify(mockFaceRecognition).createRecognizableFacesFromFaces(any(), any());
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_detectRecognizableFacesInImage_convertToRecognizableFacesFails() {
        try {
            String exceptionMessage = "Test message";
            Face mockFace = mock(Face.class);
            IFaceDetection mockFaceDetection = mock(IFaceDetection.class);
            when(mockFaceDetection.detectFacesInImage(any(), anyInt(), anyInt())).thenReturn(new Face[]{mockFace});
            IFaceRecognition mockFaceRecognition = mock(IFaceRecognition.class);
            when(mockFaceRecognition.createRecognizableFacesFromFaces(any(), any())).thenThrow(new Exception(exceptionMessage));
            VerID mockVerID = mock(VerID.class);
            when(mockVerID.getFaceDetection()).thenReturn(mockFaceDetection);
            when(mockVerID.getFaceRecognition()).thenReturn(mockFaceRecognition);
            VerIDImage mockImage = mock(VerIDImage.class);
            RxVerID rxVerID = mock(RxVerID.class);
            when(rxVerID.getVerID()).thenReturn(Single.just(mockVerID));
            when(rxVerID.convertFaceToRecognizableFace(any(), any())).thenCallRealMethod();
            when(rxVerID.convertFaceToRecognizableFace(any(VerID.class), any(), any())).thenCallRealMethod();
            when(rxVerID.detectFacesInImage(any(VerIDImage.class), anyInt())).thenCallRealMethod();
            when(rxVerID.detectFacesInImage(any(VerID.class), any(VerIDImage.class), anyInt())).thenCallRealMethod();
            when(rxVerID.detectRecognizableFacesInImage(any(VerIDImage.class), anyInt())).thenCallRealMethod();

            TestObserver<RecognizableFace> faceTestObserver = rxVerID.detectRecognizableFacesInImage(mockImage, 1).test();

            faceTestObserver
                    .assertSubscribed()
                    .assertErrorMessage(exceptionMessage)
                    .assertTerminated();

            verify(mockVerID).getFaceDetection();
            verify(mockVerID).getFaceRecognition();
            verify(mockFaceDetection).detectFacesInImage(any(), anyInt(), anyInt());
            verify(mockFaceRecognition).createRecognizableFacesFromFaces(any(), any());
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_identifyUsersInImage_succeeds() {
        try {
            String testUserId = "testUserId";
            String testUserId2 = "testUserId2";
            float testScore = 4.6f;
            float testScore2 = 4.7f;
            RecognizableFace mockRecognizableFace = mock(RecognizableFace.class);
            Face mockFace = mock(Face.class);
            IFaceDetection mockFaceDetection = mock(IFaceDetection.class);
            when(mockFaceDetection.detectFacesInImage(any(), anyInt(), anyInt())).thenReturn(new Face[]{mockFace});
            IFaceRecognition mockFaceRecognition = mock(IFaceRecognition.class);
            when(mockFaceRecognition.createRecognizableFacesFromFaces(any(), any())).thenReturn(new RecognizableFace[]{mockRecognizableFace});
            when(mockFaceRecognition.compareSubjectFacesToFaces(any(), any())).thenReturn(testScore, testScore2);
            when(mockFaceRecognition.getAuthenticationThreshold()).thenReturn(4.5f);
            IUserManagement mockUserManagement = mock(IUserManagement.class);
            when(mockUserManagement.getUsers()).thenReturn(new String[]{testUserId, testUserId2});
            when(mockUserManagement.getFacesOfUser(any())).thenReturn(new IRecognizable[]{mockRecognizableFace});
            VerID mockVerID = mock(VerID.class);
            when(mockVerID.getFaceDetection()).thenReturn(mockFaceDetection);
            when(mockVerID.getFaceRecognition()).thenReturn(mockFaceRecognition);
            when(mockVerID.getUserManagement()).thenReturn(mockUserManagement);
            VerIDImage mockImage = mock(VerIDImage.class);
            RxVerID rxVerID = mock(RxVerID.class);
            when(rxVerID.getVerID()).thenReturn(Single.just(mockVerID));
            when(rxVerID.getUserIdentification(any(VerID.class))).thenCallRealMethod();
            when(rxVerID.identifyUsersInImage(any(VerIDImage.class))).thenCallRealMethod();
            when(rxVerID.identifyUsersInImage(any(VerID.class), any(VerIDImage.class))).thenCallRealMethod();
            when(rxVerID.detectFacesInImage(any(VerID.class), any(VerIDImage.class), anyInt())).thenCallRealMethod();
            doCallRealMethod().when(rxVerID).identifyUsersInFace(any(VerID.class), any());
            when(rxVerID.detectRecognizableFacesInImage(any(VerID.class), any(VerIDImage.class), anyInt())).thenCallRealMethod();
            when(rxVerID.convertFaceToRecognizableFace(any(VerID.class), any(), any())).thenCallRealMethod();
            Pair<String, Float> score = new Pair<>(testUserId, testScore);
            Pair<String, Float> score2 = new Pair<>(testUserId2, testScore2);

            TestObserver<Pair<String,Float>> testObserver = rxVerID.identifyUsersInImage(mockImage).test();

            testObserver
                    .assertSubscribed()
                    .assertNoErrors()
                    .assertValues(score2, score)
                    .assertComplete();
            verify(mockVerID, atLeastOnce()).getFaceDetection();
            verify(mockVerID, atLeastOnce()).getFaceRecognition();
            verify(mockVerID, atLeastOnce()).getUserManagement();
            verify(mockFaceDetection).detectFacesInImage(any(), anyInt(), anyInt());
            verify(mockFaceRecognition).createRecognizableFacesFromFaces(any(), any());
            verify(mockUserManagement).getUsers();
            verify(mockUserManagement).getFacesOfUser(eq(testUserId));
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_identifyUsersInImage_detectionFails() {
        try {
            String exceptionMessage = "Test message";
            VerIDImage mockImage = mock(VerIDImage.class);
            VerID verID = mock(VerID.class);
            RxVerID rxVerID = mock(RxVerID.class);
            when(rxVerID.getVerID()).thenReturn(Single.just(verID));
            when(rxVerID.identifyUsersInImage(any(VerIDImage.class))).thenCallRealMethod();
            when(rxVerID.identifyUsersInImage(any(VerID.class), any(VerIDImage.class))).thenCallRealMethod();
            when(rxVerID.detectRecognizableFacesInImage(any(VerID.class), any(VerIDImage.class), anyInt())).thenReturn(Observable.error(new Exception(exceptionMessage)));

            TestObserver<Pair<String,Float>> testObserver = rxVerID.identifyUsersInImage(mockImage).test();

            testObserver
                    .assertSubscribed()
                    .assertErrorMessage(exceptionMessage)
                    .assertTerminated();
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_identifyUsersInImage_identificationFails() {
        try {
            String exceptionMessage = "Test message";
            VerIDImage mockImage = mock(VerIDImage.class);
            VerID verID = mock(VerID.class);
            RxVerID rxVerID = mock(RxVerID.class);
            when(rxVerID.getVerID()).thenReturn(Single.just(verID));
            UserIdentification mockUserIdentification = mock(UserIdentification.class);
            when(mockUserIdentification.identifyUsersInFace(any())).thenThrow(new Exception(exceptionMessage));
            when(rxVerID.getUserIdentification(any(VerID.class))).thenReturn(Single.just(mockUserIdentification));
            when(rxVerID.identifyUsersInImage(any(VerIDImage.class))).thenCallRealMethod();
            when(rxVerID.identifyUsersInImage(any(VerID.class), any(VerIDImage.class))).thenCallRealMethod();
            doCallRealMethod().when(rxVerID).identifyUsersInFace(any(VerID.class), any());
            when(rxVerID.detectRecognizableFacesInImage(any(VerID.class), any(VerIDImage.class), anyInt())).thenReturn(Observable.just(mock(RecognizableFace.class)));

            TestObserver<Pair<String,Float>> testObserver = rxVerID.identifyUsersInImage(mockImage).test();

            testObserver
                    .assertSubscribed()
                    .assertErrorMessage(exceptionMessage)
                    .assertTerminated();
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_identifyUsersInImageUri_succeeds() {
        try {
            String testUserId = "testUserId";
            String testUserId2 = "testUserId2";
            float testScore = 4.6f;
            float testScore2 = 4.7f;
            RecognizableFace mockRecognizableFace = mock(RecognizableFace.class);
            Face mockFace = mock(Face.class);
            IFaceDetection mockFaceDetection = mock(IFaceDetection.class);
            doReturn(new Face[]{mockFace}).when(mockFaceDetection).detectFacesInImage(any(), anyInt(), anyInt());
            IFaceRecognition mockFaceRecognition = mock(IFaceRecognition.class);
            when(mockFaceRecognition.createRecognizableFacesFromFaces(any(), any())).thenReturn(new RecognizableFace[]{mockRecognizableFace});
            when(mockFaceRecognition.compareSubjectFacesToFaces(any(), any())).thenReturn(testScore, testScore2);
            when(mockFaceRecognition.getAuthenticationThreshold()).thenReturn(4.5f);
            IUserManagement mockUserManagement = mock(IUserManagement.class);
            when(mockUserManagement.getUsers()).thenReturn(new String[]{testUserId, testUserId2});
            when(mockUserManagement.getFacesOfUser(any())).thenReturn(new IRecognizable[]{mockRecognizableFace});
            VerID mockVerID = mock(VerID.class);
            when(mockVerID.getFaceDetection()).thenReturn(mockFaceDetection);
            when(mockVerID.getFaceRecognition()).thenReturn(mockFaceRecognition);
            when(mockVerID.getUserManagement()).thenReturn(mockUserManagement);
            VerIDImage mockImage = mock(VerIDImage.class);
            RxVerID rxVerID = mock(RxVerID.class);
            doReturn(Single.just(mockImage)).when(rxVerID).convertUriToVerIDImage(any());
            doReturn(Single.just(mockVerID)).when(rxVerID).getVerID();
            doCallRealMethod().when(rxVerID).getUserIdentification(any(VerID.class));
            doCallRealMethod().when(rxVerID).identifyUsersInImage(any(Uri.class));
            doCallRealMethod().when(rxVerID).identifyUsersInImage(any(VerIDImage.class));
            doCallRealMethod().when(rxVerID).identifyUsersInImage(any(VerID.class), any(VerIDImage.class));
            doCallRealMethod().when(rxVerID).identifyUsersInFace(any(VerID.class), any());
            doCallRealMethod().when(rxVerID).detectRecognizableFacesInImage(any(VerID.class), any(VerIDImage.class), anyInt());
            doCallRealMethod().when(rxVerID).convertFaceToRecognizableFace(any(VerID.class), any(), any());
            doCallRealMethod().when(rxVerID).detectFacesInImage(any(VerID.class), any(VerIDImage.class), anyInt());
            Pair<String, Float> score = new Pair<>(testUserId, testScore);
            Pair<String, Float> score2 = new Pair<>(testUserId2, testScore2);

            TestObserver<Pair<String,Float>> testObserver = rxVerID.identifyUsersInImage(mock(Uri.class)).test();

            testObserver
                    .assertSubscribed()
                    .assertNoErrors()
                    .assertValues(score2, score)
                    .assertComplete();

            verify(mockVerID, atLeastOnce()).getFaceDetection();
            verify(mockVerID, atLeastOnce()).getFaceRecognition();
            verify(mockVerID, atLeastOnce()).getUserManagement();
            verify(mockFaceDetection).detectFacesInImage(any(), anyInt(), anyInt());
            verify(mockFaceRecognition).createRecognizableFacesFromFaces(any(), any());
            verify(mockUserManagement).getUsers();
            verify(mockUserManagement).getFacesOfUser(eq(testUserId));
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_identifyUsersInBitmap_succeeds() {
        try {
            String testUserId = "testUserId";
            String testUserId2 = "testUserId2";
            float testScore = 4.6f;
            float testScore2 = 4.7f;
            RecognizableFace mockRecognizableFace = mock(RecognizableFace.class);
            Face mockFace = mock(Face.class);
            IFaceDetection mockFaceDetection = mock(IFaceDetection.class);
            when(mockFaceDetection.detectFacesInImage(any(), anyInt(), anyInt())).thenReturn(new Face[]{mockFace});
            IFaceRecognition mockFaceRecognition = mock(IFaceRecognition.class);
            when(mockFaceRecognition.createRecognizableFacesFromFaces(any(), any())).thenReturn(new RecognizableFace[]{mockRecognizableFace});
            when(mockFaceRecognition.compareSubjectFacesToFaces(any(), any())).thenReturn(testScore, testScore2);
            when(mockFaceRecognition.getAuthenticationThreshold()).thenReturn(4.5f);
            IUserManagement mockUserManagement = mock(IUserManagement.class);
            when(mockUserManagement.getUsers()).thenReturn(new String[]{testUserId, testUserId2});
            when(mockUserManagement.getFacesOfUser(any())).thenReturn(new IRecognizable[]{mockRecognizableFace});
            VerID mockVerID = mock(VerID.class);
            when(mockVerID.getFaceDetection()).thenReturn(mockFaceDetection);
            when(mockVerID.getFaceRecognition()).thenReturn(mockFaceRecognition);
            when(mockVerID.getUserManagement()).thenReturn(mockUserManagement);
            VerIDImage mockImage = mock(VerIDImage.class);
            RxVerID rxVerID = mock(RxVerID.class);
            doReturn(Single.just(mockImage)).when(rxVerID).convertBitmapToVerIDImage(any(), anyInt());
            doReturn(Single.just(mockVerID)).when(rxVerID).getVerID();
            doCallRealMethod().when(rxVerID).getUserIdentification(any(VerID.class));
            doCallRealMethod().when(rxVerID).identifyUsersInImage(any(Bitmap.class));
            doCallRealMethod().when(rxVerID).identifyUsersInImage(any(Bitmap.class), anyInt());
            doCallRealMethod().when(rxVerID).identifyUsersInImage(any(VerIDImage.class));
            doCallRealMethod().when(rxVerID).identifyUsersInImage(any(VerID.class), any(VerIDImage.class));
            doCallRealMethod().when(rxVerID).identifyUsersInFace(any(VerID.class), any());
            doCallRealMethod().when(rxVerID).detectRecognizableFacesInImage(any(VerID.class), any(VerIDImage.class), anyInt());
            doCallRealMethod().when(rxVerID).convertFaceToRecognizableFace(any(VerID.class), any(), any());
            doCallRealMethod().when(rxVerID).detectFacesInImage(any(VerID.class), any(VerIDImage.class), anyInt());
            Pair<String, Float> score = new Pair<>(testUserId, testScore);
            Pair<String, Float> score2 = new Pair<>(testUserId2, testScore2);

            TestObserver<Pair<String,Float>> testObserver = rxVerID.identifyUsersInImage(mock(Bitmap.class)).test();

            testObserver
                    .assertSubscribed()
                    .assertNoErrors()
                    .assertValues(score2, score)
                    .assertComplete();

            verify(mockVerID, atLeastOnce()).getFaceDetection();
            verify(mockVerID, atLeastOnce()).getFaceRecognition();
            verify(mockVerID, atLeastOnce()).getUserManagement();
            verify(mockFaceDetection).detectFacesInImage(any(), anyInt(), anyInt());
            verify(mockFaceRecognition).createRecognizableFacesFromFaces(any(), any());
            verify(mockUserManagement).getUsers();
            verify(mockUserManagement).getFacesOfUser(eq(testUserId));
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_cropImageUriToFace_succeeds() {
        try {
            Uri mockUri = mock(Uri.class);
            Face mockFace = mock(Face.class);
            Bitmap mockBitmap = mock(Bitmap.class);
            Matrix mockMatrix = mock(Matrix.class);
            when(mockMatrix.isIdentity()).thenReturn(true);
            RxVerID mockRxVerID = mock(RxVerID.class);
            when(mockRxVerID.cropImageToFace(any(Uri.class), any())).thenCallRealMethod();
            when(mockRxVerID.getExifOrientationOfImage(any(Uri.class))).thenReturn(Single.just(ExifInterface.ORIENTATION_NORMAL));
            when(mockRxVerID.convertUriToBitmap(any())).thenReturn(Single.just(mockBitmap));
            when(mockRxVerID.getTransformMatrixForExifOrientation(anyInt())).thenReturn(Single.just(mockMatrix));
            when(mockRxVerID.cropImageToFace(any(Bitmap.class), any())).thenReturn(Single.just(mockBitmap));

            TestObserver<Bitmap> testObserver = mockRxVerID.cropImageToFace(mockUri, mockFace).test();

            testObserver
                    .assertSubscribed()
                    .assertNoErrors()
                    .assertValue(mockBitmap)
                    .assertComplete();

            verify(mockRxVerID).cropImageToFace(any(Bitmap.class), any());
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_getExifOrientationOfImage_succeeds() {
        try {
            int orientation = ExifInterface.ORIENTATION_ROTATE_90;
            ExifInterface exifInterface = mock(ExifInterface.class);
            when(exifInterface.getAttributeInt(eq(ExifInterface.TAG_ORIENTATION), anyInt())).thenReturn(orientation);
            RxVerID mockRxVerID = mock(RxVerID.class);
            when(mockRxVerID.getExifFromUri(any())).thenReturn(Single.just(exifInterface));
            when(mockRxVerID.getExifOrientationOfImage(any())).thenCallRealMethod();

            TestObserver<Integer> testObserver = mockRxVerID.getExifOrientationOfImage(mock(Uri.class)).test();

            testObserver
                    .assertSubscribed()
                    .assertNoErrors()
                    .assertValue(orientation)
                    .assertComplete();

        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_convertUriToVerIDImage_succeeds() {
        try {
            RxVerID rxVerID = mock(RxVerID.class);
            Bitmap mockBitmap = mock(Bitmap.class);
            VerIDImage mockImage = mock(VerIDImage.class);
            when(rxVerID.convertUriToVerIDImage(any())).thenCallRealMethod();
            when(rxVerID.convertUriToBitmap(any())).thenReturn(Single.just(mockBitmap));
            when(rxVerID.getExifOrientationOfImage(any())).thenReturn(Single.just(ExifInterface.ORIENTATION_NORMAL));
            when(rxVerID.convertBitmapToVerIDImage(any(), anyInt())).thenReturn(Single.just(mockImage));

            TestObserver<VerIDImage> testObserver = rxVerID.convertUriToVerIDImage(mock(Uri.class)).test();

            testObserver
                    .assertSubscribed()
                    .assertNoErrors()
                    .assertValue(mockImage)
                    .assertComplete();

            verify(rxVerID).convertUriToBitmap(any());
            verify(rxVerID).getExifOrientationOfImage(any());
            verify(rxVerID).convertBitmapToVerIDImage(any(), anyInt());
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_convertBitmapToVerIDImage_succeeds() {
        try {
            Bitmap mockBitmap = mock(Bitmap.class);
            RxVerID rxVerID = mock(RxVerID.class);
            when(rxVerID.convertBitmapToVerIDImage(any(), anyInt())).thenCallRealMethod();

            TestObserver<VerIDImage> testObserver = rxVerID.convertBitmapToVerIDImage(mockBitmap, ExifInterface.ORIENTATION_NORMAL).test();

            testObserver
                    .assertSubscribed()
                    .assertNoErrors()
                    .assertComplete();
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_detectRecognizableFacesInBitmap_succeeds() {
        try {
            VerIDImage mockImage = mock(VerIDImage.class);
            RecognizableFace mockFace = mock(RecognizableFace.class);
            RxVerID rxVerID = mock(RxVerID.class);
            when(rxVerID.detectRecognizableFacesInImage(any(Bitmap.class), anyInt())).thenCallRealMethod();
            when(rxVerID.convertBitmapToVerIDImage(any(), anyInt())).thenReturn(Single.just(mockImage));
            when(rxVerID.detectRecognizableFacesInImage(any(VerIDImage.class), anyInt())).thenReturn(Observable.just(mockFace));

            TestObserver<RecognizableFace> testObserver = rxVerID.detectRecognizableFacesInImage(mock(Bitmap.class),1).test();

            testObserver
                    .assertSubscribed()
                    .assertNoErrors()
                    .assertValue(mockFace)
                    .assertComplete();
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_detectRecognizableFacesInBitmapWithOrientation_succeeds() {
        try {
            VerIDImage mockImage = mock(VerIDImage.class);
            RecognizableFace mockFace = mock(RecognizableFace.class);
            RxVerID rxVerID = mock(RxVerID.class);
            when(rxVerID.detectRecognizableFacesInImage(any(), anyInt(), anyInt())).thenCallRealMethod();
            when(rxVerID.convertBitmapToVerIDImage(any(), anyInt())).thenReturn(Single.just(mockImage));
            when(rxVerID.detectRecognizableFacesInImage(any(VerIDImage.class), anyInt())).thenReturn(Observable.just(mockFace));

            TestObserver<RecognizableFace> testObserver = rxVerID.detectRecognizableFacesInImage(mock(Bitmap.class), ExifInterface.ORIENTATION_NORMAL, 1).test();

            testObserver
                    .assertSubscribed()
                    .assertNoErrors()
                    .assertValue(mockFace)
                    .assertComplete();
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_convertUriToBitmap_succeeds() {
        try {
            InputStream mockInputStream = mock(InputStream.class);
            ContentResolver mockContentResolver = mock(ContentResolver.class);
            when(mockContentResolver.openInputStream(any())).thenReturn(mockInputStream);
            Context mockContext = mock(Context.class);
            when(mockContext.getContentResolver()).thenReturn(mockContentResolver);
            RxVerID rxVerID = mock(RxVerID.class);
            when(rxVerID.getContext()).thenReturn(mockContext);
            when(rxVerID.convertUriToBitmap(any())).thenCallRealMethod();
            Bitmap mockBitmap = mock(Bitmap.class);
            doReturn(mockBitmap).when(rxVerID).bitmapFromStream(any());

            TestObserver<Bitmap> testObserver = rxVerID.convertUriToBitmap(mock(Uri.class)).test();

            testObserver
                    .assertSubscribed()
                    .assertNoErrors()
                    .assertValue(mockBitmap)
                    .assertComplete();

            verify(rxVerID).bitmapFromStream(any());
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_convertUriToBitmap_fails() {
        try {
            ContentResolver mockContentResolver = mock(ContentResolver.class);
            when(mockContentResolver.openInputStream(any())).thenThrow(new FileNotFoundException());
            Context mockContext = mock(Context.class);
            when(mockContext.getContentResolver()).thenReturn(mockContentResolver);
            RxVerID rxVerID = mock(RxVerID.class);
            when(rxVerID.getContext()).thenReturn(mockContext);
            when(rxVerID.convertUriToBitmap(any())).thenCallRealMethod();

            TestObserver<Bitmap> testObserver = rxVerID.convertUriToBitmap(mock(Uri.class)).test();

            testObserver
                    .assertSubscribed()
                    .assertError(FileNotFoundException.class)
                    .assertTerminated();

        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_detectFacesInBitmap_succeeds() {
        try {
            Bitmap bitmap = mock(Bitmap.class);
            Face mockFace = mock(Face.class);
            VerIDImage mockImage = mock(VerIDImage.class);
            RxVerID rxVerID = mock(RxVerID.class);
            when(rxVerID.detectFacesInImage(any(Bitmap.class), anyInt())).thenCallRealMethod();
            when(rxVerID.detectFacesInImage(any(Bitmap.class), anyInt(), anyInt())).thenCallRealMethod();
            when(rxVerID.convertBitmapToVerIDImage(any(), anyInt())).thenReturn(Single.just(mockImage));
            when(rxVerID.detectFacesInImage(any(VerIDImage.class), anyInt())).thenReturn(Observable.just(mockFace));

            TestObserver<Face> testObserver = rxVerID.detectFacesInImage(bitmap, 1).test();

            testObserver
                    .assertSubscribed()
                    .assertNoErrors()
                    .assertValue(mockFace)
                    .assertComplete();

        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_getFaceDetectionFactory_returnsNull() {
        RxVerID rxVerID = mock(RxVerID.class);
        when(rxVerID.getFaceDetectionFactory()).thenCallRealMethod();

        IFaceDetectionFactory faceDetectionFactory = rxVerID.getFaceDetectionFactory();
        assertNull(faceDetectionFactory);
    }

    @Test
    public void test_getFaceRecognitionFactory_returnsNull() {
        RxVerID rxVerID = mock(RxVerID.class);
        when(rxVerID.getFaceRecognitionFactory()).thenCallRealMethod();

        IFaceRecognitionFactory faceRecognitionFactory = rxVerID.getFaceRecognitionFactory();
        assertNull(faceRecognitionFactory);
    }

    @Test
    public void test_getUserManagementFactory_returnsNull() {
        RxVerID rxVerID = mock(RxVerID.class);
        when(rxVerID.getUserManagementFactory()).thenCallRealMethod();

        IUserManagementFactory userManagementFactory = rxVerID.getUserManagementFactory();
        assertNull(userManagementFactory);
    }

    @Test
    public void test_createVerIDFactory() {
        Context mockContext = mock(Context.class);
        RxVerID rxVerID = mock(RxVerID.class);
        when(rxVerID.getContext()).thenReturn(mockContext);
        when(rxVerID.createVerIDFactory()).thenCallRealMethod();

        VerIDFactory verIDFactory = rxVerID.createVerIDFactory();
        assertNotNull(verIDFactory);
    }

    @Test
    public void test_correctBitmapOrientation_returnBitmap() {
        RxVerID rxVerID = mock(RxVerID.class);
        when(rxVerID.correctBitmapOrientation(any(), anyInt())).thenCallRealMethod();
        Matrix matrix = mock(Matrix.class);
        when(matrix.isIdentity()).thenReturn(true);
        when(rxVerID.getTransformMatrixForExifOrientation(anyInt())).thenReturn(Single.just(matrix));
        Bitmap bitmap = mock(Bitmap.class);

        TestObserver<Bitmap> testObserver = rxVerID.correctBitmapOrientation(bitmap, ExifInterface.ORIENTATION_NORMAL).test();

        testObserver
                .assertSubscribed()
                .assertNoErrors()
                .assertValue(bitmap)
                .assertComplete();

        verify(rxVerID).getTransformMatrixForExifOrientation(anyInt());
    }

    @Test
    public void test_compareFaceToFaces_succeeds() {
        try {
            float score = 4.5f;
            IFaceRecognition faceRecognition = mock(IFaceRecognition.class);
            when(faceRecognition.compareSubjectFacesToFaces(any(), any())).thenReturn(score);
            VerID verID = mock(VerID.class);
            when(verID.getFaceRecognition()).thenReturn(faceRecognition);
            RxVerID rxVerID = mock(RxVerID.class);
            when(rxVerID.compareFaceToFaces(any(), any())).thenCallRealMethod();
            when(rxVerID.compareFaceToFaces(any(), any(), any())).thenCallRealMethod();
            when(rxVerID.getVerID()).thenReturn(Single.just(verID));

            TestObserver<Float> testObserver = rxVerID.compareFaceToFaces(mock(IRecognizable.class), new RecognizableFace[]{mock(RecognizableFace.class)}).test();

            testObserver
                    .assertSubscribed()
                    .assertNoErrors()
                    .assertValue(score)
                    .assertComplete();
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_compareFaceToFaces_comparisonFails() {
        try {
            String errorMessage = "Test error message";
            IFaceRecognition faceRecognition = mock(IFaceRecognition.class);
            when(faceRecognition.compareSubjectFacesToFaces(any(), any())).thenThrow(new Exception(errorMessage));
            VerID verID = mock(VerID.class);
            when(verID.getFaceRecognition()).thenReturn(faceRecognition);
            RxVerID rxVerID = mock(RxVerID.class);
            when(rxVerID.compareFaceToFaces(any(), any())).thenCallRealMethod();
            when(rxVerID.compareFaceToFaces(any(), any(), any())).thenCallRealMethod();
            when(rxVerID.getVerID()).thenReturn(Single.just(verID));

            TestObserver<Float> testObserver = rxVerID.compareFaceToFaces(mock(IRecognizable.class), new RecognizableFace[]{mock(RecognizableFace.class)}).test();

            testObserver
                    .assertSubscribed()
                    .assertErrorMessage(errorMessage)
                    .assertTerminated();
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_assignFacesToUser_succeeds() {
        try {
            IUserManagement userManagement = mock(IUserManagement.class);
            VerID verID = mock(VerID.class);
            when(verID.getUserManagement()).thenReturn(userManagement);
            RxVerID rxVerID = mock(RxVerID.class);
            when(rxVerID.assignFacesToUser(any(), any())).thenCallRealMethod();
            when(rxVerID.assignFacesToUser(any(), any(), any())).thenCallRealMethod();
            when(rxVerID.getVerID()).thenReturn(Single.just(verID));

            TestObserver testObserver = rxVerID.assignFacesToUser(new IRecognizable[]{mock(IRecognizable.class)}, "test").test();

            testObserver
                    .assertSubscribed()
                    .assertNoErrors()
                    .assertComplete();

            verify(userManagement).assignFacesToUser(any(), any());
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_assignFacesToUser_fails() {
        try {
            String errorMessage = "Test error message";
            IUserManagement userManagement = mock(IUserManagement.class);
            doThrow(new Exception(errorMessage)).when(userManagement).assignFacesToUser(any(), anyString());
            VerID verID = mock(VerID.class);
            when(verID.getUserManagement()).thenReturn(userManagement);
            RxVerID rxVerID = mock(RxVerID.class);
            when(rxVerID.assignFacesToUser(any(), any())).thenCallRealMethod();
            when(rxVerID.assignFacesToUser(any(), any(), any())).thenCallRealMethod();
            when(rxVerID.getVerID()).thenReturn(Single.just(verID));

            TestObserver testObserver = rxVerID.assignFacesToUser(new IRecognizable[]{mock(IRecognizable.class)}, "test").test();

            testObserver
                    .assertSubscribed()
                    .assertErrorMessage(errorMessage)
                    .assertTerminated();

            verify(userManagement).assignFacesToUser(any(), any());
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_assignFaceToUser_succeeds() {
        try {
            IUserManagement userManagement = mock(IUserManagement.class);
            VerID verID = mock(VerID.class);
            when(verID.getUserManagement()).thenReturn(userManagement);
            RxVerID rxVerID = mock(RxVerID.class);
            when(rxVerID.assignFaceToUser(any(), any())).thenCallRealMethod();
            when(rxVerID.assignFaceToUser(any(), any(), any())).thenCallRealMethod();
            when(rxVerID.getVerID()).thenReturn(Single.just(verID));

            TestObserver testObserver = rxVerID.assignFaceToUser(mock(IRecognizable.class), "test").test();

            testObserver
                    .assertSubscribed()
                    .assertNoErrors()
                    .assertComplete();

            verify(userManagement).assignFacesToUser(any(), any());
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_assignFaceToUser_fails() {
        try {
            String errorMessage = "Test error message";
            IUserManagement userManagement = mock(IUserManagement.class);
            doThrow(new Exception(errorMessage)).when(userManagement).assignFacesToUser(any(), anyString());
            VerID verID = mock(VerID.class);
            when(verID.getUserManagement()).thenReturn(userManagement);
            RxVerID rxVerID = mock(RxVerID.class);
            when(rxVerID.assignFaceToUser(any(), any())).thenCallRealMethod();
            when(rxVerID.assignFaceToUser(any(), any(), any())).thenCallRealMethod();
            when(rxVerID.getVerID()).thenReturn(Single.just(verID));

            TestObserver testObserver = rxVerID.assignFaceToUser(mock(IRecognizable.class), "test").test();

            testObserver
                    .assertSubscribed()
                    .assertErrorMessage(errorMessage)
                    .assertTerminated();

            verify(userManagement).assignFacesToUser(any(), any());
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_deleteUser_succeeds() {
        try {
            IUserManagement userManagement = mock(IUserManagement.class);
            VerID verID = mock(VerID.class);
            when(verID.getUserManagement()).thenReturn(userManagement);
            RxVerID rxVerID = mock(RxVerID.class);
            when(rxVerID.deleteUser(anyString())).thenCallRealMethod();
            when(rxVerID.deleteUser(any(), anyString())).thenCallRealMethod();
            when(rxVerID.getVerID()).thenReturn(Single.just(verID));

            TestObserver testObserver = rxVerID.deleteUser("test").test();

            testObserver
                    .assertSubscribed()
                    .assertNoErrors()
                    .assertComplete();

            verify(userManagement).deleteUsers(any());
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_deleteUser_fails() {
        try {
            String errorMessage = "Test error message";
            IUserManagement userManagement = mock(IUserManagement.class);
            doThrow(new Exception(errorMessage)).when(userManagement).deleteUsers(any());
            VerID verID = mock(VerID.class);
            when(verID.getUserManagement()).thenReturn(userManagement);
            RxVerID rxVerID = mock(RxVerID.class);
            when(rxVerID.deleteUser(anyString())).thenCallRealMethod();
            when(rxVerID.deleteUser(any(), anyString())).thenCallRealMethod();
            when(rxVerID.getVerID()).thenReturn(Single.just(verID));

            TestObserver testObserver = rxVerID.deleteUser("test").test();

            testObserver
                    .assertSubscribed()
                    .assertErrorMessage(errorMessage)
                    .assertTerminated();

            verify(userManagement).deleteUsers(any());
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_getUsers_succeeds() {
        try {
            String user = "test";
            IUserManagement userManagement = mock(IUserManagement.class);
            when(userManagement.getUsers()).thenReturn(new String[]{user});
            VerID verID = mock(VerID.class);
            when(verID.getUserManagement()).thenReturn(userManagement);
            RxVerID rxVerID = mock(RxVerID.class);
            when(rxVerID.getUsers()).thenCallRealMethod();
            when(rxVerID.getUsers(any())).thenCallRealMethod();
            when(rxVerID.getVerID()).thenReturn(Single.just(verID));

            TestObserver<String> testObserver = rxVerID.getUsers().test();

            testObserver
                    .assertSubscribed()
                    .assertNoErrors()
                    .assertValue(user)
                    .assertComplete();

            verify(userManagement).getUsers();
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_getUsers_fails() {
        try {
            String errorMessage = "Test error message";
            IUserManagement userManagement = mock(IUserManagement.class);
            when(userManagement.getUsers()).thenThrow(new Exception(errorMessage));
            VerID verID = mock(VerID.class);
            when(verID.getUserManagement()).thenReturn(userManagement);
            RxVerID rxVerID = mock(RxVerID.class);
            when(rxVerID.getUsers()).thenCallRealMethod();
            when(rxVerID.getUsers(any())).thenCallRealMethod();
            when(rxVerID.getVerID()).thenReturn(Single.just(verID));

            TestObserver<String> testObserver = rxVerID.getUsers().test();

            testObserver
                    .assertSubscribed()
                    .assertErrorMessage(errorMessage)
                    .assertTerminated();

            verify(userManagement).getUsers();
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_getFacesOfUser_returnsFace() {
        try {
            String user = "test";
            IRecognizable face = mock(IRecognizable.class);
            IUserManagement userManagement = mock(IUserManagement.class);
            when(userManagement.getFacesOfUser(anyString())).thenReturn(new IRecognizable[]{face});
            VerID verID = mock(VerID.class);
            when(verID.getUserManagement()).thenReturn(userManagement);
            RxVerID rxVerID = mock(RxVerID.class);
            when(rxVerID.getFacesOfUser(anyString())).thenCallRealMethod();
            when(rxVerID.getFacesOfUser(any(), anyString())).thenCallRealMethod();
            when(rxVerID.getVerID()).thenReturn(Single.just(verID));

            TestObserver<IRecognizable> testObserver = rxVerID.getFacesOfUser(user).test();

            testObserver
                    .assertSubscribed()
                    .assertNoErrors()
                    .assertValue(face)
                    .assertComplete();

            verify(userManagement).getFacesOfUser(eq(user));
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_getFacesOfUser_fails() {
        try {
            String user = "test";
            String errorMessage = "Test error message";
            IUserManagement userManagement = mock(IUserManagement.class);
            when(userManagement.getFacesOfUser(anyString())).thenThrow(new Exception(errorMessage));
            VerID verID = mock(VerID.class);
            when(verID.getUserManagement()).thenReturn(userManagement);
            RxVerID rxVerID = mock(RxVerID.class);
            when(rxVerID.getFacesOfUser(anyString())).thenCallRealMethod();
            when(rxVerID.getFacesOfUser(any(), anyString())).thenCallRealMethod();
            when(rxVerID.getVerID()).thenReturn(Single.just(verID));

            TestObserver<IRecognizable> testObserver = rxVerID.getFacesOfUser(user).test();

            testObserver
                    .assertSubscribed()
                    .assertErrorMessage(errorMessage)
                    .assertTerminated();

            verify(userManagement).getFacesOfUser(eq(user));
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_authenticateUserInFaces_succeeds() {
        try {
            String user = "test";
            float score = 4.5f;
            float threshold = 4.0f;
            IRecognizable face = mock(IRecognizable.class);
            IUserManagement userManagement = mock(IUserManagement.class);
            when(userManagement.getFacesOfUser(anyString())).thenReturn(new IRecognizable[]{face});
            IFaceRecognition faceRecognition = mock(IFaceRecognition.class);
            when(faceRecognition.compareSubjectFacesToFaces(any(), any())).thenReturn(score);
            when(faceRecognition.getAuthenticationThreshold()).thenReturn(threshold);
            VerID verID = mock(VerID.class);
            when(verID.getUserManagement()).thenReturn(userManagement);
            when(verID.getFaceRecognition()).thenReturn(faceRecognition);
            RxVerID rxVerID = mock(RxVerID.class);
            when(rxVerID.authenticateUserInFaces(anyString(), any())).thenCallRealMethod();
            when(rxVerID.authenticateUserInFaces(any(), anyString(), any())).thenCallRealMethod();
            when(rxVerID.getVerID()).thenReturn(Single.just(verID));

            TestObserver<Boolean> testObserver = rxVerID.authenticateUserInFaces(user, new RecognizableFace[]{mock(RecognizableFace.class)}).test();

            testObserver
                    .assertSubscribed()
                    .assertNoErrors()
                    .assertValue(true)
                    .assertComplete();

            verify(userManagement).getFacesOfUser(eq(user));
            verify(faceRecognition).getAuthenticationThreshold();
            verify(faceRecognition).compareSubjectFacesToFaces(any(), any());
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_authenticateUserInFaces_fails() {
        try {
            String user = "test";
            String errorMessage = "Test error message";
            IRecognizable face = mock(IRecognizable.class);
            IUserManagement userManagement = mock(IUserManagement.class);
            when(userManagement.getFacesOfUser(anyString())).thenReturn(new IRecognizable[]{face});
            IFaceRecognition faceRecognition = mock(IFaceRecognition.class);
            when(faceRecognition.compareSubjectFacesToFaces(any(), any())).thenThrow(new Exception(errorMessage));
            VerID verID = mock(VerID.class);
            when(verID.getUserManagement()).thenReturn(userManagement);
            when(verID.getFaceRecognition()).thenReturn(faceRecognition);
            RxVerID rxVerID = mock(RxVerID.class);
            when(rxVerID.authenticateUserInFaces(anyString(), any())).thenCallRealMethod();
            when(rxVerID.authenticateUserInFaces(any(), anyString(), any())).thenCallRealMethod();
            when(rxVerID.getVerID()).thenReturn(Single.just(verID));

            TestObserver<Boolean> testObserver = rxVerID.authenticateUserInFaces(user, new RecognizableFace[]{mock(RecognizableFace.class)}).test();

            testObserver
                    .assertSubscribed()
                    .assertErrorMessage(errorMessage)
                    .assertTerminated();

            verify(userManagement).getFacesOfUser(eq(user));
            verify(faceRecognition).compareSubjectFacesToFaces(any(), any());
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_getSessionResultFromIntent_succeeds() {
        RxVerID rxVerID = mock(RxVerID.class);
        when(rxVerID.getSessionResultFromIntent(any())).thenCallRealMethod();
        VerIDSessionResult result = mock(VerIDSessionResult.class);
        when(result.getError()).thenReturn(null);
        Intent intent = mock(Intent.class);
        when(intent.getParcelableExtra(eq("com.appliedrec.verid.ui.EXTRA_RESULT"))).thenReturn(result);

        TestObserver<VerIDSessionResult> testObserver = rxVerID.getSessionResultFromIntent(intent).test();

        testObserver
                .assertSubscribed()
                .assertNoErrors()
                .assertValue(result)
                .assertComplete();
    }

    @Test
    public void test_getSessionResultFromNullIntent_fails() {
        RxVerID rxVerID = mock(RxVerID.class);
        when(rxVerID.getSessionResultFromIntent(any())).thenCallRealMethod();

        TestObserver<VerIDSessionResult> testObserver = rxVerID.getSessionResultFromIntent(null).test();

        testObserver
                .assertSubscribed()
                .assertError(NullPointerException.class)
                .assertTerminated();
    }

    @Test
    public void test_getSessionResultFromIntentWithoutResult_fails() {
        String errorMessage = "Failed to parse Ver-ID session result from intent";
        RxVerID rxVerID = mock(RxVerID.class);
        when(rxVerID.getSessionResultFromIntent(any())).thenCallRealMethod();
        Intent intent = mock(Intent.class);
        when(intent.getParcelableExtra(eq("com.appliedrec.verid.ui.EXTRA_RESULT"))).thenReturn(null);

        TestObserver<VerIDSessionResult> testObserver = rxVerID.getSessionResultFromIntent(intent).test();

        testObserver
                .assertSubscribed()
                .assertErrorMessage(errorMessage)
                .assertTerminated();
    }

    @Test
    public void test_getFailedSessionResultFromIntent_fails() {
        RxVerID rxVerID = mock(RxVerID.class);
        when(rxVerID.getSessionResultFromIntent(any())).thenCallRealMethod();
        Exception exception = mock(Exception.class);
        VerIDSessionResult result = mock(VerIDSessionResult.class);
        when(result.getError()).thenReturn(exception);
        Intent intent = mock(Intent.class);
        when(intent.getParcelableExtra(eq("com.appliedrec.verid.ui.EXTRA_RESULT"))).thenReturn(result);

        TestObserver<VerIDSessionResult> testObserver = rxVerID.getSessionResultFromIntent(intent).test();

        testObserver
                .assertSubscribed()
                .assertError(exception)
                .assertTerminated();
    }

    @Test
    public void test_getImageUriFaceAndBearingFromSessionResult_returnsImageUriFaceAndBearing() {
        RxVerID rxVerID = mock(RxVerID.class);
        when(rxVerID.getImageUriFaceAndBearingFromSessionResult(any())).thenCallRealMethod();
        Uri uri = mock(Uri.class);
        Face face = mock(Face.class);
        DetectedFace detectedFace = mock(DetectedFace.class);
        when(detectedFace.getBearing()).thenReturn(Bearing.STRAIGHT);
        when(detectedFace.getImageUri()).thenReturn(uri);
        when(detectedFace.getFace()).thenReturn(face);
        VerIDSessionResult result = mock(VerIDSessionResult.class);
        when(result.getAttachments()).thenReturn(new DetectedFace[]{detectedFace});
        when(result.getError()).thenReturn(null);
        Triplet<Uri, Face, Bearing> value = new Triplet<>(uri, face, Bearing.STRAIGHT);

        TestObserver<Triplet<Uri, Face, Bearing>> testObserver = rxVerID.getImageUriFaceAndBearingFromSessionResult(result).test();

        testObserver
                .assertSubscribed()
                .assertNoErrors()
                .assertValue(value)
                .assertComplete();
    }

    @Test
    public void test_getImageUriFaceAndBearingFromFailedSessionResult_fails() {
        RxVerID rxVerID = mock(RxVerID.class);
        when(rxVerID.getImageUriFaceAndBearingFromSessionResult(any())).thenCallRealMethod();
        Exception exception = mock(Exception.class);
        VerIDSessionResult result = mock(VerIDSessionResult.class);
        when(result.getError()).thenReturn(exception);

        TestObserver<Triplet<Uri, Face, Bearing>> testObserver = rxVerID.getImageUriFaceAndBearingFromSessionResult(result).test();

        testObserver
                .assertSubscribed()
                .assertError(exception)
                .assertTerminated();
    }

    @Test
    public void test_getFacesAndImageUrisFromSessionResult_returnsFacesAndImageUris() {
        RxVerID rxVerID = mock(RxVerID.class);
        when(rxVerID.getFacesAndImageUrisFromSessionResult(any())).thenCallRealMethod();
        when(rxVerID.getFacesAndImageUrisFromSessionResult(any(), any())).thenCallRealMethod();
        Uri uri = mock(Uri.class);
        Face face = mock(Face.class);
        DetectedFace detectedFace = mock(DetectedFace.class);
        when(detectedFace.getImageUri()).thenReturn(uri);
        when(detectedFace.getFace()).thenReturn(face);
        VerIDSessionResult result = mock(VerIDSessionResult.class);
        when(result.getAttachments()).thenReturn(new DetectedFace[]{detectedFace});
        when(result.getError()).thenReturn(null);

        TestObserver<DetectedFace> testObserver = rxVerID.getFacesAndImageUrisFromSessionResult(result).test();

        testObserver
                .assertSubscribed()
                .assertNoErrors()
                .assertValue(detectedFace)
                .assertComplete();
    }

    @Test
    public void test_getFacesAndImageUrisFromFailedSessionResult_fails() {
        RxVerID rxVerID = mock(RxVerID.class);
        when(rxVerID.getFacesAndImageUrisFromSessionResult(any())).thenCallRealMethod();
        when(rxVerID.getFacesAndImageUrisFromSessionResult(any(), any())).thenCallRealMethod();
        Exception exception = mock(Exception.class);
        VerIDSessionResult result = mock(VerIDSessionResult.class);
        when(result.getError()).thenReturn(exception);

        TestObserver<DetectedFace> testObserver = rxVerID.getFacesAndImageUrisFromSessionResult(result).test();

        testObserver
                .assertSubscribed()
                .assertError(exception)
                .assertTerminated();
    }

    @Test
    public void test_getRecognizableFacesFromSessionResult_returnsFace() {
        RxVerID rxVerID = mock(RxVerID.class);
        when(rxVerID.getRecognizableFacesFromSessionResult(any())).thenCallRealMethod();
        when(rxVerID.getRecognizableFacesFromSessionResult(any(), any())).thenCallRealMethod();
        Uri uri = mock(Uri.class);
        RecognizableFace face = mock(RecognizableFace.class);
        DetectedFace detectedFace = mock(DetectedFace.class);
        when(detectedFace.getFace()).thenReturn(face);
        VerIDSessionResult result = mock(VerIDSessionResult.class);
        when(result.getAttachments()).thenReturn(new DetectedFace[]{detectedFace});
        when(result.getError()).thenReturn(null);

        TestObserver<RecognizableFace> testObserver = rxVerID.getRecognizableFacesFromSessionResult(result).test();

        testObserver
                .assertSubscribed()
                .assertNoErrors()
                .assertValue(face)
                .assertComplete();
    }

    @Test
    public void test_getRecognizableFacesFromFailedSessionResult_fails() {
        RxVerID rxVerID = mock(RxVerID.class);
        when(rxVerID.getRecognizableFacesFromSessionResult(any())).thenCallRealMethod();
        when(rxVerID.getRecognizableFacesFromSessionResult(any(), any())).thenCallRealMethod();
        Exception exception = mock(Exception.class);
        VerIDSessionResult result = mock(VerIDSessionResult.class);
        when(result.getError()).thenReturn(exception);

        TestObserver<RecognizableFace> testObserver = rxVerID.getRecognizableFacesFromSessionResult(result).test();

        testObserver
                .assertSubscribed()
                .assertError(exception)
                .assertTerminated();
    }

    @Test
    public void test_getImageUrisFromSessionResult_returnsUri() {
        RxVerID rxVerID = mock(RxVerID.class);
        when(rxVerID.getImageUrisFromSessionResult(any())).thenCallRealMethod();
        when(rxVerID.getImageUrisFromSessionResult(any(), any())).thenCallRealMethod();
        Uri uri = mock(Uri.class);
        RecognizableFace face = mock(RecognizableFace.class);
        DetectedFace detectedFace = mock(DetectedFace.class);
        when(detectedFace.getImageUri()).thenReturn(uri);
        VerIDSessionResult result = mock(VerIDSessionResult.class);
        when(result.getAttachments()).thenReturn(new DetectedFace[]{detectedFace});
        when(result.getError()).thenReturn(null);

        TestObserver<Uri> testObserver = rxVerID.getImageUrisFromSessionResult(result).test();

        testObserver
                .assertSubscribed()
                .assertNoErrors()
                .assertValue(uri)
                .assertComplete();
    }

    @Test
    public void test_getImageUrisFromFailedSessionResult_fails() {
        RxVerID rxVerID = mock(RxVerID.class);
        when(rxVerID.getImageUrisFromSessionResult(any())).thenCallRealMethod();
        when(rxVerID.getImageUrisFromSessionResult(any(), any())).thenCallRealMethod();
        Exception exception = mock(Exception.class);
        VerIDSessionResult result = mock(VerIDSessionResult.class);
        when(result.getError()).thenReturn(exception);

        TestObserver<Uri> testObserver = rxVerID.getImageUrisFromSessionResult(result).test();

        testObserver
                .assertSubscribed()
                .assertError(exception)
                .assertTerminated();
    }
}