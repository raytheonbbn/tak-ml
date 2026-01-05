package com.atakmap.android.takml_android.metrics;

import android.opengl.GLES20;
import android.os.Build;

import com.bbn.takml_server.client.models.DeviceMetadata;
import com.bbn.takml_server.client.models.GpuInfo;

import javax.microedition.khronos.egl.*;
import javax.microedition.khronos.opengles.GL10;

public class DeviceInfoUtil {
    private static DeviceMetadata deviceMetadata = null;

    public static DeviceMetadata getInfo(){
        // Device metadata is static, only decipher it once!
        if (deviceMetadata != null){
            return deviceMetadata;
        }

        deviceMetadata = new DeviceMetadata();
        deviceMetadata.setBrand(Build.BRAND);
        deviceMetadata.setDevice(Build.DEVICE);
        deviceMetadata.setManufacturer(Build.MANUFACTURER);
        deviceMetadata.setModel(Build.MODEL);
        deviceMetadata.setProduct(Build.PRODUCT);

        // Create an offscreen OpenGL context
        EGL10 egl = (EGL10) EGLContext.getEGL();
        EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        int[] version = new int[2];
        egl.eglInitialize(display, version);

        int[] configAttribs = {
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_RENDERABLE_TYPE, 4, // EGL_OPENGL_ES2_BIT
                EGL10.EGL_NONE
        };

        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfig = new int[1];
        egl.eglChooseConfig(display, configAttribs, configs, 1, numConfig);
        EGLConfig config = configs[0];

        int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
        int[] contextAttribs = {
                EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL10.EGL_NONE
        };

        EGLContext context = egl.eglCreateContext(display, config,
                EGL10.EGL_NO_CONTEXT, contextAttribs);

        int[] surfaceAttribs = {
                EGL10.EGL_WIDTH, 1,
                EGL10.EGL_HEIGHT, 1,
                EGL10.EGL_NONE
        };

        EGLSurface surface = egl.eglCreatePbufferSurface(display, config, surfaceAttribs);

        egl.eglMakeCurrent(display, surface, surface, context);

        // Now we can safely query GL strings
        String vendor = safe(GL10.GL_VENDOR);
        String renderer = safe(GL10.GL_RENDERER);
        String glVersion = safe(GL10.GL_VERSION);

        // Fill metadata
        GpuInfo gpuInfo = new GpuInfo();
        gpuInfo.setVendor(vendor);
        gpuInfo.setRenderer(renderer);
        gpuInfo.setVersion(glVersion);

        deviceMetadata.setGpuInfo(gpuInfo);

        // Cleanup
        egl.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
        egl.eglDestroySurface(display, surface);
        egl.eglDestroyContext(display, context);
        egl.eglTerminate(display);

        return deviceMetadata;
    }

    private static String safe(int which) {
        String value = GLES20.glGetString(which);
        return value != null ? value : "unknown";
    }
}
