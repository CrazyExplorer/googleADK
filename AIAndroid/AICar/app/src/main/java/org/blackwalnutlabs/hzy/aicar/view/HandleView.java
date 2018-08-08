package org.blackwalnutlabs.hzy.aicar.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import org.blackwalnutlabs.hzy.aicar.R;

public class HandleView  extends View {

    //固定摇杆背景圆形的X,Y坐标以及半径
    private float mRockerBg_X;
    private float mRockerBg_Y;
    private float mRockerBg_R;
    //摇杆的X,Y坐标以及摇杆的半径
    private float mRockerBtn_X;
    private float mRockerBtn_Y;
    private float mRockerBtn_R;
    private Bitmap mBmpRockerBg;
    private Bitmap mBmpRockerBtn;
    private PointF mCenterPoint;
    private int lowerBound;
    private int upperBound;

    public int angleResult= (Integer.MAX_VALUE);

    public HandleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub

        // 获取bitmap
        mBmpRockerBg = BitmapFactory.decodeResource(context.getResources(), R.drawable.rocker_bg_gray);
        mBmpRockerBtn = BitmapFactory.decodeResource(context.getResources(), R.drawable.rocker_btn_blc);

        // 调用该方法时可以获取view实际的宽getWidth()和高getHeight()
        getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                getViewTreeObserver().removeOnPreDrawListener(this);
                mCenterPoint = new PointF(getWidth() / 2, getHeight() / 2);
                mRockerBg_X = mCenterPoint.x;
                mRockerBg_Y = mCenterPoint.y;
                mRockerBtn_X = mCenterPoint.x;
                mRockerBtn_Y = mCenterPoint.y;
                float tmp_f = mBmpRockerBg.getWidth() / (float)(mBmpRockerBg.getWidth() + mBmpRockerBtn.getWidth());
                mRockerBg_R = tmp_f * getWidth() / 2;
                mRockerBtn_R = (1.0f - tmp_f)* getWidth() / 2;
                return true;
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {

                while(true){
                    //系统调用onDraw方法刷新画面
                    HandleView.this.postInvalidate();
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {

                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void map(float x,float y){//映射
        int angle;
        int perRegion=0;
        perRegion=upperBound-lowerBound;
        if(perRegion<=0)
        {
            upperBound=100;
            lowerBound=2;
        }
        if(x<=0){
            angle= (int) Math.toDegrees(Math.atan(y / x))+90+180;
        }
        else{
            angle=  (int)Math.toDegrees(Math.atan(y / x))+90;
        }
        angleResult=(int)((angle*perRegion/360)+lowerBound);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // TODO Auto-generated method stub
        super.onDraw(canvas);
        canvas.drawBitmap(mBmpRockerBg, null,
                new Rect((int)(mRockerBg_X - mRockerBg_R),
                        (int)(mRockerBg_Y - mRockerBg_R),
                        (int)(mRockerBg_X + mRockerBg_R),
                        (int)(mRockerBg_Y + mRockerBg_R)),
                null);
        canvas.drawBitmap(mBmpRockerBtn, null,
                new Rect((int)(mRockerBtn_X - mRockerBtn_R),
                        (int)(mRockerBtn_Y - mRockerBtn_R),
                        (int)(mRockerBtn_X + mRockerBtn_R),
                        (int)(mRockerBtn_Y + mRockerBtn_R)),
                null);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            // 当触屏区域不在活动范围内
            if (Math.sqrt(Math.pow((mRockerBg_X - (int) event.getX()), 2) + Math.pow((mRockerBg_Y - (int) event.getY()), 2)) >= mRockerBg_R) {
                //得到摇杆与触屏点所形成的角度
                double tempRad = getRad(mRockerBg_X, mRockerBg_Y, event.getX(), event.getY());
                //保证内部小圆运动的长度限制
                getXY(mRockerBg_X, mRockerBg_Y, mRockerBg_R, tempRad);
            } else {//如果小球中心点小于活动区域则随着用户触屏点移动即可
                double tempRad = getRad(mRockerBg_X, mRockerBg_Y, event.getX(), event.getY());
                getXY(mRockerBg_X, mRockerBg_Y, mRockerBg_R, tempRad);
            }
            if(mRockerChangeListener != null) {
                map(mRockerBtn_X - mCenterPoint.x, mRockerBtn_Y - mCenterPoint.y);
                mRockerChangeListener.report(angleResult);
            }
        }
        else if (event.getAction() == MotionEvent.ACTION_UP)
        {
            mRockerBtn_X = mCenterPoint.x;
            mRockerBtn_Y = mCenterPoint.y;
            angleResult=-777;
            if(mRockerChangeListener != null) {
                mRockerChangeListener.report(angleResult);
            }
        }
        return true;
    }

    /***
     * 得到两点之间的弧度
     */
    public double getRad(float px1, float py1, float px2, float py2) {
        //得到两点X的距离
        float x = px2 - px1;
        //得到两点Y的距离
        float y = py1 - py2;
        //算出斜边长
        float xie = (float) Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
        //得到这个角度的余弦值（通过三角函数中的定理 ：邻边/斜边=角度余弦值）
        float cosAngle = x / xie;
        //通过反余弦定理获取到其角度的弧度
        float rad = (float) Math.acos(cosAngle);
        //注意：当触屏的位置Y坐标<摇杆的Y坐标我们要取反值-0~-180
        if (py2 < py1) {
            rad = -rad;
        }
        return rad;
    }

    public void getXY(float centerX, float centerY, float R, double rad) {
        //获取圆周运动的X坐标
        mRockerBtn_X = (float) (R * Math.cos(rad)) + centerX;
        //获取圆周运动的Y坐标
        mRockerBtn_Y = (float) (R * Math.sin(rad)) + centerY;
    }


    public void setLowerBound(int lowerBound) {
        this.lowerBound = lowerBound;
    }
    public void setUpperBound(int upperBound) {
        this.upperBound = upperBound;
    }

    RockerChangeListener mRockerChangeListener = null;
    public void setRockerChangeListener(RockerChangeListener rockerChangeListener) {
        mRockerChangeListener = rockerChangeListener;
    }

    public interface RockerChangeListener {
        public void report(int value);
    }
}