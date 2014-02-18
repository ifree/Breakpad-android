LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := BreakpadHelper
LOCAL_ARM_MODE	:= arm
LOCAL_CPP_EXTENSION :=.cc .cpp .cxx
LOCAL_LDLIBS += -llog -landroid
LOCAL_CPPFLAGS += -D_Android
LOCAL_CPPFLAGS += -DANDROID
LOCAL_CFLAGS		+= -DANDROID
LOCAL_CFLAGS		+= -D_Android
LOCAL_CFLAGS 		+= -g
#LOCAL_CFLAGS 		+= -v



breakpad_path=$(LOCAL_PATH)/../google-breakpad

LOCAL_C_INCLUDES	+= $(breakpad_path)/src
LOCAL_C_INCLUDES	+= $(breakpad_path)/src/common/android/include

FILE_LIST := $(wildcard $(LOCAL_PATH)/*.c*)
LOCAL_SRC_FILES := $(FILE_LIST:$(LOCAL_PATH)/%=%)

#include $(BUILD_SHARED_LIBRARY)

LOCAL_STATIC_LIBRARIES += breakpad_processor
LOCAL_STATIC_LIBRARIES += breakpad_client

include $(BUILD_SHARED_LIBRARY)

include $(LOCAL_PATH)/../google-breakpad/android/google_breakpad/Android.mk
include $(LOCAL_PATH)/../google-breakpad/android/google_breakpad/Android_processor.mk





