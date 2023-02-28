package com.marktreble.f3ftimer.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/*
 * WindVane
 */
public class WindVane extends View {
    private final String logTag = "WindVane";

    private final RectF rect = new RectF(0f, 0f, 0f, 0f);

    private final Paint bgPaint = new Paint();
    private final Paint fgPaint = new Paint();

    private Double currentAngle;

    Path path = new Path();

    public WindVane(Context context) {
        super(context);
        init(context);
    }

    public WindVane(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        bgPaint.setColor(Color.LTGRAY);
        bgPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        fgPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        fgPaint.setStrokeCap(Paint.Cap.ROUND);
        fgPaint.setStrokeWidth(2 * Resources.getSystem().getDisplayMetrics().density);

        setValue(0.0);
    }

    public void setValue(Double value) {
        currentAngle = (value + 90) % 360;
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        double w = getMeasuredWidth();
        double h = getMeasuredHeight();

        rect.right = (float) w;
        rect.bottom = (float) h;

        canvas.drawArc(rect, 45, 90, true, bgPaint);

        if ((currentAngle > 45) &&
                (currentAngle < 135)) {
            fgPaint.setColor(Color.GREEN);
        } else {
            fgPaint.setColor(Color.RED);
        }

        double a = Math.toRadians(currentAngle);
        float radius = (float) w / 2;

        float ex = (float) (radius * (1 + Math.cos(a)));
        float ey = (float) (radius * (1 + Math.sin(a)));

        canvas.drawLine(
            radius,
            radius,
            ex,
            ey,
            fgPaint
        );

        float m = 0.92f;
        float rm = 0.05f;

        path.moveTo(ex, ey);
        path.lineTo((float) (radius + (radius * m * Math.cos(a - rm))), (float) (radius + (radius * m * Math.sin(a - rm))));
        path.lineTo((float) (radius + (radius * m * Math.cos(a + rm))), (float) (radius + (radius * m * Math.sin(a + rm))));
        path.lineTo(ex, ey);
        canvas.drawPath(path, fgPaint);
        path.reset();
    }
}
