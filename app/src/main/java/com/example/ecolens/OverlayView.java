package com.example.ecolens;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import org.tensorflow.lite.task.vision.detector.Detection;
import java.util.ArrayList;
import java.util.List;

public class OverlayView extends View {
    private List<Detection> results = new ArrayList<>();
    private Paint boxPaint = new Paint();
    private Paint textPaint = new Paint();
    private float scaleFactorX = 1.0f;
    private float scaleFactorY = 1.0f;

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaints();
    }

    private void initPaints() {
        boxPaint.setColor(Color.GREEN);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(8f);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(50f);
        textPaint.setStyle(Paint.Style.FILL);
    }

    public void setResults(List<Detection> detectionResults, int imageHeight, int imageWidth) {
        this.results = detectionResults;
        // calculate how much to scale the box to fit the screen
        scaleFactorX = (float) getWidth() / imageWidth;
        scaleFactorY = (float) getHeight() / imageHeight;
        invalidate(); // redraw
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        for (Detection result : results) {
            RectF box = result.getBoundingBox();

            // scale the coordinates to the phone screen size
            float top = box.top * scaleFactorY;
            float bottom = box.bottom * scaleFactorY;
            float left = box.left * scaleFactorX;
            float right = box.right * scaleFactorX;

            // draw Box
            RectF drawableRect = new RectF(left, top, right, bottom);
            canvas.drawRect(drawableRect, boxPaint);

            // draw Text (label and confidence)
            String label = result.getCategories().get(0).getLabel();
            int confidence = (int)(result.getCategories().get(0).getScore() * 100);
            canvas.drawText(label + " " + confidence + "%", left, top - 10, textPaint);
        }
    }
}
