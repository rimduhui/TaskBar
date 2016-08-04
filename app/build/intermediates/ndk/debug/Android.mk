LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := libgetapp
LOCAL_LDFLAGS := -Wl,--build-id
LOCAL_SRC_FILES := \
	/Users/rimduhui/AndroidStudioProjects/TaskBar/app/src/main/jni/Android.mk \
	/Users/rimduhui/AndroidStudioProjects/TaskBar/app/src/main/jni/getapp.c \

LOCAL_C_INCLUDES += /Users/rimduhui/AndroidStudioProjects/TaskBar/app/src/main/jni
LOCAL_C_INCLUDES += /Users/rimduhui/AndroidStudioProjects/TaskBar/app/src/debug/jni

include $(BUILD_SHARED_LIBRARY)
