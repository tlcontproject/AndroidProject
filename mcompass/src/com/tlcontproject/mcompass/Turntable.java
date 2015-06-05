/*
 * Copyright (C) 2009 Pierre H�bert <pierrox@pierrox.net>
 * http://www.pierrox.net/mcompass/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tlcontproject.mcompass;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;

public class Turntable {
    private static final int DETAIL_X[]={ 15, 25, 30 };
    private static final int DETAIL_Y[]={ 3, 6, 6 };
    private static final int RING_HEIGHT[]={ 2, 3, 3};

    private static final int TEXTURE_RING=0;
    private static final int TEXTURE_DIAL=1;

    private static final String[] CARDINAL_POINTS={ "N", "W", "S", "E" };

    // preference values
    private int mDetailsLevel;
    private boolean mReversedRing;

    private int[] mTextures;

    private IntBuffer mRingVertexBuffer;
    private IntBuffer mRingNormalBuffer;
    private IntBuffer mRingTexCoordBuffer;
    private ByteBuffer mRingIndexBuffer;

    private IntBuffer mDialVertexBuffer;
    private IntBuffer mDialNormalBuffer;
    private IntBuffer mDialTexCoordBuffer;
    private ByteBuffer mDialIndexBuffer;

    private IntBuffer mCapVertexBuffer;
    private ByteBuffer mCapIndexBuffer;

    private boolean mNeedObjectsUpdate;
    private boolean mNeedTextureUpdate;


    public Turntable() {
        mDetailsLevel=0;
        mReversedRing=false;

        // initially both objects and textures need to be built
        mNeedObjectsUpdate=true;
        mNeedTextureUpdate=true;
    }

    private void buildObjects() {
        buildRingObject();
        buildCapObject();
        buildDialObject();

        mNeedObjectsUpdate=false;
    }

    void buildRingObject() {
        // build vertices
        int dx=DETAIL_X[mDetailsLevel];
        int dy=DETAIL_Y[mDetailsLevel];
        int rh=RING_HEIGHT[mDetailsLevel];

        int vertices[]=new int[((dx+1)*(rh+1))*3];
        int normals[]=new int[((dx+1)*(rh+1))*3];
        int n=0;
        for(int i=0; i<=dx; i++) {
            for(int j=0; j<=rh; j++) {
                double a = i*(Math.PI*2)/dx;
                double b = j*Math.PI/(dy*2);

                double x = Math.sin(a)*Math.cos(b);
                double y = -Math.sin(b);
                double z = Math.cos(a)*Math.cos(b);

                vertices[n] = (int) (x*65536);
                vertices[n+1] = (int) (y*65536);
                vertices[n+2] = (int) (z*65536);
                normals[n] = vertices[n];
                normals[n+1] = vertices[n+1];
                normals[n+2] = vertices[n+2];
                n+=3;
            }
        }

        // build textures coordinates
        int texCoords[]=new int[(dx+1)*(rh+1)*2];
        n=0;
        for(int i=0; i<=dx; i++) {
            for(int j=0; j<=rh; j++) {
                texCoords[n++] = (i<<16)/dx;
                texCoords[n++] = (j<<16)/rh;
            }
        }

        // build indices
        byte indices[]=new byte[dx*rh*3*2];
        n=0;
        for(int i=0; i<dx; i++) {
            for(int j=0; j<rh; j++) {
                byte p0=(byte) ((rh+1)*i+j);
                indices[n++]=p0;
                indices[n++]=(byte) (p0+rh+1);
                indices[n++]=(byte) (p0+1);

                indices[n++]=(byte) (p0+rh+1);
                indices[n++]=(byte) (p0+rh+2);
                indices[n++]=(byte) (p0+1);
            }
        }

        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length*4);
        vbb.order(ByteOrder.nativeOrder());
        mRingVertexBuffer = vbb.asIntBuffer();
        mRingVertexBuffer.put(vertices);
        mRingVertexBuffer.position(0);

        ByteBuffer nbb = ByteBuffer.allocateDirect(normals.length*4);
        nbb.order(ByteOrder.nativeOrder());
        mRingNormalBuffer = nbb.asIntBuffer();
        mRingNormalBuffer.put(normals);
        mRingNormalBuffer.position(0);

        mRingIndexBuffer = ByteBuffer.allocateDirect(indices.length);
        mRingIndexBuffer.put(indices);
        mRingIndexBuffer.position(0);

        ByteBuffer tbb = ByteBuffer.allocateDirect(texCoords.length*4);
        tbb.order(ByteOrder.nativeOrder());
        mRingTexCoordBuffer = tbb.asIntBuffer();
        mRingTexCoordBuffer.put(texCoords);
        mRingTexCoordBuffer.position(0);
    }

    void buildCapObject() {
        int dx=DETAIL_X[mDetailsLevel];
        int dy=DETAIL_Y[mDetailsLevel];
        int rh=RING_HEIGHT[mDetailsLevel];

        int h=dy-rh;

        // build vertices
        int vertices[]=new int[((dx+1)*(h+1))*3];
        int n=0;
        for(int i=0; i<=dx; i++) {
            for(int j=rh; j<=dy; j++) {
                double a = i*(Math.PI*2)/dx;
                double b = j*Math.PI/(dy*2);

                double x = Math.sin(a)*Math.cos(b);
                double y = -Math.sin(b);
                double z = Math.cos(a)*Math.cos(b);

                vertices[n++] = (int) (x*65536);
                vertices[n++] = (int) (y*65536);
                vertices[n++] = (int) (z*65536);
            }
        }

        // build indices
        byte indices[]=new byte[dx*h*3*2];
        n=0;
        for(int i=0; i<dx; i++) {
            for(int j=0; j<h; j++) {
                byte p0=(byte) ((h+1)*i+j);
                indices[n++]=p0;
                indices[n++]=(byte) (p0+h+1);
                indices[n++]=(byte) (p0+1);

                indices[n++]=(byte) (p0+h+1);
                indices[n++]=(byte) (p0+h+2);
                indices[n++]=(byte) (p0+1);
            }
        }

        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length*4);
        vbb.order(ByteOrder.nativeOrder());
        mCapVertexBuffer = vbb.asIntBuffer();
        mCapVertexBuffer.put(vertices);
        mCapVertexBuffer.position(0);

        mCapIndexBuffer = ByteBuffer.allocateDirect(indices.length);
        mCapIndexBuffer.put(indices);
        mCapIndexBuffer.position(0);		
    }

    void buildDialObject() {
        // build vertices
        int dx=DETAIL_X[mDetailsLevel];

        int vertices[]=new int[(dx+2)*3];
        int normals[]=new int[(dx+2)*3];
        int n=0;
        // center of the dial
        vertices[n] = 0;
        vertices[n+1] = 0;
        vertices[n+2] = 0;
        normals[n] = 0;
        normals[n+1] = 1<<16;
        normals[n+2] = 0;
        n+=3;
        for(int i=0; i<=dx; i++) {
            double a = i*(Math.PI*2)/dx;

            double x = Math.sin(a);
            double z = Math.cos(a);

            vertices[n] = (int) (x*65536);
            vertices[n+1] = 0;
            vertices[n+2] = (int) (z*65536);
            normals[n] = 0;
            normals[n+1] = 1<<16;
            normals[n+2] = 0;
            n+=3;
        }

        // build textures coordinates
        int texCoords[]=new int[(dx+2)*2];
        n=0;
        texCoords[n++] = (int)(0.5*65536);
        texCoords[n++] = (int)(0.5*65536);
        for(int i=0; i<=dx; i++) {
            double a = i*(Math.PI*2)/dx;

            double x = (Math.sin(a)+1)/2;
            double z = (Math.cos(a)+1)/2;

            texCoords[n++] = (int)(x*65536);
            texCoords[n++] = (int)(z*65536);
        }

        // build indices
        byte indices[]=new byte[dx+2];
        n=0;
        for(int i=0; i<=(dx+1); i++) {
            indices[n++]=(byte)i;
        }        

        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length*4);
        vbb.order(ByteOrder.nativeOrder());
        mDialVertexBuffer = vbb.asIntBuffer();
        mDialVertexBuffer.put(vertices);
        mDialVertexBuffer.position(0);

        ByteBuffer nbb = ByteBuffer.allocateDirect(normals.length*4);
        nbb.order(ByteOrder.nativeOrder());
        mDialNormalBuffer = nbb.asIntBuffer();
        mDialNormalBuffer.put(normals);
        mDialNormalBuffer.position(0);

        mDialIndexBuffer = ByteBuffer.allocateDirect(indices.length);
        mDialIndexBuffer.put(indices);
        mDialIndexBuffer.position(0);

        ByteBuffer tbb = ByteBuffer.allocateDirect(texCoords.length*4);
        tbb.order(ByteOrder.nativeOrder());
        mDialTexCoordBuffer = tbb.asIntBuffer();
        mDialTexCoordBuffer.put(texCoords);
        mDialTexCoordBuffer.position(0);
    }

    public void draw(GL10 gl) {
        // rebuild objects or textures if needed
        if(mNeedObjectsUpdate) {
            buildObjects();
        }

        if(mNeedTextureUpdate) {
            buildTextures(gl);
        }

        int dx=DETAIL_X[mDetailsLevel];
        int dy=DETAIL_Y[mDetailsLevel];
        int rh=RING_HEIGHT[mDetailsLevel];

        gl.glFrontFace(GL10.GL_CW);
        gl.glColor4x(1<<16, 0<<16, 0<<16, 1<<16);

        // common parameters for the ring and the dial
        gl.glEnable(GL10.GL_TEXTURE_2D);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);

        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        gl.glColor4x(1<<16, 1<<16, 1<<16, 1<<16);
        gl.glScalex(90000, 90000, 90000);

        // draw the ring
        gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
        gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextures[TEXTURE_RING]);
        gl.glVertexPointer(3, GL10.GL_FIXED, 0, mRingVertexBuffer);
        gl.glNormalPointer(GL10.GL_FIXED, 0, mRingNormalBuffer);
        gl.glTexCoordPointer(2, GL10.GL_FIXED, 0, mRingTexCoordBuffer);
        gl.glDrawElements(GL10.GL_TRIANGLES, dx*rh*6, GL10.GL_UNSIGNED_BYTE, mRingIndexBuffer);

        // draw the dial
        gl.glFrontFace(GL10.GL_CCW);
        gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextures[TEXTURE_DIAL]);
        gl.glVertexPointer(3, GL10.GL_FIXED, 0, mDialVertexBuffer);
        gl.glNormalPointer(GL10.GL_FIXED, 0, mDialNormalBuffer);
        gl.glTexCoordPointer(2, GL10.GL_FIXED, 0, mDialTexCoordBuffer);
        gl.glDrawElements(GL10.GL_TRIANGLE_FAN, dx+2, GL10.GL_UNSIGNED_BYTE, mDialIndexBuffer);
        gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);

        // draw the cap
        gl.glFrontFace(GL10.GL_CW);
        gl.glColor4x(0<<16, 0<<16, 0<<16, 1<<16);
        gl.glDisable(GL10.GL_TEXTURE_2D);
        gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        gl.glVertexPointer(3, GL10.GL_FIXED, 0, mCapVertexBuffer);
        gl.glDrawElements(GL10.GL_TRIANGLES, dx*(dy-rh)*6, GL10.GL_UNSIGNED_BYTE, mCapIndexBuffer);
    }

    void buildTextures(GL10 gl) {
        mTextures=new int[2];

        gl.glGenTextures(2, mTextures, 0);

        buildRingTexture(gl);
        buildDialTexture(gl);

        mNeedTextureUpdate=false;
    }

    void buildRingTexture(GL10 gl) {
        gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextures[TEXTURE_RING]);
        gl.glPixelStorei(GL10.GL_UNPACK_ALIGNMENT, 1);
        gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);

        final int length=512;
        final int height=64;
        Bitmap b=Bitmap.createBitmap(length, height, Bitmap.Config.ARGB_8888);
        b.eraseColor(0xff000000);
        Canvas canvas=new Canvas(b);

        Paint p=new Paint();
        p.setAntiAlias(true);

        // draw minor graduations in grey
        /*p.setColor(0xffa0a0a0);
        for(int d=0; d<360; d++) {
        	canvas.drawLine(d*2, 0, d*2, 10, p);
        }*/

        // draw medium graduations in white
        p.setColor(0xffffffff);
        for(int d=0; d<360; d+=10) {
            int pos=d*length/360;
            canvas.drawLine(pos, 0, pos, 20, p);
        }

        // draw major graduations in yellow
        p.setColor(0xffffff00);
        p.setStrokeWidth(3);
        for(int d=0; d<360; d+=90) {
            int pos=d*length/360;
            canvas.drawLine(pos, 0, pos, 30, p);
        }

        // use center alignment for text
        p.setTextAlign(Paint.Align.CENTER);

        // draw minor graduations text
        p.setTextSize(14);
        p.setColor(0xffffffff);
        for(int d=0; d<360; d+=30) {
            // do not draw 0/90/180/270
            int pos=d*length/360;
            int angle=mReversedRing ? (360+180-d)%360 : 360-d;
            if(d%90!=0) canvas.drawText(Integer.toString(angle), pos, 30, p);
        }

        // draw N/O/S/E yellow
        // hack : go till 360, so that "N" is printed at both end of the texture...
        p.setTextSize(20);
        p.setColor(0xffffff00);
        for(int d=0; d<=360; d+=90) {
            int pos=d*length/360;
            if(mReversedRing) {
                canvas.drawText(CARDINAL_POINTS[((d+180)/90)%4], pos, 50, p);
            } else {
                canvas.drawText(CARDINAL_POINTS[(d/90)%4], pos, 50, p);
            }
        }

        p.setShader(new LinearGradient(0, 5, 0, 0, 0xff000000, 0xff404040, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, length, 5, p);

        /*BitmapDrawable bd=(BitmapDrawable)mContext.getResources().getDrawable(R.drawable.ruler);*/
        //GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, b, 0);
        ByteBuffer bb=ByteBuffer.allocate(length*height*4);
        b.copyPixelsToBuffer(bb);
        gl.glTexImage2D(GL10.GL_TEXTURE_2D, 0, GL10.GL_RGBA, length, height, 0, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, bb);
    }

    void buildDialTexture(GL10 gl) {
        int params[]=new int[1];
        gl.glGetIntegerv(GL10.GL_MAX_TEXTURE_SIZE, params, 0);
        gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextures[TEXTURE_DIAL]);
        gl.glPixelStorei(GL10.GL_UNPACK_ALIGNMENT, 1);
        gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);

        final int radius=256;
        Bitmap b=Bitmap.createBitmap(radius*2, radius*2, Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(b);

        Paint p=new Paint();
        p.setAntiAlias(true);

        // external shaded ring
        //int colors[]={0xff808080, 0xff000000, 0xffffffff, 0xff808080, 0x00000000};
        int colors[]={0xff000000, 0xff000000, 0xffffffff, 0xff000000, 0x00000000};
        float positions[]={0f, 0.94f, 0.95f, 0.98f, 1.0f};
        p.setShader(new RadialGradient(radius, radius, radius, colors, positions, Shader.TileMode.CLAMP));
        canvas.drawCircle(radius, radius, radius, p);
        p.setShader(null);

        // build the inner decoration, using two symmetrical paths
        Path pathl=new Path();
        pathl.moveTo(radius, radius/2);
        pathl.lineTo(radius+20, radius-20);
        pathl.lineTo(radius, radius);
        pathl.close();
        Path pathr=new Path();
        pathr.moveTo(radius, radius/2);
        pathr.lineTo(radius-20, radius-20);
        pathr.lineTo(radius, radius);
        pathr.close();
        canvas.save();
        for(int i=0; i<4; i++) {
            canvas.rotate((float) (i*90), radius, radius);
            p.setColor(0xff808080); //cross shade
            canvas.drawPath(pathl, p);
            p.setColor(0xffffffff);
            canvas.drawPath(pathr, p);
        }
        canvas.restore();

        // draw medium graduations in white
        p.setColor(0xffffffff);
        p.setStrokeWidth(4);
        for(int i=0; i<360; i+=10) {
            canvas.save();
            canvas.rotate(i, radius, radius);
            canvas.drawLine(radius, radius*2, radius, 1.75f*radius, p);
            canvas.restore();
        }


        // draw major graduations in yellow
        p.setColor(0xffffff00);
        p.setStrokeWidth(7);
        for(int i=0; i<360; i+=90) {
            canvas.save();
            canvas.rotate(i, radius, radius);
            canvas.drawLine(radius, radius*2, radius, 1.70f*radius, p);
            canvas.restore();
        }

        // medium graduation texts white
        p.setTextSize(40);
        p.setTextAlign(Paint.Align.CENTER);
        p.setColor(0xffffffff);
        for(int i=0; i<360; i+=30) {
            // do not draw 0/90/180/270
            if((i%90)!=0) {
                double a = -i*(Math.PI*2)/360;
                float x = (float)(Math.sin(a)*0.7*radius+radius);
                float y = (float)(Math.cos(a)*0.7*radius+radius);

                canvas.save();
                canvas.rotate(i, x, y);
                canvas.drawText(Integer.toString(i), x, y, p);
                canvas.restore();
            }
        }

        // draw N/O/S/E is yellow
        p.setTextSize(50);
        p.setColor(0xffffff00);
        for(int i=0; i<360; i+=90) {
            double a = i*(Math.PI*2)/360;
            float x = (float)(Math.sin(a)*0.65*radius+radius);
            float y = (float)(Math.cos(a)*0.65*radius+radius);

            canvas.save();
            canvas.rotate(-i, x, y);
            canvas.drawText(CARDINAL_POINTS[i/90], x, y, p);
            canvas.restore();
        }

        //GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, b, 0);
        ByteBuffer bb=ByteBuffer.allocate(radius*2*radius*2*4);
        b.copyPixelsToBuffer(bb);
        gl.glTexImage2D(GL10.GL_TEXTURE_2D, 0, GL10.GL_RGBA, radius*2, radius*2, 0, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, bb);
    }

    public void setDetailsLevel(int detailsLevel) {
        if(detailsLevel!=mDetailsLevel) {
            mDetailsLevel=detailsLevel;
            mNeedObjectsUpdate=true;
        }
    }

    public void setReversedRing(boolean reversedRing) {
        if(reversedRing!=mReversedRing) {
            mReversedRing=reversedRing;
            mNeedTextureUpdate=true;
        }
    }
}
