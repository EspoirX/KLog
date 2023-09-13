
LOCAL_CPPFLAGS += -frtti #-fexceptions
LOCAL_CFLAGS += -Werror -Wall
LOCAL_CFLAGS += -Wsign-compare -Wtype-limits -Wuninitialized -Wempty-body #-Wextra 64bit
LOCAL_CFLAGS += -Wno-error=conversion -Wno-error=sign-conversion -Werror=sign-compare -Wno-error=format -Wno-error=pointer-to-int-cast
LOCAL_CFLAGS += -Wno-unused-parameter -Wno-missing-field-initializers -Wno-mismatched-tags
LOCAL_CFLAGS += -Wno-tautological-constant-compare -Wno-deprecated-declarations

NDK_VERSION := $(strip $(patsubst android-ndk-%,%,$(filter android-ndk-%, $(subst /, ,$(dir $(TARGET_CC))))))
ifneq ($(filter r13 r13b, $(NDK_VERSION)),)
LOCAL_CFLAGS += -Wno-unknown-warning-option -Wno-deprecated-register -Wno-mismatched-tags -Wno-char-subscripts	#ndk r13 compile
LOCAL_CFLAGS +=	-Wno-infinite-recursion -Wno-gnu-designator -Wno-unused-const-variable	-Wno-unused-local-typedef -Wno-unused-private-field -Wno-error=unused-variable
LOCAL_CFLAGS += -Wno-overloaded-virtual#ndk r13 compile

LOCAL_LDLIBS += -latomic
endif

LOCAL_CFLAGS +=  -fdata-sections
LOCAL_LDFLAGS += -Wl,--gc-sections 

