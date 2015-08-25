package com.example.matteo.smiley;

import android.content.Context;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Matteo on 19/08/2015.
 */
public class SmileyCreator {
    static{ System.loadLibrary("opencv_java"); }

    private static String FACE_DETECTOR = "/haarcascade_frontalface_alt.xml";
    private static String EYE_DETECTOR = "/haarcascade_eye.xml";
    private static String MOUTH_DETECTOR = "/haarcascade_eye.xml";
    private static String NOSE_DETECTOR = "/haarcascade_eye.xml";
    private static String GENERATED_TMP_SMILEY = "generated_tmp";

    String iFP;
    Mat inputImage;
    Context mContext;

    CascadeClassifier faceDetector;
    CascadeClassifier eyesDetector;
    CascadeClassifier mouthDetector;
    CascadeClassifier noseDetector;

    MatOfRect faceDetection;
    MatOfRect eyesDetection;
    MatOfRect mouthDetection;
    MatOfRect noseDetection;


    public SmileyCreator (){}


    public SmileyCreator (Context context,File input){

        mContext = context;
        iFP = input.getAbsolutePath();

        loadCascadeClassifiers();




        faceDetection = new MatOfRect();
        eyesDetection = new MatOfRect();
        mouthDetection = new MatOfRect();
        noseDetection = new MatOfRect();


        inputImage = Imgcodecs.imread(iFP);









    }

    public void elaborate(){

        faceDetector.detectMultiScale(inputImage, faceDetection);

        System.out.println(String.format("Detected %s faces", faceDetection.toArray().length));

        // Draw a bounding box around each face.
        for (Rect rect : faceDetection.toArray()) {
            Imgproc.rectangle(inputImage, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 0));
        }


    }

    public File getSmileY(){


        return new File("tmp");

    }

    private void loadCascadeClassifiers(){

        /* Copy the resource into a temp file so OpenCV can load it */

        InputStream isF = mContext.getResources().openRawResource(R.raw.haarcascade_profileface);
        InputStream isE = mContext.getResources().openRawResource(R.raw.haarcascade_eye);
        InputStream isM = mContext.getResources().openRawResource(R.raw.Mouth);
        InputStream isN = mContext.getResources().openRawResource(R.raw.Nariz);


        File cascadeDir = mContext.getDir("cascade", Context.MODE_PRIVATE);
        File mCascadeFileF = new File(cascadeDir, FACE_DETECTOR);
        File mCascadeFileE = new File(cascadeDir, EYE_DETECTOR);
        File mCascadeFileM = new File(cascadeDir, MOUTH_DETECTOR);
        File mCascadeFileN = new File(cascadeDir, NOSE_DETECTOR);
        try {


            FileOutputStream osF = new FileOutputStream(mCascadeFileF);
            FileOutputStream osE = new FileOutputStream(mCascadeFileE);
            FileOutputStream osM = new FileOutputStream(mCascadeFileM);
            FileOutputStream osN = new FileOutputStream(mCascadeFileN);



            osF.close();
            osE.close();
            osM.close();
            osN.close();


            isF.close();
            isE.close();
            isM.close();
            isN.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }




        faceDetector = new CascadeClassifier();
        faceDetector.load(mCascadeFileF.getAbsolutePath());

        eyesDetector = new CascadeClassifier();
        eyesDetector.load(mCascadeFileE.getAbsolutePath());

        mouthDetector = new CascadeClassifier();
        mouthDetector.load(mCascadeFileM.getAbsolutePath());

        noseDetector = new CascadeClassifier();
        noseDetector.load(mCascadeFileN.getAbsolutePath());


    }

    /*
    private void crop(Mat srcImg){


        Mat crop=new Mat(); crop=srcImg.submat(y,height,x,width);
        Imgcodecs.imwrite(GENERATED_TMP_SMILEY, crop);
    }
    */
}
