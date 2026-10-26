/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** A consistent, original 24-unit rounded-stroke icon family. */
public final class FluentIcon extends Drawable {
    private final Paint paint=new Paint(3);
    private final int kind,color;
    private final Path gear;
    private int alpha=255;
    public FluentIcon(int kind,int color){this.kind=kind;this.color=color;this.gear=kind==4?gearPath():null;}
    private static Path gearPath(){
        Path cog=new Path();double step=Math.PI/4;double[] offsets={-.5,-.29,-.22,.22,.29,.5};float[] radii={7.1f,7.1f,9.4f,9.4f,7.1f,7.1f};
        for(int tooth=0;tooth<8;tooth++)for(int point=0;point<offsets.length;point++){double angle=-Math.PI/2+tooth*step+offsets[point]*step;float x=12+(float)Math.cos(angle)*radii[point],y=12+(float)Math.sin(angle)*radii[point];if(tooth==0&&point==0)cog.moveTo(x,y);else cog.lineTo(x,y);}
        cog.close();return cog;
    }
    @Override public void draw(Canvas canvas){
        Rect b=getBounds();float size=Math.min(b.width(),b.height());
        canvas.save();canvas.translate(b.centerX()-size/2,b.centerY()-size/2);canvas.scale(size/24,size/24);
        paint.setColor(color);paint.setAlpha(Math.round(Color.alpha(color)*alpha/255f));paint.setStrokeWidth(1.7f);paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
        if(kind==0){ // paired music notes
            Path p=new Path();p.moveTo(9,17);p.lineTo(9,6);p.quadTo(9,5,10,4.8f);p.lineTo(19,3);p.lineTo(19,15);
            canvas.drawPath(p,paint);canvas.drawLine(9,9,19,7,paint);
            canvas.drawOval(3,16,9,21,paint);canvas.drawOval(13,14,19,19,paint);
        }else if(kind==1){ // radio receiver + antenna
            canvas.drawRoundRect(3,9,21,21,3,3,paint);canvas.drawLine(6,9,17,3,paint);
            canvas.drawCircle(8,15,2.5f,paint);canvas.drawLine(14,13,18,13,paint);canvas.drawLine(14,17,18,17,paint);
        }else if(kind==2){ // video clapper + play
            canvas.drawRoundRect(3,5,21,21,2.5f,2.5f,paint);canvas.drawLine(3,10,21,10,paint);
            canvas.drawLine(7,5,5,10,paint);canvas.drawLine(13,5,11,10,paint);canvas.drawLine(19,5,17,10,paint);
            Path p=new Path();p.moveTo(10,13);p.lineTo(15,16);p.lineTo(10,19);p.close();canvas.drawPath(p,paint);
        }else if(kind==3){ // television + feet
            canvas.drawRoundRect(2.5f,7,21.5f,19,2.5f,2.5f,paint);canvas.drawLine(8,3,12,7,paint);canvas.drawLine(16,3,12,7,paint);
            canvas.drawLine(7,19,5,22,paint);canvas.drawLine(17,19,19,22,paint);
        }else if(kind==5){
            canvas.drawLine(20,12,4,12,paint);canvas.drawLine(4,12,11,5,paint);canvas.drawLine(4,12,11,19,paint);
        }else if(kind==6||kind==7){
            if(kind==7){canvas.translate(24,0);canvas.scale(-1,1);}canvas.drawLine(5.5f,5,5.5f,19,paint);Path p=new Path();p.moveTo(18.5f,5);p.lineTo(8.5f,12);p.lineTo(18.5f,19);p.close();canvas.drawPath(p,paint);
        }else if(kind==8||kind==10){
            Path top=new Path();top.moveTo(4,10);top.lineTo(4,7);top.quadTo(4,5,6,5);top.lineTo(20,5);canvas.drawPath(top,paint);canvas.drawLine(17,2,20,5,paint);canvas.drawLine(20,5,17,8,paint);
            Path bottom=new Path();bottom.moveTo(20,14);bottom.lineTo(20,17);bottom.quadTo(20,19,18,19);bottom.lineTo(4,19);canvas.drawPath(bottom,paint);canvas.drawLine(7,16,4,19,paint);canvas.drawLine(4,19,7,22,paint);
            if(kind==10){Path one=new Path();one.moveTo(10.5f,10.5f);one.lineTo(12,9.5f);one.lineTo(12,14.5f);one.moveTo(10.5f,14.5f);one.lineTo(13.5f,14.5f);canvas.drawPath(one,paint);}
        }else if(kind==9){
            Path a=new Path();a.moveTo(3,6);a.lineTo(6,6);a.cubicTo(10,6,14,18,18,18);a.lineTo(21,18);canvas.drawPath(a,paint);canvas.drawLine(18,15,21,18,paint);canvas.drawLine(21,18,18,21,paint);
            Path crossing=new Path();crossing.moveTo(3,18);crossing.lineTo(6,18);crossing.cubicTo(10,18,14,6,18,6);crossing.lineTo(21,6);canvas.drawPath(crossing,paint);canvas.drawLine(18,3,21,6,paint);canvas.drawLine(21,6,18,9,paint);
        }else if(kind==12){ // clockwise refresh
            canvas.drawArc(4,4,20,20,40,290,false,paint);canvas.drawLine(19,3,19,8,paint);canvas.drawLine(14,8,19,8,paint);
        }else if(kind==14){ // direct URL / chain link
            canvas.rotate(-45,12,12);canvas.drawRoundRect(3,8,13,16,4,4,paint);canvas.drawRoundRect(11,8,21,16,4,4,paint);canvas.drawLine(9,12,15,12,paint);
        }else if(kind==15){ // region globe
            canvas.drawCircle(12,12,9,paint);canvas.drawOval(8,3,16,21,paint);canvas.drawLine(3,12,21,12,paint);canvas.drawLine(4.2f,7.5f,19.8f,7.5f,paint);canvas.drawLine(4.2f,16.5f,19.8f,16.5f,paint);
        }else if(kind==16){ // solid saved/favorite heart in the same 24-unit family
            Path heart=new Path();heart.moveTo(12,21);heart.cubicTo(10,19,3,14,3,8);heart.cubicTo(3,2,9,1,12,6);heart.cubicTo(15,1,21,2,21,8);heart.cubicTo(21,14,14,19,12,21);heart.close();paint.setStyle(Paint.Style.FILL);canvas.drawPath(heart,paint);
        }else if(kind==17){ // clean reset arrow: continuous circular path, no overlapping clock hands
            Path reset=new Path();reset.moveTo(4,10);reset.cubicTo(5,4,12,2,17,6);reset.cubicTo(23,11,19,20,12,20);reset.cubicTo(8,20,5,18,4,15);canvas.drawPath(reset,paint);canvas.drawLine(4,4,4,10,paint);canvas.drawLine(4,10,10,10,paint);
        }else if(kind==18){ // descending sort bars and down arrow
            canvas.drawLine(3,5,14,5,paint);canvas.drawLine(3,11,11,11,paint);canvas.drawLine(3,17,8,17,paint);canvas.drawLine(18,4,18,20,paint);canvas.drawLine(15,17,18,20,paint);canvas.drawLine(18,20,21,17,paint);
        }else if(kind==19){ // media permission shield + check
            Path shield=new Path();shield.moveTo(12,2);shield.lineTo(21,6);shield.lineTo(20,15);shield.quadTo(18,20,12,23);shield.quadTo(6,20,4,15);shield.lineTo(3,6);shield.close();canvas.drawPath(shield,paint);canvas.drawLine(8,12,11,15,paint);canvas.drawLine(11,15,17,9,paint);
        }else if(kind==20){ // guided setup steps
            for(int i=0;i<3;i++){canvas.drawCircle(5,5+i*7,2,paint);canvas.drawLine(10,5+i*7,21,5+i*7,paint);}
        }else if(kind==4){ // One connected mechanical cog outline with eight flat-topped teeth.
            canvas.drawPath(gear,paint);canvas.drawCircle(12,12,3,paint);
        }
        canvas.restore();
    }
    @Override public void setAlpha(int alpha){this.alpha=alpha;invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
