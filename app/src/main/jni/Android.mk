LOCAL_PATH:=$(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE:=libgetapp
LOCAL_MODULE_TAGS := optional eng
LOCAL_PRELINK_MODULE := false
LOCAL_SRC_FILES:=getapp.c
LOCAL_LDLIBS := -llog
#LOCAL_LDFLAGS += $(TARGET_OUT_INTERMEDIATE_LIBRARIES)/liblog.so

include $(BUILD_SHARED_LIBRARY)

